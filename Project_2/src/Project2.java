import java.util.ArrayList;
import lejos.hardware.Sound;
import lejos.hardware.Button;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Navigator;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Rectangle;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.robotics.pathfinding.ShortestPathFinder;

public class Project2 {
	
	private RobotController robot;
	private ArrayList<Line> obstacleLines;
	private LineMap currentMap;
	
	// Detection thresholds
	private static final float OBSTACLE_DISTANCE = 20.0f; // cm - distance to detect obstacles (high sensor)
	private static final float BALL_DETECTION_DISTANCE = 30.0f; // cm - distance to ball (low sensor)
	private static final float OBSTACLE_PADDING = 35.0f; // cm
	
	// Field Dimensions
	private static final float FIELD_WIDTH = 143.0f;
	private static final float FIELD_HEIGHT = 115.0f;
	private static final float GOAL_Y = 57.5f;
	private static final float BLUE_START_Y = 20.0f;
	private static final float GREEN_START_Y = 95.0f;
	private static final float BLUE_START_X = 20.0f;
	private static final float GREEN_START_X = 123.0f;
	
	// Goal Coordinates (Based on 143x115 map)
	private static final Waypoint BLUE_GOAL = new Waypoint(15.0f, GOAL_Y); 
	private static final Waypoint GREEN_GOAL = new Waypoint(128.0f, GOAL_Y);
	
	private boolean isBlueTeam = true; // Default to Blue team
	private boolean isAttacker = true; // Default to Attacker
	private boolean isAutonomous = true; // Default to Autonomous
	private boolean isArmDown = false;
	
	private RemoteControl remoteControl;
	
	private Waypoint detectedOpponent = null; // Track opponent position for defense

	public Project2() {
		robot = new RobotController();
		obstacleLines = new ArrayList<>();
		currentMap = initializeBaseMap();
	}

	public static void main(String[] args) {
		Project2 project = new Project2();
		project.run();
	}
	
