import java.util.ArrayList;

import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Rectangle;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.robotics.pathfinding.ShortestPathFinder;
import lejos.hardware.motor.*;
import lejos.hardware.sensor.*;
import lejos.hardware.port.*;
import lejos.robotics.SampleProvider;

public class Project2 {
	
	// Sensors - matching Client.java setup
	private EV3UltrasonicSensor ev3UltrasonicSensor; // High sensor on S2 - obstacle avoidance
	private NXTUltrasonicSensor nxtUltrasonicSensor; // Low sensor on S4 - ball detection
	private EV3GyroSensor gyroSensor; // Gyro sensor on S3 - accurate rotation tracking
	private SampleProvider ev3Distance;
	private SampleProvider nxtDistance;
	private SampleProvider gyroAngle;
	
	// Robot configuration
	private DifferentialPilot pilot;
	private Navigator nav;
	private ArrayList<Line> obstacleLines;
	private LineMap currentMap;
	
	// Detection thresholds
	private static final float OBSTACLE_DISTANCE = 20.0f; // cm - distance to detect obstacles (high sensor)
	private static final float BALL_DETECTION_DISTANCE = 30.0f; // cm - distance to ball (low sensor) - increased from 15
	private static final float SCAN_ANGLE_STEP = 15.0f; // degrees to rotate during scan
	private static final float OBSTACLE_PADDING = 10.0f; // cm - buffer around obstacles
	
	public static void main(String[] args) {
		Project2 robot = new Project2();
		robot.run();
	}
	
