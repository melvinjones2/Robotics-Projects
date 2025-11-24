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
			
			// Phase 1: Scan for both obstacles and ball
			System.out.println("Scanning for obstacles and ball...");
			Waypoint ballLocation = scanEnvironmentAndFindBall();
			
			if (ballLocation == null) {
				System.out.println("Ball not found! Try repositioning.");
				return;
			}
			
			System.out.println("Ball found at: (" + ballLocation.x + ", " + ballLocation.y + ")");
			
			// Phase 2: Update map with detected obstacles
			System.out.println("Updating map with " + (obstacleLines.size()/4) + " obstacles...");
			updateMapWithObstacles();
			
			// Phase 3: Plan path and navigate to the ball
			System.out.println("Planning path to ball...");
			navigateWithPathPlanning(ballLocation);
			
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
	 * Scan 360 degrees to detect obstacles and find the ball
	 * Obstacles: High sensor detects them
	 * Ball: Low sensor detects it, high sensor doesn't (ball is low to ground)
	 */
	private Waypoint scanEnvironmentAndFindBall() {
		System.out.println("Starting 360 degree scan...");
		System.out.println("Looking for obstacles (high sensor) and ball (low sensor)");
		
		Waypoint closestBall = null;
		float closestDistance = Float.MAX_VALUE;
		float ballHeading = 0;
		
		if (gyroSensor != null) {
			gyroSensor.reset(); // Reset gyro to 0 at start of scan
		}
		
		float startAngle = getGyroAngle();
		float cumulativeAngle = 0;
		final float STEP_SIZE = 10.0f; // 10 degree steps for smooth but accurate scan
		final int TOTAL_STEPS = 36; // 360 / 10 = 36 steps
		
		// Step-by-step rotation for accuracy
		for (int step = 0; step < TOTAL_STEPS; step++) {
			rotateBy(STEP_SIZE);
			cumulativeAngle += STEP_SIZE;
			
			// Let sensors stabilize after rotation
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			float currentAngle = getGyroAngle();
			float lowDistance = getNXTDistance(); // Use low sensor for ball detection
			float highDistance = getEV3Distance(); // Use high sensor to check for obstacles
			
			System.out.println("Step " + (step+1) + "/36, Angle " + String.format("%.0f", cumulativeAngle) + 
				"° (Gyro: " + String.format("%.0f", currentAngle - startAngle) + 
				"°): Low=" + String.format("%.1f", lowDistance) + 
				"cm, High=" + String.format("%.1f", highDistance) + "cm");
			
			// Detect obstacles with high sensor
			// Obstacle detection
			if (highDistance > 5 && highDistance < OBSTACLE_DISTANCE) {
				Pose pose = nav.getPoseProvider().getPose();
				float angleOffset = cumulativeAngle;
				
				// Calculate obstacle position
				float obstacleX = pose.getX() + highDistance * (float)Math.cos(Math.toRadians(pose.getHeading() + angleOffset));
				float obstacleY = pose.getY() + highDistance * (float)Math.sin(Math.toRadians(pose.getHeading() + angleOffset));
				
				// Add obstacle to map (avoid duplicates by checking distance)
				boolean isDuplicate = false;
				for (int i = 0; i < obstacleLines.size(); i += 4) {
					// Check if we already have an obstacle near this location
					if (obstacleLines.size() > i) {
						Line firstLine = obstacleLines.get(i);
						float dx = firstLine.x1 - obstacleX;
						float dy = firstLine.y1 - obstacleY;
						if (Math.sqrt(dx*dx + dy*dy) < 15) { // Within 15cm
							isDuplicate = true;
							break;
						}
					}
				}
				
				if (!isDuplicate) {
					addObstacleToMap(obstacleX, obstacleY);
					System.out.println("  -> OBSTACLE at " + String.format("%.0f", highDistance) + "cm");
				}
			}
			
			// Ball detection: Low sensor sees something BUT high sensor doesn't see it (or sees it much farther)
			// This means the object is low to the ground = likely the ball!
			if (lowDistance > 2 && lowDistance < 250) { // Low sensor detects something
				// High sensor should NOT detect it at the same or closer distance
				if (highDistance > lowDistance + 10 || highDistance > 100) {
					System.out.println("  -> BALL detected by LOW sensor only!");
					
					// Track the closest ball
					if (lowDistance < closestDistance) {
						closestDistance = lowDistance;
						ballHeading = currentAngle; // Store actual gyro angle when ball found
						Pose pose = nav.getPoseProvider().getPose();
						float angleOffset = cumulativeAngle;
						
						// Calculate ball position using current heading
						float ballX = pose.getX() + lowDistance * (float)Math.cos(Math.toRadians(pose.getHeading() + angleOffset));
						float ballY = pose.getY() + lowDistance * (float)Math.sin(Math.toRadians(pose.getHeading() + angleOffset));
						
						closestBall = new Waypoint(ballX, ballY);
						System.out.println("  -> Closest ball so far: " + String.format("%.1f", closestDistance) + 
							" cm at gyro angle " + String.format("%.0f", ballHeading) + " degrees");
					}
				}
			}
		}
		
		if (closestBall != null) {
			float currentGyro = getGyroAngle();
			float rotateBack = ballHeading - currentGyro;
			
			// Normalize angle
			while (rotateBack > 180) rotateBack -= 360;
			while (rotateBack < -180) rotateBack += 360;
			
			System.out.println("\n*** BALL FOUND at distance: " + String.format("%.1f", closestDistance) + " cm ***");
			System.out.println("Ball gyro angle: " + String.format("%.0f", ballHeading) + 
				"°, Current gyro: " + String.format("%.0f", currentGyro) + "°");
			System.out.println("Rotating " + String.format("%.0f", rotateBack) + " degrees to face ball...");
			
			lejos.hardware.Sound.beep();
			rotateBy(rotateBack);
			
			return closestBall;
		}
		
		System.out.println("\nBall not detected in 360 degree scan");
		System.out.println("Low sensor must detect something that high sensor doesn't see");
		return null; // Ball not found
	}
	
	/**
	 * Navigate to ball using path planning to avoid obstacles
	 */
	private void navigateWithPathPlanning(Waypoint ballLocation) {
		ShortestPathFinder pathPlanner = new ShortestPathFinder(currentMap);
		
		try {
			Pose startPose = nav.getPoseProvider().getPose();
			System.out.println("Current position: (" + String.format("%.1f", startPose.getX()) + ", " + 
						   String.format("%.1f", startPose.getY()) + ")");
			System.out.println("Ball position: (" + String.format("%.1f", ballLocation.x) + ", " + 
						   String.format("%.1f", ballLocation.y) + ")");
			
			Path path = pathPlanner.findRoute(startPose, ballLocation);
			
			if (path == null || path.isEmpty()) {
				System.out.println("No path found! Trying direct approach...");
				navigateDirectToBall(ballLocation);
				return;
			}
			
			System.out.println("Path found with " + path.size() + " waypoints");
			
			// Follow the path waypoint by waypoint
			for (int i = 0; i < path.size(); i++) {
				Waypoint wp = path.get(i);
				System.out.println("\nWaypoint " + (i+1) + "/" + path.size() + ": (" + 
							   String.format("%.1f", wp.x) + ", " + String.format("%.1f", wp.y) + ")");
				
				// Navigate to this waypoint
				if (i < path.size() - 1) {
					// Intermediate waypoint - go directly
					navigateToWaypoint(wp);
				} else {
					// Final waypoint (the ball) - use ball tracking
					navigateDirectToBall(wp);
				}
			}
			
		} catch (DestinationUnreachableException e) {
			System.out.println("Destination unreachable! Trying direct approach...");
			navigateDirectToBall(ballLocation);
		}
	}
	
	/**
	 * Navigate to a waypoint without ball tracking
	 */
	private void navigateToWaypoint(Waypoint wp) {
		Pose currentPose = nav.getPoseProvider().getPose();
		float dx = wp.x - currentPose.getX();
		float dy = wp.y - currentPose.getY();
		float distance = (float)Math.sqrt(dx*dx + dy*dy);
		float targetAngle = (float)Math.toDegrees(Math.atan2(dy, dx));
		float currentHeading = currentPose.getHeading();
		float turnAngle = targetAngle - currentHeading;
		
		// Normalize angle
		while (turnAngle > 180) turnAngle -= 360;
		while (turnAngle < -180) turnAngle += 360;
		
		System.out.println("Distance: " + String.format("%.1f", distance) + "cm, Turn: " + String.format("%.0f", turnAngle) + "°");
		
		// Turn to face waypoint
		if (Math.abs(turnAngle) > 5) {
			rotateBy(turnAngle);
		}
		
		// Travel to waypoint
		pilot.travel(distance);
		System.out.println("Waypoint reached");
	}
	
	/**
	 * Navigate directly to ball with tracking and stopping when close
	 */
	private void navigateDirectToBall(Waypoint ballLocation) {
		System.out.println("Direct navigation to ball at (" + (int)ballLocation.x + ", " + (int)ballLocation.y + ")");
		
		// Calculate initial distance to ball
		Pose currentPose = nav.getPoseProvider().getPose();
		float dx = ballLocation.x - currentPose.getX();
		float dy = ballLocation.y - currentPose.getY();
		float estimatedDistance = (float)Math.sqrt(dx*dx + dy*dy);
		
		System.out.println("Estimated distance to ball: " + String.format("%.1f", estimatedDistance) + " cm");
		System.out.println("Moving forward with ball tracking...");
		
		// First, turn to face the ball
		float targetAngle = (float)Math.toDegrees(Math.atan2(dy, dx));
		float currentHeading = currentPose.getHeading();
		float turnAngle = targetAngle - currentHeading;
		while (turnAngle > 180) turnAngle -= 360;
		while (turnAngle < -180) turnAngle += 360;
		if (Math.abs(turnAngle) > 5) {
			rotateBy(turnAngle);
		}
		
		pilot.setLinearSpeed(5); // Slower speed for better control
		pilot.forward(); // Start moving forward
		
		float lastBallDistance = Float.MAX_VALUE;
		int lostBallCount = 0;
		int cycleCount = 0;
		
		// Move toward ball with continuous tracking
		while (pilot.isMoving()) {
			cycleCount++;
			float lowDistance = getNXTDistance();
			float highDistance = getEV3Distance();
			
			// More lenient ball detection - just look for something close with low sensor
			boolean ballDetected = (lowDistance > 2 && lowDistance < 150);
			
			if (ballDetected) {
				System.out.println("Tracking: Low=" + String.format("%.1f", lowDistance) + 
								 "cm, High=" + String.format("%.1f", highDistance) + "cm");
				
				// If ball distance is increasing, we might have passed it or it moved
				if (lastBallDistance != Float.MAX_VALUE && lowDistance > lastBallDistance + 5) {
					pilot.stop();
					System.out.println("Ball distance increasing! Was " + String.format("%.1f", lastBallDistance) + 
									 "cm, now " + String.format("%.1f", lowDistance) + "cm. Sweeping...");
					if (sweepForBall()) {
						System.out.println("Ball realigned! Continuing...");
						pilot.forward();
						lastBallDistance = Float.MAX_VALUE; // Reset
						lostBallCount = 0;
					} else {
						System.out.println("Could not realign to ball.");
						break;
					}
				} else {
					lastBallDistance = lowDistance;
					lostBallCount = 0;
					
					// Stop if we're close enough to the ball
					if (lowDistance < 10) {
						pilot.stop();
						System.out.println("*** REACHED BALL! Stopping at " + String.format("%.1f", lowDistance) + " cm ***");
						break;
					}
				}
			} else {
				lostBallCount++;
				System.out.println("No detection: Low=" + String.format("%.1f", lowDistance) + 
								 "cm, High=" + String.format("%.1f", highDistance) + 
								 "cm (lost count: " + lostBallCount + ")");
				
				// Give it more time before sweeping
				if (lostBallCount == 8) {
					pilot.stop();
					System.out.println("Lost ball, sweeping to reacquire...");
					
					// Quick sweep left and right
					if (sweepForBall()) {
						System.out.println("Ball reacquired! Continuing...");
						pilot.forward();
						lostBallCount = 0;
					} else {
						System.out.println("Ball not found in sweep, stopping.");
						break;
					}
				} else if (lostBallCount >= 15) {
					pilot.stop();
					System.out.println("Ball lost for too long, stopping.");
					break;
				}
			}
			
			// Check for high obstacles (not the ball)
			if (highDistance < 15 && highDistance > 0 && 
				(lowDistance > highDistance || lowDistance > 100)) {
				pilot.stop();
				System.out.println("Obstacle detected by high sensor! Stopping.");
				break;
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		pilot.stop();
		System.out.println("Navigation complete!");
		lejos.hardware.Sound.beepSequenceUp();
	}
	
	/**
	 * Perform a quick sweep to reacquire the ball
	 * Returns true if ball found
	 */
	private boolean sweepForBall() {
		float[] sweepAngles = {-15, 30, -30, 15}; // Sweep pattern: left, right, back to center
		
		for (float angle : sweepAngles) {
			rotateBy(angle);
			
			try {
				Thread.sleep(200); // Let sensors stabilize
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			float lowDistance = getNXTDistance();
			float highDistance = getEV3Distance();
			
			// Check if we found the ball
			if (lowDistance > 2 && lowDistance < 50 && 
				(highDistance > lowDistance + 10 || highDistance > 100)) {
				System.out.println("  -> Ball found at " + String.format("%.1f", lowDistance) + " cm!");
				return true;
			}
		}
		
		return false; // Ball not found during sweep
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
