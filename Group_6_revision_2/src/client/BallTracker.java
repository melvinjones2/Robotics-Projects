package client;

import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.navigation.Pose;
import lejos.robotics.pathfinding.Path;
import lejos.robotics.pathfinding.ShortestPathFinder;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Rectangle;
import lejos.robotics.navigation.DestinationUnreachableException;
import shared.SocketConnection;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Simple ball tracker combining A05.java path planning with BallDetector scanning.
 * 1. Scan 360 degrees to find ball
 * 2. Add detected objects to map
 * 3. Find path to ball
 * 4. Navigate to ball
 */
public class BallTracker {
	private MovePilot pilot;
	private Navigator navigator;
	private SensorDataCollection sensorData;
	private SocketConnection connection;
	private LineMap playField;
	private ShortestPathFinder pathFinder;
	
	private static final float FIELD_WIDTH = 143.0f;   // cm
	private static final float FIELD_HEIGHT = 115.0f;  // cm
	private static final float STOP_DISTANCE = 5.0f;   // cm
	private static final int SCAN_SAMPLES = 36;        // 10-degree increments
	
	// Current robot pose
	private Pose currentPose;
	
	public BallTracker(MovePilot pilot, SensorDataCollection sensorData, SocketConnection connection) {
		this.pilot = pilot;
		this.navigator = new Navigator(pilot);
		this.sensorData = sensorData;
		this.connection = connection;
		
		// Start at center of field facing forward
		this.currentPose = new Pose(FIELD_WIDTH / 2, FIELD_HEIGHT / 2, 0);
		
		// Initialize play field with boundaries only
		this.playField = createPlayField();
		this.pathFinder = new ShortestPathFinder(playField);
		
		// Set speeds (from A05.java)
		pilot.setLinearSpeed(6);
		pilot.setAngularSpeed(15);
		pilot.setLinearAcceleration(200);
		
		log("Ball tracker initialized");
		log("Field: " + FIELD_WIDTH + "x" + FIELD_HEIGHT + " cm");
	}
	
	/**
	 * Create play field with boundary walls only (from A05.java)
	 */
	private LineMap createPlayField() {
		ArrayList<Line> lines = new ArrayList<Line>();
		
		lines.add(new Line(0.0f, 0.0f, FIELD_WIDTH, 0.0f));
		lines.add(new Line(0.0f, 0.0f, 0.0f, FIELD_HEIGHT));
		lines.add(new Line(0.0f, FIELD_HEIGHT, FIELD_WIDTH, FIELD_HEIGHT));
		lines.add(new Line(FIELD_WIDTH, FIELD_HEIGHT, FIELD_WIDTH, 0.0f));
		
		Line[] lineArr = lines.toArray(new Line[lines.size()]);
		return new LineMap(lineArr, new Rectangle(0.0f, 0.0f, FIELD_WIDTH, FIELD_HEIGHT));
	}
	
