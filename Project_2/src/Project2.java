import java.util.ArrayList;
import lejos.hardware.Button;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.NXTUltrasonicSensor;
import lejos.robotics.RegulatedMotor;
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
import lejos.hardware.Button;
import lejos.robotics.SampleProvider;
import lejos.robotics.RegulatedMotor;

public class Project2 {
	
	// Sensors - matching Client.java setup
	private EV3UltrasonicSensor ev3UltrasonicSensor; // High sensor on S2 - obstacle avoidance
	private NXTUltrasonicSensor nxtUltrasonicSensor; // Low sensor on S4 - ball detection
	private EV3GyroSensor gyroSensor; // Gyro sensor on S3 - accurate rotation tracking
	private SampleProvider ev3Distance;
	private SampleProvider nxtDistance;
	private SampleProvider gyroAngle;
	
	// Actuators & Additional Sensors
	private EV3MediumRegulatedMotor armMotor; // Port A
	private RegulatedMotor leftMotor;
	private RegulatedMotor rightMotor;
	private EV3IRSensor irSensor; // Port S1
	private SampleProvider irDistanceMode;
	
	// Robot configuration
	private DifferentialPilot pilot;
	private Navigator nav;
	private ArrayList<Line> obstacleLines;
	private LineMap currentMap;
	
	// Detection thresholds
	private static final float OBSTACLE_DISTANCE = 20.0f; // cm - distance to detect obstacles (high sensor)
	private static final float BALL_DETECTION_DISTANCE = 30.0f; // cm - distance to ball (low sensor) - increased from 15
	private static final float SCAN_ANGLE_STEP = 15.0f; // degrees to rotate during scan
	private static final float OBSTACLE_PADDING = 35.0f; // cm - increased to account for robot radius + obstacle radius
	
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
		
		// Initialize Arm Motor
		armMotor = new EV3MediumRegulatedMotor(MotorPort.A);
		
		// Initialize IR Sensor
		try {
			irSensor = new EV3IRSensor(SensorPort.S1);
			irDistanceMode = irSensor.getDistanceMode();
		} catch (Exception e) {
			System.out.println("Warning: IR Sensor not found on S1");
		}
		
		// Initialize obstacle list
		obstacleLines = new ArrayList<>();
		
		// Initialize map with play area boundaries
		currentMap = initializeBaseMap();
		
		// Initialize robot motors
		double diam = DifferentialPilot.WHEEL_SIZE_NXT1;  // change this accordingly, unit is cm
		double trackWidth = 15.5; // change this accordingly, unit is cm
		
		// Use EV3LargeRegulatedMotor directly to avoid conflict with static Motor class
		leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
		rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
		
		pilot = new DifferentialPilot(diam, trackWidth, leftMotor, rightMotor);
		pilot.setLinearSpeed(6);	// 6 cm/s for linear movement
		pilot.setAngularSpeed(15);	// 15 degree/s for rotation in place
		pilot.setLinearAcceleration(200);	// gradually increase to the desired linear speed
		
