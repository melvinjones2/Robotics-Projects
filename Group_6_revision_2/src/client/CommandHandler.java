package client;

import java.io.IOException;
import shared.SocketConnection;
import shared.CommandBuilder;
import lejos.robotics.navigation.MovePilot;

public class CommandHandler {
	private static NavigationController navController;
	private static BallTracker ballTracker;
	private static SafetyMonitor safetyMonitor;
	
	public static void setNavigationController(NavigationController controller) {
		navController = controller;
	}
	
	public static void setBallTracker(BallTracker tracker) {
		ballTracker = tracker;
	}
	
	public static void setSafetyMonitor(SafetyMonitor monitor) {
		safetyMonitor = monitor;
	}
	
	public static void handleCommand(String commandString, MovePilot pilot, SocketConnection connection) {
			try {
				String cmd = CommandBuilder.getCommand(commandString);
				if (cmd.isEmpty()) {
					return;
				}
				
				// Check if emergency stop is active (except for emergency stop commands)
				if (safetyMonitor != null && safetyMonitor.isEmergencyStopped() && 
					!cmd.equals(CommandBuilder.EMERGENCY_STOP) && 
					!cmd.equals(CommandBuilder.CLEAR_STOP) &&
					!cmd.equals(CommandBuilder.STOP)) {
					connection.sendLine("ERROR: Emergency stop active. Send CLEAR_STOP first.");
					return;
				}
				
			switch (cmd) {
				case CommandBuilder.MOVE:
					Double distance = CommandBuilder.getParameterAsDouble(commandString, 0);
					if (distance != null) {
						connection.sendLine("OK");
						connection.sendLog("Moving " + distance + " cm");
						pilot.travel(distance);
						connection.sendLog("Move complete");
					} else {
						connection.sendLine("ERROR: MOVE requires distance parameter");
					}
					break;
					
				case CommandBuilder.ROTATE:
					Double angle = CommandBuilder.getParameterAsDouble(commandString, 0);
					if (angle != null) {
						connection.sendLine("OK");
						connection.sendLog("Rotating " + angle + " degrees");
						pilot.rotate(angle);
						connection.sendLog("Rotation complete");
					} else {
						connection.sendLine("ERROR: ROTATE requires angle parameter");
					}
					break;
					
				case CommandBuilder.STOP:
					connection.sendLog("Stopping robot");
					pilot.stop();
					connection.sendLine("OK");
					break;
					
				case CommandBuilder.SPEED:
					Double speed = CommandBuilder.getParameterAsDouble(commandString, 0);
					if (speed != null) {
						pilot.setLinearSpeed(speed);
						connection.sendLog("Speed set to " + speed + " cm/s");
						connection.sendLine("OK");
					} else {
						connection.sendLine("ERROR: SPEED requires speed parameter");
					}
					break;
					
				case CommandBuilder.NAV_SIMPLE:
					if (navController == null) {
						connection.sendLine("ERROR: Navigation not initialized");
						break;
					}
					Double navDistance = CommandBuilder.getParameterAsDouble(commandString, 0);
					if (navDistance != null) {
						connection.sendLine("OK");
						connection.sendLog("Starting simple navigation: " + navDistance + " cm");
						navController.simpleAvoidanceMove(navDistance);
						connection.sendLog("Simple navigation complete");
					} else {
						connection.sendLine("ERROR: NAV_SIMPLE requires distance parameter");
					}
					break;
					
				case CommandBuilder.NAV_GOTO:
					if (navController == null) {
						connection.sendLine("ERROR: Navigation not initialized");
						break;
					}
					Double targetX = CommandBuilder.getParameterAsDouble(commandString, 0);
					Double targetY = CommandBuilder.getParameterAsDouble(commandString, 1);
					if (targetX != null && targetY != null) {
						connection.sendLine("OK");
						connection.sendLog("Navigating to (" + targetX + ", " + targetY + ")");
						navController.navigateToGoal(targetX, targetY);
						connection.sendLog("Navigation to goal complete");
					} else {
						connection.sendLine("ERROR: NAV_GOTO requires X and Y coordinates");
					}
					break;
					
				case CommandBuilder.NAV_DYNAMIC:
					if (navController == null) {
						connection.sendLine("ERROR: Navigation not initialized");
						break;
					}
					Double dynDistance = CommandBuilder.getParameterAsDouble(commandString, 0);
					if (dynDistance != null) {
						connection.sendLine("OK");
						connection.sendLog("Starting dynamic navigation: " + dynDistance + " cm");
						navController.dynamicNavigationMove(dynDistance);
						connection.sendLog("Dynamic navigation complete");
					} else {
						connection.sendLine("ERROR: NAV_DYNAMIC requires distance parameter");
					}
					break;
					
				case CommandBuilder.FIND_BALL:
					if (ballTracker == null) {
						connection.sendLine("ERROR: Ball tracker not initialized");
						break;
					}
					connection.sendLine("OK");
					connection.sendLog("Searching for ball...");
					boolean found = ballTracker.findAndApproachBall();
					if (found) {
						connection.sendLog("Ball found and approached");
					} else {
						connection.sendLog("Ball not found");
					}
					break;
					
				case CommandBuilder.APPROACH_BALL:
					if (ballTracker == null) {
						connection.sendLine("ERROR: Ball tracker not initialized");
						break;
					}
					connection.sendLine("OK");
					connection.sendLog("Approaching ball...");
					ballTracker.approachBall();
					connection.sendLog("Approach complete");
					break;
					
				case CommandBuilder.TRACK_BALL:
					if (ballTracker == null) {
						connection.sendLine("ERROR: Ball tracker not initialized");
						break;
					}
					Double duration = CommandBuilder.getParameterAsDouble(commandString, 0);
					if (duration != null) {
						connection.sendLine("OK");
						connection.sendLog("Tracking ball for " + duration + " seconds");
						ballTracker.trackBall(duration.intValue());
						connection.sendLog("Tracking complete");
					} else {
						connection.sendLine("ERROR: TRACK_BALL requires duration parameter");
					}
					break;
					
				default:
					connection.sendLine("ERROR: Unknown command '" + cmd + "'");
					break;
			}
		} catch (IOException e) {
			// Connection error, will be handled by main loop
		}
	}
}