	/**
	 * Step 1: Scan 360 degrees to find ball (from BallDetector)
	 * Returns angle and distance to closest object
	 */
	private ScanResult scan360() throws IOException {
		log("=== Starting 360° scan ===");
		
		float minDist = Float.MAX_VALUE;
		float ballAngle = 0;
		
		// Start continuous rotation
		pilot.rotate(360, true);
		
		long startTime = System.currentTimeMillis();
		float lastAngle = 0;
		
		while (pilot.isMoving() && (System.currentTimeMillis() - startTime) < 8000) {
			float dist = sensorData.getBallDistance();
			float angle = (float) pilot.getMovement().getAngleTurned();
			
			// Sample every 10 degrees
			if (angle - lastAngle >= 10) {
				if (dist > 5 && dist < minDist && dist < 200) {
					minDist = dist;
					ballAngle = angle;
					log("Object: " + (int)angle + "° at " + (int)dist + " cm");
				}
				lastAngle = angle;
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		pilot.stop();
		
		if (minDist < 200) {
			log("Ball found: " + (int)ballAngle + "° (" + (int)minDist + " cm)");
			return new ScanResult(ballAngle, minDist);
		}
		
		log("No ball detected");
		return null;
	}
	
	/**
	 * Step 2: Calculate ball position and create waypoint
	 */
	private Waypoint calculateBallWaypoint(float angleFromRobot, float distance) {
		// Convert polar to cartesian relative to robot
		double angleRad = Math.toRadians(angleFromRobot + currentPose.getHeading());
		float ballX = currentPose.getX() + (distance * (float) Math.cos(angleRad));
		float ballY = currentPose.getY() + (distance * (float) Math.sin(angleRad));
		
		log("Ball position: (" + (int)ballX + "," + (int)ballY + ")");
		return new Waypoint(ballX, ballY);
	}
	
	/**
	 * Step 3: Navigate to ball using path finder (from A05.java)
	 */
	private boolean navigateToBall(Waypoint ballPosition) throws IOException {
		log("=== Navigating to ball ===");
		
		try {
			// Find route from current position to ball
			Path path = pathFinder.findRoute(currentPose, ballPosition);
			
			if (path == null) {
				log("No path found");
				return false;
			}
			
			// Set path and follow it step by step (like A05.java)
			navigator.setPath(path);
			navigator.singleStep(true);
			
			while (!navigator.getPath().isEmpty()) {
				Waypoint wp = navigator.getPath().get(0);
				log("Moving to waypoint: (" + (int)wp.x + "," + (int)wp.y + ")");
				
				navigator.followPath();
				navigator.waitForStop();
				
				// Check if we're close enough to ball
				float dist = sensorData.getBallDistance();
				if (dist > 0 && dist < STOP_DISTANCE) {
					log("Reached ball!");
					navigator.stop();
					return true;
				}
			}
			
			navigator.stop();
			return true;
			
		} catch (DestinationUnreachableException e) {
			log("Cannot reach ball: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Main entry point: Find and approach ball
	 * Combines: BallDetector scanning + A05 path planning
	 */
	public boolean findBall() throws IOException {
		// Step 1: Scan 360 to find ball
		ScanResult result = scan360();
		if (result == null) {
			return false;
		}
		
		// Step 2: Rotate to face ball
		float rotateToFace = result.angle - 360;  // We rotated 360, need to go back
		pilot.rotate(rotateToFace);
		log("Facing ball");
		
		return true;
	}
	
	/**
	 * Approach ball after finding it
	 */
	public void approachBall() throws IOException {
		log("=== Approaching ball ===");
		
		// Simple forward approach with distance monitoring
		for (int i = 0; i < 50; i++) {
			float dist = sensorData.getBallDistance();
			
			if (dist <= STOP_DISTANCE && dist > 0) {
				pilot.stop();
				log("Reached ball at " + (int)dist + " cm");
				break;
			}
			
			if (dist > STOP_DISTANCE && dist < 200) {
				double move = Math.min(dist - STOP_DISTANCE, 5);
				pilot.travel(move);
			}
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		pilot.stop();
	}
	
	/**
	 * Complete sequence: scan + approach
	 */
	public boolean findAndApproachBall() throws IOException {
		if (findBall()) {
			approachBall();
			return true;
		}
		return false;
	}
	
	/**
	 * Helper class to store scan results
	 */
	private static class ScanResult {
		float angle;
		float distance;
		
		ScanResult(float angle, float distance) {
			this.angle = angle;
			this.distance = distance;
		}
	}
	
	/**
	 * Track ball continuously for the specified duration.
	 */
	public void trackBall(int durationSeconds) throws IOException {
		log("Tracking object for " + durationSeconds + " seconds");
		
		long startTime = System.currentTimeMillis();
		long endTime = startTime + (durationSeconds * 1000);
		
		while (System.currentTimeMillis() < endTime) {
			float distance = sensorData.getBallDistance();
			
			// If object moved away, adjust
			if (distance > APPROACH_DISTANCE + 10 && distance < 200) {
				log("Object moved - adjusting position");
				pilot.travel(Math.min(distance - APPROACH_DISTANCE, 5));
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		log("Tracking complete");
	}
	
	private void log(String message) {
		try {
			if (connection != null) {
				connection.sendLog(message);
			}
		} catch (IOException e) {
			// Silent fail
		}
	}
}