		nav = new Navigator(pilot);
	}
	
	// Goal Coordinates (Based on 143x115 map)
	// Blue Goal (Left side) - Target for Green Team
	private static final Waypoint BLUE_GOAL = new Waypoint(15.0f, 57.5f); 
	// Green Goal (Right side) - Target for Blue Team
	private static final Waypoint GREEN_GOAL = new Waypoint(128.0f, 57.5f);
	
	private boolean isBlueTeam = true; // Default to Blue team
	private boolean isAttacker = true; // Default to Attacker

	public static void main(String[] args) {
		Project2 robot = new Project2();
		robot.run();
	}
	
	public void run() {
		try {
			// 0. Setup and Team Selection
			selectTeamAndRole();
			
			// Reset arm at the beginning
			resetArm();
			
			long gameEndTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
			
			while (System.currentTimeMillis() < gameEndTime) {
				System.out.println("--- NEW CYCLE ---");
				System.out.println("Waiting for Start Signal (ENTER)...");
				Button.ENTER.waitForPress();
				
				if (isAttacker) {
					runOffense();
				} else {
					runDefense();
				}
				
				System.out.println("Cycle Ended. Press ESC to exit game, or loop continues.");
				if (Button.ESCAPE.isDown()) {
					break;
				}
			}
			
			System.out.println("Game Over!");
			
		} catch (RuntimeException e) {
			if (e.getMessage().equals("Game aborted by user")) {
				System.out.println("Game aborted by user (ESCAPE pressed).");
			} else {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cleanup();
		}
	}
	
	private void checkEscape() {
		if (Button.ESCAPE.isDown()) {
			throw new RuntimeException("Game aborted by user");
		}
	}
	
	private void selectTeamAndRole() {
		System.out.println("Select Team:");
		System.out.println("UP: Blue Team");
		System.out.println("DOWN: Green Team");
		
		int button = Button.waitForAnyPress();
		if (button == Button.ID_DOWN) {
			isBlueTeam = false;
			System.out.println("Team: GREEN");
		} else {
			isBlueTeam = true;
			System.out.println("Team: BLUE");
		}
		
		try { Thread.sleep(500); } catch (Exception e) {}
		
		System.out.println("Select Role:");
		System.out.println("UP: Attacker");
		System.out.println("DOWN: Defender");
		
		button = Button.waitForAnyPress();
		if (button == Button.ID_DOWN) {
			isAttacker = false;
			System.out.println("Role: DEFENDER");
		} else {
			isAttacker = true;
			System.out.println("Role: ATTACKER");
		}
		
		try { Thread.sleep(500); } catch (Exception e) {}
	}
	
	private void runOffense() {
		System.out.println("OFFENSE STATE");
		
		// Phase 1: Scan
		Waypoint ballLocation = scanEnvironmentAndFindBall();
		
		if (ballLocation == null) {
			System.out.println("Ball not found! Resetting...");
			return;
		}
		
		System.out.println("Ball found at: (" + ballLocation.x + ", " + ballLocation.y + ")");
		
		// Phase 2: Update map with detected obstacles
		updateMapWithObstacles();
		
		// Phase 3: Plan path and navigate to the ball
		System.out.println("Planning path to ball...");
		boolean reachedBall = navigateWithPathPlanning(ballLocation, true);
		
		if (reachedBall) {
			// Phase 4: Capture Ball
			captureBall();
			
			// Phase 5: Deliver to Goal
			Waypoint goal = isBlueTeam ? GREEN_GOAL : BLUE_GOAL;
			System.out.println("Delivering to " + (isBlueTeam ? "GREEN" : "BLUE") + " goal...");
			boolean delivered = deliverBallToGoal(goal);
			
			if (delivered) {
				// Phase 6: Release
				releaseBall();
				
				// Phase 7: Celebrate (3 seconds)
				celebrateGoal();
				
				System.out.println("GOAL SCORED! Waiting for Ending Signal (ENTER) to reset...");
			} else {
				System.out.println("FAILED to deliver ball. Resetting...");
				lejos.hardware.Sound.buzz();
			}
			
			// Reset arm for next round
			resetArm();
			
			if (delivered) {
				Button.ENTER.waitForPress();
			}
		}
	}
	
	private void runDefense() {
		System.out.println("DEFENSE STATE");
		System.out.println("Waiting 3 seconds...");
		try { Thread.sleep(3000); } catch (Exception e) {}
		
		// Defense Loop - break on button press
		while (!Button.ENTER.isDown()) {
			checkEscape();
			// Scan for ball
			Waypoint ballLocation = scanEnvironmentAndFindBall();
			
			if (ballLocation != null) {
				// Check if ball is in safe zone to go to (not in back zones)
				// Assuming back zones are x < 10 or x > 133
				if (ballLocation.x > 10 && ballLocation.x < 133) {
					System.out.println("Defending! Going to ball...");
					navigateWithPathPlanning(ballLocation, true); // Go to ball
					
					// Kick/Push
					System.out.println("Kicking ball away!");
					pilot.setLinearSpeed(20);
					pilot.travel(10); // Push
					pilot.travel(-10); // Back off
					pilot.rotate(45); // Turn away
				} else {
					System.out.println("Ball in restricted zone. Holding position.");
				}
			} else {
				System.out.println("Ball not found. Scanning again...");
			}
			
			// Check for manual end of cycle
			if (Button.ENTER.isDown()) break;
		}
	}
	
	private void celebrateGoal() {
		System.out.println("Celebrating Goal!");
		for (int i = 0; i < 3; i++) {
			lejos.hardware.Sound.beep();
			try { Thread.sleep(1000); } catch (Exception e) {}
		}
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
			checkEscape();
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
			if (lowDistance > 2 && lowDistance < 250) // Low sensor detects something
			{
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
							
						// If ball is dangerously close, stop scanning to avoid bumping it
						if (lowDistance < 18.0f) {
							System.out.println("Ball is very close! Stopping scan to avoid collision.");
							break;
						}
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
			
			// Only zone in if the ball is far enough away to avoid hitting it during the wiggle
			if (closestDistance > 25.0f) {
				// Zone in on the ball with a fine scan
				System.out.println("Zoning in on ball (+- 5 degrees)...");
				rotateBy(-5); // Start 5 degrees to the left
				
				float minDistance = Float.MAX_VALUE;
				float bestAngleOffset = 0;
				
				// Scan 10 degrees in 1 degree steps
				for (int i = 0; i <= 10; i++) {
					checkEscape();
					float dist = getNXTDistance();
					// System.out.println("Fine Scan " + i + ": " + dist);
					if (dist < minDistance) {
						minDistance = dist;
						bestAngleOffset = i;
					}
					rotateBy(1); // Move 1 degree right
					try { Thread.sleep(50); } catch (Exception e) {}
				}
				
				// Rotate back to best angle
				// We are currently at +5 relative to center (total 10 degrees movement)
				// We want to go to bestAngleOffset (which is 0..10 relative to start)
				// So we need to rotate back by (10 - bestAngleOffset)
				float correction = -(10 - bestAngleOffset);
				System.out.println("Correction: " + correction + " degrees (Best dist: " + minDistance + ")");
				rotateBy(correction);
			} else {
				System.out.println("Ball is close (" + closestDistance + "cm). Skipping fine scan.");
			}
			
			return closestBall;
		}
		
		System.out.println("\nBall not detected in 360 degree scan");
		System.out.println("Low sensor must detect something that high sensor doesn't see");
		return null; // Ball not found
	}
	
	/**
	 * Navigate to destination using path planning to avoid obstacles
	 * @param destination Target location
	 * @param trackBall If true, use ball tracking for final approach. If false, just go to coordinates.
	 * @return true if navigation completed
	 */
	private boolean navigateWithPathPlanning(Waypoint destination, boolean trackBall) {
		int maxRetries = 5;
		int retries = 0;
		
		while (retries < maxRetries) {
			ShortestPathFinder pathPlanner = new ShortestPathFinder(currentMap);
			
			try {
				Pose startPose = nav.getPoseProvider().getPose();
				System.out.println("Current position: (" + String.format("%.1f", startPose.getX()) + ", " + 
							   String.format("%.1f", startPose.getY()) + ")");
				System.out.println("Target: (" + String.format("%.1f", destination.x) + ", " + 
							   String.format("%.1f", destination.y) + ")");
				
				Path path = pathPlanner.findRoute(startPose, destination);
				
				if (path == null || path.isEmpty()) {
					System.out.println("No path found! Trying direct approach...");
					if (trackBall) {
						navigateDirectToBall(destination);
					} else {
						navigateToWaypoint(destination);
					}
					return true;
				}
				
				System.out.println("Path found with " + path.size() + " waypoints");
				
				boolean pathBlocked = false;
				
				// Follow the path waypoint by waypoint
				for (int i = 0; i < path.size(); i++) {
					checkEscape();
					Waypoint wp = path.get(i);
					System.out.println("\nWaypoint " + (i+1) + "/" + path.size() + ": (" + 
								   String.format("%.1f", wp.x) + ", " + String.format("%.1f", wp.y) + ")");
					
					// Navigate to this waypoint
					if (i < path.size() - 1 || !trackBall) {
						// Intermediate waypoint OR final waypoint if not tracking ball - go directly
						if (!navigateToWaypoint(wp)) {
							System.out.println("Path blocked! Re-planning...");
							pathBlocked = true;
							break; // Break for loop to re-plan
						}
					} else {
						// Final waypoint AND tracking ball - use ball tracking
						navigateDirectToBall(wp);
					}
				}
				
				if (!pathBlocked) {
					return true; // Success
				}
				
				retries++;
				
			} catch (DestinationUnreachableException e) {
				System.out.println("Destination unreachable! Trying direct approach...");
				if (trackBall) {
					navigateDirectToBall(destination);
				} else {
					navigateToWaypoint(destination);
				}
				return true;
			}
		}
		
		System.out.println("Failed to reach destination after " + maxRetries + " retries.");
		return false;
	}
	
	/**
	 * Navigate to a waypoint without ball tracking, with dynamic obstacle avoidance
	 * @return true if reached, false if obstacle detected
	 */
	private boolean navigateToWaypoint(Waypoint wp) {
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
		
		// Travel to waypoint with monitoring
		pilot.forward();
		
		float startX = currentPose.getX();
		float startY = currentPose.getY();
		boolean obstacleDetected = false;
		
		while (pilot.isMoving()) {
			checkEscape();
			// Check for obstacles
			float highDistance = getEV3Distance();
			if (highDistance < OBSTACLE_DISTANCE && highDistance > 0) {
				pilot.stop();
				System.out.println("Obstacle detected during movement! Distance: " + highDistance);
				
				// Add obstacle
				Pose pose = nav.getPoseProvider().getPose();
				float obstacleX = pose.getX() + highDistance * (float)Math.cos(Math.toRadians(pose.getHeading()));
				float obstacleY = pose.getY() + highDistance * (float)Math.sin(Math.toRadians(pose.getHeading()));
				addObstacleToMap(obstacleX, obstacleY);
				updateMapWithObstacles();
				
				// Back up slightly to avoid getting stuck on the obstacle
				System.out.println("Backing up slightly...");
				pilot.travel(-5);
				
				obstacleDetected = true;
				break;
			}
			
			// Check if reached
			Pose p = nav.getPoseProvider().getPose();
			float distTraveled = (float)Math.sqrt(Math.pow(p.getX()-startX, 2) + Math.pow(p.getY()-startY, 2));
			if (distTraveled >= distance) {
				pilot.stop();
				break;
			}
			
			try { Thread.sleep(50); } catch (Exception e) {}
		}
		
		if (obstacleDetected) return false;
		
		System.out.println("Waypoint reached");
		return true;
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
			checkEscape();
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
					// Increased stop distance to 30cm to prevent pushing the ball
					if (lowDistance < 30) {
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
	 * Drive forward to capture the ball using IR sensor and Arm
	 */
	private void captureBall() {
		System.out.println("Capturing ball...");
		
		// We are currently ~15cm away from the ball
		pilot.setLinearSpeed(3); // Very slow for capture
		pilot.forward();
		
		// Use tacho count to estimate distance traveled since pilot.getMovement() might not be available
		int startTacho = leftMotor.getTachoCount();
		// Approx 20.5 degrees per cm for NXT wheels
		float degreesPerCm = 20.5f; 
		
		boolean armLifted = false;
		
		while (pilot.isMoving()) {
			checkEscape();
			int currentTacho = leftMotor.getTachoCount();
			float distanceTraveled = Math.abs(currentTacho - startTacho) / degreesPerCm;
			
			// Check IR Sensor
			float irDist = getIRDistance();
			
			// 1. Start lifting arm when we are ~10cm away (we started at 30, so after 20cm travel)
			if (!armLifted && distanceTraveled >= 20.0f) {
				System.out.println("Lifting arm...");
				// Rotate arm to lift (adjust angle as needed, assuming 90 degrees lifts it)
				armMotor.rotate(90, true); // Async return
				armLifted = true;
			}
			
			// 2. Stop if IR detects ball (close proximity) or we traveled too far
			// IR distance is usually arbitrary units or cm. Assuming cm for EV3 IR.
			// "touches the ball" -> very small value.
			if (irDist < 5.0f) { // Ball detected underneath/close
				System.out.println("IR detected ball! Stopping.");
				break;
			}
			
			// Safety: Stop if we traveled the requested ~30cm (plus small buffer) without IR detection
			if (distanceTraveled > 35.0f) { 
				System.out.println("IR not detected. Stopping at max distance (35cm).");
				break;
			}
			
			try { Thread.sleep(10); } catch (Exception e) {}
		}
		
		pilot.stop();
		// Ensure arm is fully up if not already
		if (!armLifted) {
			armMotor.rotate(90, false);
		}
		
		// Lower arm to trap the ball underneath
		System.out.println("Trapping ball (lowering arm)...");
		armMotor.rotate(-90);
	}

	/**
	 * Navigate to the goal area
	 */
	private boolean deliverBallToGoal(Waypoint goal) {
		// Reuse path planning to get to the goal
		return navigateWithPathPlanning(goal, false);
	}

	/**
	 * Release the ball at the goal
	 */
	private void releaseBall() {
		System.out.println("Releasing ball...");
		// Lift the arm to release/place the ball
		armMotor.rotate(90);
		// Robot sits on the goal (no backup)
	}
	
	private float getIRDistance() {
		if (irDistanceMode == null) return 255;
		float[] sample = new float[irDistanceMode.sampleSize()];
		irDistanceMode.fetchSample(sample, 0);
		return sample[0];
	}
	
	/**
	 * Reset arm to down position
	 */
	private void resetArm() {
		System.out.println("Resetting arm to DOWN position...");
		armMotor.rotate(-90);
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
		if (irSensor != null) {
			irSensor.close();
		}
		if (armMotor != null) {
			armMotor.close();
		}
		if (leftMotor != null) {
			leftMotor.close();
		}
		if (rightMotor != null) {
			rightMotor.close();
		}
		System.out.println("Sensors closed.");
	}
}