	public Project2() {
		// Initialize sensors - matching Client.java setup
		// EV3 Ultrasonic (high - obstacle avoidance) on S2
		ev3UltrasonicSensor = new EV3UltrasonicSensor(SensorPort.S2);
		ev3Distance = ev3UltrasonicSensor.getDistanceMode();
		
		// NXT Ultrasonic (low - ball detection) on S4
		nxtUltrasonicSensor = new NXTUltrasonicSensor(SensorPort.S4);
		nxtDistance = nxtUltrasonicSensor.getDistanceMode();
		
		// Gyro sensor on S3 - for accurate rotation
		try {
			gyroSensor = new EV3GyroSensor(SensorPort.S3);
			gyroAngle = gyroSensor.getAngleMode();
			System.out.println("Gyro initialized. Calibrating...");
			try {
				Thread.sleep(500); // Give sensor time to calibrate
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			gyroSensor.reset();
			System.out.println("Gyro ready.");
		} catch (Exception e) {
			System.out.println("Warning: Gyro sensor failed to initialize: " + e.getMessage());
			gyroSensor = null;
			gyroAngle = null;
		}
		
		// Initialize obstacle list
		obstacleLines = new ArrayList<>();
		
		// Initialize map with play area boundaries
		currentMap = initializeBaseMap();
		
		// Initialize robot motors
		double diam = DifferentialPilot.WHEEL_SIZE_NXT1;  // change this accordingly, unit is cm
		double trackWidth = 15.5; // change this accordingly, unit is cm
		
		pilot = new DifferentialPilot(diam, trackWidth, Motor.B, Motor.C);
		pilot.setLinearSpeed(6);	// 6 cm/s for linear movement
		pilot.setAngularSpeed(15);	// 15 degree/s for rotation in place
		pilot.setLinearAcceleration(200);	// gradually increase to the desired linear speed
		
		nav = new Navigator(pilot);
	}
	
	public void run() {
		try {
			System.out.println("Starting obstacle detection and ball search...");
			System.out.println("Press any button to start...");
			
			// Wait for button press to start
			lejos.hardware.Button.waitForAnyPress();
			
			// Phase 1: Quick scan for ball first (don't waste time on full obstacle scan)
			System.out.println("Searching for ping pong ball...");
			Waypoint ballLocation = searchForBall();
			
			if (ballLocation == null) {
				System.out.println("Ball not found! Try repositioning.");
				return;
			}
			
			System.out.println("Ball found at: (" + ballLocation.x + ", " + ballLocation.y + ")");
			
			// Phase 3: Navigate to the ball
			System.out.println("Navigating to ball...");
			navigateToBall(ballLocation);
			
			System.out.println("Mission complete!");
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cleanup();
		}
	}
	
	/**
	 * Scan environment in 360 degrees to detect obstacles
	 */
	private void scanEnvironment() {
		Pose currentPose = nav.getPoseProvider().getPose();
		float startAngle = currentPose.getHeading();
		
		// Perform 360-degree scan
		for (float angle = 0; angle < 360; angle += SCAN_ANGLE_STEP) {
			pilot.rotate(SCAN_ANGLE_STEP);
			
			float distance = getEV3Distance(); // Use high sensor for obstacles
			
			// If obstacle detected within threshold
			if (distance < OBSTACLE_DISTANCE && distance > 0) {
				// Get current position and heading
				Pose pose = nav.getPoseProvider().getPose();
				
				// Calculate obstacle position
				float obstacleX = pose.getX() + distance * (float)Math.cos(Math.toRadians(pose.getHeading()));
				float obstacleY = pose.getY() + distance * (float)Math.sin(Math.toRadians(pose.getHeading()));
				
				// Add obstacle as a square around the detected point
				addObstacleToMap(obstacleX, obstacleY);
				
				System.out.println("Obstacle detected at (" + Math.round(obstacleX) + ", " + Math.round(obstacleY) + ")");
			}
			
			try {
				Thread.sleep(100); // Brief pause for sensor stabilization
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// Return to original heading
		pilot.rotate(startAngle - nav.getPoseProvider().getPose().getHeading());
	}
	
	/**
	 * Search for the ping pong ball using low NXT ultrasonic sensor
	 * Ball detection: Low sensor detects it, high sensor doesn't (ball is low to ground)
	 */
	private Waypoint searchForBall() {
		System.out.println("Starting 360 degree scan for ball...");
		System.out.println("Looking for low objects (ball) that high sensor can't see");
		
		Waypoint closestBall = null;
		float closestDistance = Float.MAX_VALUE;
		float ballHeading = 0;
		
		if (gyroSensor != null) {
			gyroSensor.reset(); // Reset gyro to 0 at start of scan
		}
		float cumulativeAngle = 0;
		
		// Scan in 360 degrees
		for (float angle = 0; angle < 360; angle += SCAN_ANGLE_STEP) {
			rotateBy(SCAN_ANGLE_STEP);
			cumulativeAngle += SCAN_ANGLE_STEP;
			
			try {
				Thread.sleep(100); // Let sensor stabilize
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			float lowDistance = getNXTDistance(); // Use low sensor for ball detection
			float highDistance = getEV3Distance(); // Use high sensor to check for obstacles
			float currentGyroAngle = getGyroAngle();
			
			System.out.println("Angle " + (int)angle + "° (Gyro: " + String.format("%.0f", currentGyroAngle) + 
				"°): Low=" + String.format("%.1f", lowDistance) + 
				"cm, High=" + String.format("%.1f", highDistance) + "cm");
			
			// Ball detection: Low sensor sees something BUT high sensor doesn't see it (or sees it much farther)
			// This means the object is low to the ground = likely the ball!
			if (lowDistance > 2 && lowDistance < 250) { // Low sensor detects something
				// High sensor should NOT detect it at the same or closer distance
				if (highDistance > lowDistance + 10 || highDistance > 100) {
					System.out.println("  -> BALL detected by LOW sensor only!");
					
					// Track the closest ball
					if (lowDistance < closestDistance) {
						closestDistance = lowDistance;
						ballHeading = cumulativeAngle; // Store angle from start of scan
						Pose pose = nav.getPoseProvider().getPose();
						
						// Calculate ball position
						float ballX = pose.getX() + lowDistance * (float)Math.cos(Math.toRadians(pose.getHeading() + ballHeading));
						float ballY = pose.getY() + lowDistance * (float)Math.sin(Math.toRadians(pose.getHeading() + ballHeading));
						
						closestBall = new Waypoint(ballX, ballY);
						System.out.println("  -> Closest ball so far: " + String.format("%.1f", closestDistance) + 
							" cm at " + String.format("%.0f", ballHeading) + " degrees from start");
					}
				}
			}
		}
		
		if (closestBall != null) {
			System.out.println("\n*** BALL FOUND at distance: " + String.format("%.1f", closestDistance) + 
				" cm, " + String.format("%.0f", ballHeading) + " degrees from start ***");
			lejos.hardware.Sound.beep();
			
			// Rotate back to face the ball (we're at 360, ball was at ballHeading)
			float rotateBack = ballHeading - 360;
			System.out.println("Rotating " + String.format("%.0f", rotateBack) + " degrees to face ball...");
			rotateBy(rotateBack);
			
			return closestBall;
		}
		
		System.out.println("\nBall not detected in 360 degree scan");
		System.out.println("Low sensor must detect something that high sensor doesn't see");
		return null; // Ball not found
	}
	
	/**
	 * Navigate to the ball location - simple approach
	 */
	private void navigateToBall(Waypoint ballLocation) {
		System.out.println("Navigating to ball at (" + (int)ballLocation.x + ", " + (int)ballLocation.y + ")");
		
		// Calculate distance to ball
		Pose currentPose = nav.getPoseProvider().getPose();
		float dx = ballLocation.x - currentPose.getX();
		float dy = ballLocation.y - currentPose.getY();
		float distance = (float)Math.sqrt(dx*dx + dy*dy);
		
		System.out.println("Distance to ball: " + String.format("%.1f", distance) + " cm");
		System.out.println("Already facing ball, moving forward...");
		
		// Move toward ball with obstacle checking
		float travelDistance = distance - 5; // Stop 5cm before ball
		if (travelDistance > 0) {
			pilot.travel(travelDistance, true); // Non-blocking travel
			
			// Monitor for obstacles while moving
			while (pilot.isMoving()) {
				float obstacleDistance = getEV3Distance();
				if (obstacleDistance < 10 && obstacleDistance > 0) {
					pilot.stop();
					System.out.println("Obstacle detected! Stopping.");
					break;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("Reached ball location!");
		lejos.hardware.Sound.beepSequenceUp();
	}
	
	/**
	 * Quick scan in forward direction only
	 */
	private void scanAhead() {
		for (float angle = -45; angle <= 45; angle += 15) {
			pilot.rotate(15);
			
			float distance = getEV3Distance(); // Use high sensor for obstacles
			if (distance < OBSTACLE_DISTANCE && distance > 0) {
				Pose pose = nav.getPoseProvider().getPose();
				float obstacleX = pose.getX() + distance * (float)Math.cos(Math.toRadians(pose.getHeading()));
				float obstacleY = pose.getY() + distance * (float)Math.sin(Math.toRadians(pose.getHeading()));
				addObstacleToMap(obstacleX, obstacleY);
			}
		}
		pilot.rotate(-45); // Return to center
	}
	
	/**
	 * Add obstacle as a square on the map
	 */
	private void addObstacleToMap(float centerX, float centerY) {
		float half = OBSTACLE_PADDING / 2;
		
		// Create a square obstacle
		obstacleLines.add(new Line(centerX - half, centerY - half, centerX + half, centerY - half));
		obstacleLines.add(new Line(centerX + half, centerY - half, centerX + half, centerY + half));
		obstacleLines.add(new Line(centerX + half, centerY + half, centerX - half, centerY + half));
		obstacleLines.add(new Line(centerX - half, centerY + half, centerX - half, centerY - half));
	}
	
	/**
	 * Update the map with all detected obstacles
	 */
	private void updateMapWithObstacles() {
		ArrayList<Line> allLines = new ArrayList<>();
		
		// Add base map boundaries
		allLines.add(new Line(0.0f, 0.0f, 143.0f, 0.0f));
		allLines.add(new Line(0.0f, 0.0f, 0.0f, 115.0f));
		allLines.add(new Line(0.0f, 115.0f, 143.0f, 115.0f));
		allLines.add(new Line(143.0f, 115.0f, 143.0f, 0.0f));
		
		// Add all obstacle lines
		allLines.addAll(obstacleLines);
		
		Line[] lineArr = allLines.toArray(new Line[allLines.size()]);
		currentMap = new LineMap(lineArr, new Rectangle(0.0f, 0.0f, 143.0f, 115.0f));
		
		System.out.println("Map updated with " + obstacleLines.size()/4 + " obstacles");
	}
	
	/**
	 * Initialize base map with play area boundaries only
	 */
	private LineMap initializeBaseMap() {
		ArrayList<Line> lines = new ArrayList<>();
		
		// Play area edges
		lines.add(new Line(0.0f, 0.0f, 143.0f, 0.0f));
		lines.add(new Line(0.0f, 0.0f, 0.0f, 115.0f));
		lines.add(new Line(0.0f, 115.0f, 143.0f, 115.0f));
		lines.add(new Line(143.0f, 115.0f, 143.0f, 0.0f));
		
		Line[] lineArr = lines.toArray(new Line[lines.size()]);
		return new LineMap(lineArr, new Rectangle(0.0f, 0.0f, 143.0f, 115.0f));
	}
	
	/**
	 * Get distance from EV3 ultrasonic sensor (high - for obstacles)
	 */
	private float getEV3Distance() {
		float[] sample = new float[ev3Distance.sampleSize()];
		ev3Distance.fetchSample(sample, 0);
		float distance = sample[0] * 100; // Convert to cm
		// Handle infinity/out of range
		if (Float.isInfinite(distance) || distance > 255) {
			return 255; // Max sensor range
		}
		return distance;
	}
	
	/**
	 * Get distance from NXT ultrasonic sensor (low - for ball detection)
	 */
	private float getNXTDistance() {
		float[] sample = new float[nxtDistance.sampleSize()];
		nxtDistance.fetchSample(sample, 0);
		float distance = sample[0] * 100; // Convert to cm
		// Handle infinity/out of range
		if (Float.isInfinite(distance) || distance > 255) {
			return 255; // Max sensor range
		}
		return distance;
	}
	
	/**
	 * Get current angle from gyro sensor
	 */
	private float getGyroAngle() {
		if (gyroAngle == null) {
			return 0; // Gyro not available
		}
		float[] sample = new float[gyroAngle.sampleSize()];
		gyroAngle.fetchSample(sample, 0);
		return sample[0];
	}
	
	/**
	 * Rotate by a relative angle amount
	 */
	private void rotateBy(float degrees) {
		pilot.rotate(degrees);
		// Wait for rotation to complete
		while (pilot.isMoving()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Cleanup resources
	 */
	private void cleanup() {
		if (ev3UltrasonicSensor != null) {
			ev3UltrasonicSensor.close();
		}
		if (nxtUltrasonicSensor != null) {
			nxtUltrasonicSensor.close();
		}
		if (gyroSensor != null) {
			gyroSensor.close();
		}
		System.out.println("Sensors closed.");
	}
}