	public void run() {
		try {
			robot.initHardware();
			robot.startThreads();
			
			// Start Remote Control Listener
			remoteControl = new RemoteControl(12345);
			remoteControl.start();
			
			// 0. Setup and Team Selection
			selectTeamAndRole();
			
			long gameEndTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
			
			while (System.currentTimeMillis() < gameEndTime) {
				System.out.println("--- NEW CYCLE ---");
				System.out.println("Reposition Robot & Press ENTER (or GUI Enter)");
				waitForStartSignal();
				
				// Reset Map and Pose for new cycle
				obstacleLines.clear();
				currentMap = initializeBaseMap();
				
				if (isBlueTeam) {
					robot.getNav().getPoseProvider().setPose(new Pose(BLUE_START_X, BLUE_START_Y, 0.0f));
				} else {
					robot.getNav().getPoseProvider().setPose(new Pose(GREEN_START_X, GREEN_START_Y, 180.0f));
				}
				System.out.println("State Reset.");
				
				resetArm(); // Reset arm at the start of each cycle
				
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
				System.exit(0);
			} else {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cleanup();
		}
	}
	
	private void waitForStartSignal() {
		while (true) {
			if (Button.ENTER.isDown()) {
				while(Button.ENTER.isDown()) sleep(10); // Wait for release
				break;
			}
			
			if (remoteControl != null) {
				String cmd = remoteControl.getCommand();
				if (cmd.equals("ENTER")) {
					remoteControl.clearCommand();
					break;
				}
				if (cmd.equals("EXIT")) {
					throw new RuntimeException("Game aborted by user");
				}
			}
			
			if (Button.ESCAPE.isDown()) {
				throw new RuntimeException("Game aborted by user");
			}
			
			sleep(100);
		}
	}
	
	private void checkEscape() {
		if (Button.ESCAPE.isDown()) {
			throw new RuntimeException("Game aborted by user");
		}
		checkRemoteOverride();
	}
	
	private void checkRemoteOverride() {
		if (remoteControl == null) return;
		
		String cmd = remoteControl.getCommand();
		
		if (cmd.equals("EXIT")) {
			throw new RuntimeException("Game aborted by user");
		}
		
		if (!cmd.equals("STOP")) {
			System.out.println("Remote Override: " + cmd);
			if (isMoving()) {
				stop();
			}
			
			while (!cmd.equals("STOP") && !Button.ESCAPE.isDown()) {
				cmd = remoteControl.getCommand();
				
				if (cmd.equals("EXIT")) {
					throw new RuntimeException("Game aborted by user");
				}
				
				float obstacleDist = robot.getEV3Distance();
				
				// Asimov's First Law: A robot may not allow harm to come to itself (or the wall)
				if (cmd.equals("FORWARD") && obstacleDist < 25) {
					stop();
					System.out.println("First Law Violation: Collision Imminent! Stopping.");
					lejos.hardware.Sound.buzz();
					sleep(500);
					continue;
				}
				
				if ((cmd.equals("LEFT") || cmd.equals("RIGHT")) && obstacleDist < 10) {
					stop();
					System.out.println("First Law Violation: Too close to turn safely!");
					lejos.hardware.Sound.buzz();
					sleep(500);
					continue;
				}
				
				switch (cmd) {
					case "FORWARD":
						if (!isMoving()) forward();
						break;
					case "BACKWARD":
						if (!isMoving()) robot.getPilot().backward();
						break;
					case "LEFT":
						if (!isMoving()) robot.getPilot().rotateLeft();
						break;
					case "RIGHT":
						if (!isMoving()) robot.getPilot().rotateRight();
						break;
					case "STOP":
						stop();
						break;
					case "KICK":
						System.out.println("Kicking!");
						robot.getPilot().setLinearSpeed(robot.getPilot().getMaxLinearSpeed());
						robot.getPilot().travel(15);
						robot.getPilot().travel(-15);
						remoteControl.clearCommand();
						break;
					case "ARM_UP":
						 if (isArmDown) {
							robot.getArmMotor().rotate(90);
							isArmDown = false;
						 }
						 remoteControl.clearCommand();
						 break;
					case "ARM_DOWN":
						 if (!isArmDown) {
							robot.getArmMotor().rotate(-90);
							isArmDown = true;
						 }
						 remoteControl.clearCommand();
						 break;
				}
				
				sleep(50);
			}
			
			robot.getPilot().stop();
			System.out.println("Remote Override Ended. Resuming Auto.");
		}
	}
	
	private void selectTeamAndRole() {
		System.out.println("Select Team & Role:");
		System.out.println("Use GUI Setup OR Buttons:");
		System.out.println("UP: Blue / Attacker");
		System.out.println("DOWN: Green / Defender");
		
		boolean setupComplete = false;
		
		while (!setupComplete) {
			// Check GUI Command
			String cmd = remoteControl.getCommand();
			if (cmd.startsWith("SETUP")) {
				String[] parts = cmd.split(" ");
				if (parts.length == 3) {
					isBlueTeam = parts[1].equals("BLUE");
					isAttacker = parts[2].equals("ATTACKER");
					System.out.println("GUI Setup Received: " + parts[1] + " / " + parts[2]);
					remoteControl.clearCommand();
					setupComplete = true;
					break;
				}
			}
			
			// Check Buttons (Fallback)
			if (Button.UP.isDown()) {
				// Simple button logic: UP = Blue/Attacker default? 
				// The original logic was sequential (Team then Role).
				// Let's keep it simple: If they touch buttons, we go into button mode.
				System.out.println("Button detected. Entering manual setup...");
				manualButtonSetup();
				setupComplete = true;
			}
			
			if (Button.DOWN.isDown()) {
				System.out.println("Button detected. Entering manual setup...");
				manualButtonSetup();
				setupComplete = true;
			}
			
			sleep(100);
		}
		
		// Set initial pose based on team
		if (isBlueTeam) {
			robot.getNav().getPoseProvider().setPose(new Pose(BLUE_START_X, BLUE_START_Y, 0.0f));
			System.out.println("Start Pose: (" + BLUE_START_X + ", " + BLUE_START_Y + ", 0°)");
		} else {
			robot.getNav().getPoseProvider().setPose(new Pose(GREEN_START_X, GREEN_START_Y, 180.0f));
			System.out.println("Start Pose: (" + GREEN_START_X + ", " + GREEN_START_Y + ", 180°)");
		}
		
		try { Thread.sleep(500); } catch (Exception e) {}
	}
	
	private void manualButtonSetup() {
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
	}
	
	private void runOffense() {
		System.out.println("OFFENSE STATE");
		
		playFightSong();
		
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
				
				// Drive forward to place ball on goal
				System.out.println("Nudging ball into goal...");
				robot.getPilot().travel(10);
				robot.getPilot().stop();
				
				// Phase 7: Celebrate (3 seconds)
				celebrateGoal();
				
				System.out.println("GOAL SCORED! Waiting for Ending Signal (ENTER) to reset...");
			} else {
				System.out.println("FAILED to deliver ball. Resetting...");
				lejos.hardware.Sound.buzz();
			}
			
			// Reset arm for next round
			// resetArm(); // Removed: Arm reset is now handled at start of cycle
			
			if (delivered) {
				Button.ENTER.waitForPress();
			}
		}
	}
	
	private void runDefense() {
		System.out.println("DEFENSE STATE");
		
		playDefenseSong();
		
		System.out.println("Waiting 3 seconds...");
		sleep(3000);
		
		// Defense Loop - break on button press
		while (!Button.ENTER.isDown()) {
			checkEscape();
			
			// Scan for threats (Ball and Opponent)
			Waypoint ballLocation = scanForDefense();
			
			if (ballLocation != null) {
				// Priority 1: Ball found - Kick it!
				if (ballLocation.x > 10 && ballLocation.x < 133) {
					System.out.println("Defending! Going to ball...");
					navigateWithPathPlanning(ballLocation, true); // Go to ball
					
					// Kick/Push
					System.out.println("Kicking ball away!");
					robot.getPilot().setLinearSpeed(20);
					robot.getPilot().travel(15); // Push
					robot.getPilot().travel(-15); // Back off
					rotateBy(45); // Turn away
				} else {
					System.out.println("Ball in restricted zone. Holding position.");
				}
			} else if (detectedOpponent != null) {
				// Priority 2: Opponent found - Intercept!
				System.out.println("Opponent detected at (" + (int)detectedOpponent.x + ", " + (int)detectedOpponent.y + ")");
				interceptOpponent(detectedOpponent);
			} else {
				System.out.println("No threats found. Scanning again...");
			}
			
			if (Button.ENTER.isDown()) break;
		}
	}
	
	private void playFightSong() {
		new Thread(new Runnable() {
			public void run() {
				int[] freqs = { 
					440, 440, 440, 440, 349, 523, 440, 349, 523, 440, // Intro
					440, 440, 440, 440, 349, 523, 440, 349, 523, 440
				};
				int[] durations = { 
					150, 150, 150, 450, 150, 150, 150, 150, 150, 450,
					150, 150, 150, 450, 150, 150, 150, 150, 150, 450
				};
				
				for(int i=0; i<freqs.length; i++) {
					if (!isAutonomous) break; 
					lejos.hardware.Sound.playTone(freqs[i], durations[i]);
					try { Thread.sleep(durations[i] + 20); } catch(Exception e){}
				}
			}
		}).start();
	}
	
	private void playDefenseSong() {
		new Thread(new Runnable() {
			public void run() {
				// Chocobo Theme (Final Fantasy)
				int[] freqs = { 
					349, 440, 523, 698, 659, 587, 523, // F A C F(hi) E D C
					440, 392, 349, 392, 440, 392, 349, 330, 349 // A G F G A G F E F
				};
				int[] durations = { 
					150, 150, 150, 300, 150, 150, 300,
					150, 150, 150, 150, 150, 150, 150, 150, 450
				};
				
				for(int i=0; i<freqs.length; i++) {
					if (!isAutonomous) break; 
					lejos.hardware.Sound.playTone(freqs[i], durations[i]);
					try { Thread.sleep(durations[i] + 20); } catch(Exception e){}
				}
			}
		}).start();
	}
	
	private void interceptOpponent(Waypoint opponent) {
		System.out.println("Intercepting opponent...");
		
		Pose currentPose = robot.getNav().getPoseProvider().getPose();
		float dx = opponent.x - currentPose.getX();
		float dy = opponent.y - currentPose.getY();
		float distance = (float)Math.sqrt(dx*dx + dy*dy);
		
		float targetAngle = (float)Math.toDegrees(Math.atan2(dy, dx));
		float currentHeading = currentPose.getHeading();
		float turnAngle = targetAngle - currentHeading;
		while (turnAngle > 180) turnAngle -= 360;
		while (turnAngle < -180) turnAngle += 360;
		rotateBy(turnAngle);
		
		float travelDist = distance - 25.0f;
		
		if (travelDist > 5) {
			System.out.println("Closing distance: " + travelDist + "cm");
			robot.getPilot().travel(travelDist, true);
			while (robot.getPilot().isMoving()) {
				checkEscape();
				sleep(50);
			}
			lejos.hardware.Sound.buzz();
		} else {
			System.out.println("Already close to opponent. Holding ground.");
		}
	}
	
	private Waypoint scanForDefense() {
		System.out.println("Scanning for threats (Ball/Opponent)...");
		detectedOpponent = null;
		Waypoint closestBall = null;
		float closestBallDist = Float.MAX_VALUE;
		float closestOpponentDist = Float.MAX_VALUE;
		
		robot.resetGyro();
		float cumulativeAngle = 0;
		
		for (int step = 0; step < 18; step++) {
			checkEscape();
			rotateBy(20);
			cumulativeAngle += 20;
			sleep(100);
			
			float lowDistance = robot.getNXTDistance(); // Ball
			float highDistance = robot.getEV3Distance(); // Opponent/Wall
			
			Pose pose = robot.getNav().getPoseProvider().getPose();
			float headingRad = (float)Math.toRadians(pose.getHeading());
			
			// 1. Check for Ball
			if (lowDistance > 2 && lowDistance < 200) {
				if (highDistance > lowDistance + 10 || highDistance > 100) {
					if (lowDistance < closestBallDist) {
						closestBallDist = lowDistance;
						float ballX = pose.getX() + lowDistance * (float)Math.cos(headingRad);
						float ballY = pose.getY() + lowDistance * (float)Math.sin(headingRad);
						closestBall = new Waypoint(ballX, ballY);
					}
				}
			}
			
			// 2. Check for Opponent (High Sensor)
			if (highDistance > 5 && highDistance < 100) {
				float objX = pose.getX() + highDistance * (float)Math.cos(headingRad);
				float objY = pose.getY() + highDistance * (float)Math.sin(headingRad);
				
				boolean isWall = (objX < 15 || objX > 128 || objY < 15 || objY > 100);
				
				if (!isWall) {
					if (highDistance < closestOpponentDist) {
						closestOpponentDist = highDistance;
						detectedOpponent = new Waypoint(objX, objY);
					}
				}
			}
		}
		
		return closestBall;
	}

	private void celebrateGoal() {
		System.out.println("Celebrating Goal!");
		for (int i = 0; i < 3; i++) {
			lejos.hardware.Sound.beep();
			sleep(1000);
		}
	}

	private Waypoint scanEnvironmentAndFindBall() {
		System.out.println("Starting 100 degree scan (Field Side)...");
		System.out.println("Looking for obstacles (high sensor) and ball (low sensor)");
		
		Waypoint closestBall = null;
		float closestDistance = Float.MAX_VALUE;
		float ballHeading = 0;
		
		robot.resetGyro();
		
		float startAngle = robot.getGyroAngle();
		float cumulativeAngle = 0;
		// Scan towards the field (Left Turn / CCW)
		// Blue (0 deg): Scan 0 to 100 (North)
		// Green (180 deg): Scan 180 to 280 (South)
		final float STEP_SIZE = 10.0f; 
		final int TOTAL_STEPS = 10; // Scan 100 degrees total
		
		for (int step = 0; step < TOTAL_STEPS; step++) {
			checkEscape();
			rotateBy(STEP_SIZE);
			cumulativeAngle += STEP_SIZE;
			
			sleep(150);
			
			float currentAngle = robot.getGyroAngle();
			float lowDistance = robot.getNXTDistance(); 
			float highDistance = robot.getEV3Distance(); 
			
			System.out.println("Step " + (step+1) + "/" + TOTAL_STEPS + ", Angle " + String.format("%.0f", cumulativeAngle) + 
				"° (Gyro: " + String.format("%.0f", currentAngle - startAngle) + 
				"°): Low=" + String.format("%.1f", lowDistance) + 
				"cm, High=" + String.format("%.1f", highDistance) + "cm");
			
			// Obstacle detection
			if (highDistance > 5 && highDistance < OBSTACLE_DISTANCE) {
				System.out.println("  -> OBSTACLE detected at " + String.format("%.0f", highDistance) + "cm. Refining...");
				scanAndAddObstacle();
			}
			
			// Ball detection
			if (lowDistance > 2 && lowDistance < 250) {
				if (highDistance > lowDistance + 10 || highDistance > 100) {
					
					Pose pose = robot.getNav().getPoseProvider().getPose();
					float ballX = pose.getX() + lowDistance * (float)Math.cos(Math.toRadians(pose.getHeading()));
					float ballY = pose.getY() + lowDistance * (float)Math.sin(Math.toRadians(pose.getHeading()));
					
					// Filter out walls (objects outside or on boundary of field)
					if (ballX < 8 || ballX > FIELD_WIDTH - 8 || ballY < 8 || ballY > FIELD_HEIGHT - 8) {
						System.out.println("  -> Ignored object at (" + (int)ballX + ", " + (int)ballY + ") - Likely Wall");
					} else {
						System.out.println("  -> BALL detected by LOW sensor only!");
						
						if (lowDistance < closestDistance) {
							closestDistance = lowDistance;
							ballHeading = currentAngle; 
							
							closestBall = new Waypoint(ballX, ballY);
							System.out.println("  -> Closest ball so far: " + String.format("%.1f", closestDistance) + 
								" cm at gyro angle " + String.format("%.0f", ballHeading) + " degrees");
								
							if (lowDistance < 18.0f) {
								System.out.println("Ball is very close! Stopping scan to avoid collision.");
								break;
							}
						}
					}
				}
			}
		}
		
		if (closestBall != null) {
			float currentGyro = robot.getGyroAngle();
			float rotateBack = ballHeading - currentGyro;
			
			while (rotateBack > 180) rotateBack -= 360;
			while (rotateBack < -180) rotateBack += 360;
			
			System.out.println("\n*** BALL FOUND at distance: " + String.format("%.1f", closestDistance) + " cm ***");
			System.out.println("Rotating " + String.format("%.0f", rotateBack) + " degrees to face ball...");
			
			lejos.hardware.Sound.beep();
			rotateBy(rotateBack);
			
			if (closestDistance > 25.0f) {
				System.out.println("Zoning in on ball (+- 5 degrees)...");
				rotateBy(-5); 
				
				float minDistance = Float.MAX_VALUE;
				float bestAngleOffset = 0;
				
				for (int i = 0; i <= 10; i++) {
					checkEscape();
					float dist = robot.getNXTDistance();
					if (dist < minDistance) {
						minDistance = dist;
						bestAngleOffset = i;
					}
					rotateBy(1); 
					sleep(50);
				}
				
				float correction = -(10 - bestAngleOffset);
				System.out.println("Correction: " + correction + " degrees (Best dist: " + minDistance + ")");
				rotateBy(correction);
			} else {
				System.out.println("Ball is close (" + closestDistance + "cm). Skipping fine scan.");
			}
			
			return closestBall;
		}
		
		System.out.println("\nBall not detected in 360 degree scan");
		return null; 
	}
	
	private boolean navigateWithPathPlanning(Waypoint destination, boolean trackBall) {
		int maxRetries = 5;
		int retries = 0;
		
		while (retries < maxRetries) {
			ShortestPathFinder pathPlanner = new ShortestPathFinder(currentMap);
			
			try {
				Pose startPose = robot.getNav().getPoseProvider().getPose();
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
				
				for (int i = 0; i < path.size(); i++) {
					checkEscape();
					Waypoint wp = path.get(i);
					System.out.println("\nWaypoint " + (i+1) + "/" + path.size() + ": (" + 
								   String.format("%.1f", wp.x) + ", " + String.format("%.1f", wp.y) + ")");
					
					if (i < path.size() - 1 || !trackBall) {
						if (!navigateToWaypoint(wp)) {
							System.out.println("Path blocked! Re-planning...");
							pathBlocked = true;
							break; 
						}
					} else {
						navigateDirectToBall(wp);
					}
				}
				
				if (!pathBlocked) {
					return true; 
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
	
	private void scanAndAddObstacle() {
		System.out.println("Scanning obstacle to refine position...");
		
		float minDistance = Float.MAX_VALUE;
		float angleAtMin = 0;
		
		// Start scan from -20 degrees
		rotateBy(-20);
		
		// Scan 40 degrees total (9 steps of 5 degrees: -20, -15, ..., 20)
		// Note: The loop rotates 5 degrees *after* each measurement.
		// 9 iterations * 5 degrees = 45 degrees total rotation.
		// Starts at -20, ends at +25.
		for (int i = 0; i <= 8; i++) {
			float dist = robot.getEV3Distance();
			if (dist < minDistance && dist > 0) {
				minDistance = dist;
				angleAtMin = -20 + (i * 5);
			}
			rotateBy(5);
		}
		
		// Return to original heading (from +25 back to 0)
		rotateBy(-25);
		
		if (minDistance < OBSTACLE_DISTANCE + 15) {
			System.out.println("Refined Obstacle: " + minDistance + "cm at " + angleAtMin + " degrees");
			
			Pose pose = robot.getNav().getPoseProvider().getPose();
			float headingRad = (float)Math.toRadians(pose.getHeading());
			
			float obstacleX = pose.getX() + minDistance * (float)Math.cos(headingRad);
			float obstacleY = pose.getY() + minDistance * (float)Math.sin(headingRad);
			
			// Check for duplicates
			boolean isDuplicate = false;
			for (int i = 0; i < obstacleLines.size(); i += 4) {
				if (obstacleLines.size() > i) {
					Line firstLine = obstacleLines.get(i);
					// Check distance to the first line of the existing obstacle (approx center)
					// Note: firstLine.x1 is (centerX - half), so we add half back or just check proximity
					// The original logic checked against x1,y1 directly with a 15cm threshold.
					float dx = firstLine.x1 - obstacleX;
					float dy = firstLine.y1 - obstacleY;
					if (Math.sqrt(dx*dx + dy*dy) < 20) { // Increased threshold slightly to be safe
						isDuplicate = true;
						break;
					}
				}
			}
			
			if (!isDuplicate) {
				addObstacleToMap(obstacleX, obstacleY);
				updateMapWithObstacles();
			} else {
				System.out.println("Obstacle already exists in map. Skipping.");
			}
		} else {
			System.out.println("Scan did not confirm obstacle within range.");
		}
	}
	
	private boolean navigateToWaypoint(Waypoint wp) {
		Pose currentPose = robot.getNav().getPoseProvider().getPose();
		float dx = wp.x - currentPose.getX();
		float dy = wp.y - currentPose.getY();
		float distance = (float)Math.sqrt(dx*dx + dy*dy);
		float targetAngle = (float)Math.toDegrees(Math.atan2(dy, dx));
		float currentHeading = currentPose.getHeading();
		float turnAngle = targetAngle - currentHeading;
		
		while (turnAngle > 180) turnAngle -= 360;
		while (turnAngle < -180) turnAngle += 360;
		
		System.out.println("Distance: " + String.format("%.1f", distance) + "cm, Turn: " + String.format("%.0f", turnAngle) + "°");
		
		if (Math.abs(turnAngle) > 5) {
			rotateBy(turnAngle);
		}
		
		forward();
		
		float startX = currentPose.getX();
		float startY = currentPose.getY();
		boolean obstacleDetected = false;
		
		while (robot.getPilot().isMoving()) {
			checkEscape();
			float highDistance = robot.getEV3Distance();
			if (highDistance < OBSTACLE_DISTANCE && highDistance > 0) {
				robot.getPilot().stop();
				System.out.println("Obstacle detected during movement! Distance: " + highDistance);
				
				scanAndAddObstacle();
				
				System.out.println("Backing up slightly...");
				robot.getPilot().travel(-5);
				
				obstacleDetected = true;
				break;
			}
			
			Pose p = robot.getNav().getPoseProvider().getPose();
			float distTraveled = (float)Math.sqrt(Math.pow(p.getX()-startX, 2) + Math.pow(p.getY()-startY, 2));
			if (distTraveled >= distance) {
				robot.getPilot().stop();
				break;
			}
			
			try { Thread.sleep(50); } catch (Exception e) {}
		}
		
		if (obstacleDetected) return false;
		
		Pose p = robot.getNav().getPoseProvider().getPose();
		float distRemaining = (float)p.distanceTo(wp);
		
		if (distRemaining > 5.0f) {
			System.out.println("Waypoint missed! Remaining dist: " + distRemaining);
			return false;
		}
		
		System.out.println("Waypoint reached");
		return true;
	}
	
	private void navigateDirectToBall(Waypoint ballLocation) {
		System.out.println("Direct navigation to ball at (" + (int)ballLocation.x + ", " + (int)ballLocation.y + ")");
		
		Pose currentPose = robot.getNav().getPoseProvider().getPose();
		float dx = ballLocation.x - currentPose.getX();
		float dy = ballLocation.y - currentPose.getY();
		float estimatedDistance = (float)Math.sqrt(dx*dx + dy*dy);
		
		System.out.println("Estimated distance to ball: " + String.format("%.1f", estimatedDistance) + " cm");
		System.out.println("Moving forward with ball tracking...");
		
		float targetAngle = (float)Math.toDegrees(Math.atan2(dy, dx));
		float currentHeading = currentPose.getHeading();
		float turnAngle = targetAngle - currentHeading;
		while (turnAngle > 180) turnAngle -= 360;
		while (turnAngle < -180) turnAngle += 360;
		if (Math.abs(turnAngle) > 5) {
			rotateBy(turnAngle);
		}
		
		robot.getPilot().setLinearSpeed(5); 
		forward(); 
		
		float lastBallDistance = Float.MAX_VALUE;
		int lostBallCount = 0;
		int cycleCount = 0;
		
		while (robot.getPilot().isMoving()) {
			checkEscape();
			cycleCount++;
			float lowDistance = robot.getNXTDistance();
			float highDistance = robot.getEV3Distance();
			
			boolean ballDetected = (lowDistance > 2 && lowDistance < 150);
			
			if (ballDetected) {
				// Filter out walls during tracking
				Pose pose = robot.getNav().getPoseProvider().getPose();
				float ballX = pose.getX() + lowDistance * (float)Math.cos(Math.toRadians(pose.getHeading()));
				float ballY = pose.getY() + lowDistance * (float)Math.sin(Math.toRadians(pose.getHeading()));
				
				if (ballY < 8 || ballY > FIELD_HEIGHT - 8 || ballX < 8 || ballX > FIELD_WIDTH - 8) {
					// System.out.println("Ignoring wall at (" + (int)ballX + "," + (int)ballY + ")");
					ballDetected = false;
				}
			}
			
			if (ballDetected) {
				System.out.println("Tracking: Low=" + String.format("%.1f", lowDistance) + 
								 "cm, High=" + String.format("%.1f", highDistance) + "cm");
				
				if (lastBallDistance != Float.MAX_VALUE && lowDistance > lastBallDistance + 5) {
					robot.getPilot().stop();
					System.out.println("Ball distance increasing! Was " + String.format("%.1f", lastBallDistance) + 
									 "cm, now " + String.format("%.1f", lowDistance) + "cm. Sweeping...");
					if (sweepForBall()) {
						System.out.println("Ball realigned! Continuing...");
						robot.getPilot().forward();
						lastBallDistance = Float.MAX_VALUE; 
						lostBallCount = 0;
					} else {
						System.out.println("Could not realign to ball.");
						break;
					}
				} else {
					lastBallDistance = lowDistance;
					lostBallCount = 0;
					
					if (lowDistance < 30) {
						robot.getPilot().stop();
						System.out.println("*** REACHED BALL! Stopping at " + String.format("%.1f", lowDistance) + " cm ***");
						break;
					}
				}
			} else {
				lostBallCount++;
				System.out.println("No detection: Low=" + String.format("%.1f", lowDistance) + 
								 "cm, High=" + String.format("%.1f", highDistance) + 
								 "cm (lost count: " + lostBallCount + ")");
				
				if (lostBallCount == 8) {
					robot.getPilot().stop();
					System.out.println("Lost ball, sweeping to reacquire...");
					
					if (sweepForBall()) {
						System.out.println("Ball reacquired! Continuing...");
						robot.getPilot().forward();
						lostBallCount = 0;
					} else {
						System.out.println("Ball not found in sweep, stopping.");
						break;
					}
				} else if (lostBallCount >= 15) {
					robot.getPilot().stop();
					System.out.println("Ball lost for too long, stopping.");
					break;
				}
			}
			
			if (highDistance < 15 && highDistance > 0 && 
				(lowDistance > highDistance || lowDistance > 100)) {
				robot.getPilot().stop();
				System.out.println("Obstacle detected by high sensor! Stopping.");
				break;
			}
			
			sleep(100);
		}
		
		stop();
		System.out.println("Navigation complete!");
		lejos.hardware.Sound.beepSequenceUp();
	}
	
	private boolean sweepForBall() {
		float[] sweepAngles = {-15, 30, -30, 15}; 
		
		for (float angle : sweepAngles) {
			rotateBy(angle);
			
			sleep(200);
			
			float lowDistance = robot.getNXTDistance();
			float highDistance = robot.getEV3Distance();
			
			if (lowDistance > 2 && lowDistance < 50 && 
				(highDistance > lowDistance + 10 || highDistance > 100)) {
				System.out.println("  -> Ball found at " + String.format("%.1f", lowDistance) + " cm!");
				return true;
			}
		}
		
		return false; 
	}
	
	private void addObstacleToMap(float centerX, float centerY) {
		float half = OBSTACLE_PADDING / 2;
		
		obstacleLines.add(new Line(centerX - half, centerY - half, centerX + half, centerY - half));
		obstacleLines.add(new Line(centerX + half, centerY - half, centerX + half, centerY + half));
		obstacleLines.add(new Line(centerX + half, centerY + half, centerX - half, centerY + half));
		obstacleLines.add(new Line(centerX - half, centerY + half, centerX - half, centerY - half));
	}
	
	private void updateMapWithObstacles() {
		ArrayList<Line> allLines = getBoundaryLines();
		allLines.addAll(obstacleLines);
		currentMap = createLineMap(allLines);
		System.out.println("Map updated with " + obstacleLines.size()/4 + " obstacles");
	}
	
	private LineMap initializeBaseMap() {
		return createLineMap(getBoundaryLines());
	}
	
	private void rotateBy(float degrees) {
		robot.getPilot().rotate(degrees, true);
		while (isMoving()) {
			checkEscape();
			sleep(50);
		}
	}

	private void stop() {
		robot.getPilot().stop();
	}

	private void forward() {
		robot.getPilot().forward();
	}

	private boolean isMoving() {
		return robot.getPilot().isMoving();
	}

	private void captureBall() {
		System.out.println("Capturing ball...");
		
		robot.getPilot().setLinearSpeed(3); 
		
		robot.getArmMotor().setSpeed(robot.getArmMotor().getMaxSpeed());
		
		forward();
		
		int startTacho = robot.getLeftMotor().getTachoCount();
		float degreesPerCm = 20.5f; 
		
		boolean armLifted = false;
		
		while (isMoving()) {
			checkEscape();
			int currentTacho = robot.getLeftMotor().getTachoCount();
			float distanceTraveled = Math.abs(currentTacho - startTacho) / degreesPerCm;
			
			float irDist = robot.getIRDistance();
			
			if (!armLifted && distanceTraveled >= 5.0f) {
				System.out.println("Lifting arm...");
				robot.getArmMotor().rotate(90, true); 
				armLifted = true;
				isArmDown = false;
			}
			
			if (irDist < 5.0f) { 
				System.out.println("IR detected ball! Stopping.");
				break;
			}
			
			if (distanceTraveled > 35.0f) { 
				System.out.println("IR not detected. Stopping at max distance (35cm).");
				break;
			}
			
			sleep(10);
		}
		
		stop();
		if (!armLifted) {
			robot.getArmMotor().rotate(90, false);
			isArmDown = false;
		}
		
		System.out.println("Trapping ball (lowering arm)...");
		robot.getArmMotor().rotate(-90);
		isArmDown = true;
	}

	private boolean deliverBallToGoal(Waypoint goal) {
		return navigateWithPathPlanning(goal, false);
	}

	private void releaseBall() {
		System.out.println("Releasing ball...");
		robot.getArmMotor().rotate(90);
		isArmDown = false;
	}
	
	private void resetArm() {
		if (isArmDown) {
			System.out.println("Arm is already down.");
			return;
		}
		System.out.println("Resetting arm to DOWN position...");
		robot.getArmMotor().rotate(-90);
		isArmDown = true;
	}

	private void sleep(long millis) {
		try { Thread.sleep(millis); } catch (Exception e) {}
	}

	private ArrayList<Line> getBoundaryLines() {
		ArrayList<Line> lines = new ArrayList<>();
		lines.add(new Line(0.0f, 0.0f, FIELD_WIDTH, 0.0f));
		lines.add(new Line(0.0f, 0.0f, 0.0f, FIELD_HEIGHT));
		lines.add(new Line(0.0f, FIELD_HEIGHT, FIELD_WIDTH, FIELD_HEIGHT));
		lines.add(new Line(FIELD_WIDTH, FIELD_HEIGHT, FIELD_WIDTH, 0.0f));
		return lines;
	}

	private LineMap createLineMap(ArrayList<Line> lines) {
		Line[] lineArr = lines.toArray(new Line[lines.size()]);
		return new LineMap(lineArr, new Rectangle(0.0f, 0.0f, FIELD_WIDTH, FIELD_HEIGHT));
	}

	private void cleanup() {
		robot.stopThreads();
		robot.close();
		System.out.println("Sensors closed.");
	}
}
