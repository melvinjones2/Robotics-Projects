package client;

import java.io.IOException;
import shared.SocketConnection;
import shared.CommandBuilder;
import lejos.robotics.navigation.MovePilot;

public class CommandHandler {
	private static NavigationController navController;
	
	public static void setNavigationController(NavigationController controller) {
		navController = controller;
	}
	
	public static void handleCommand(String commandString, MovePilot pilot, SocketConnection connection) {
		try {
			String cmd = CommandBuilder.getCommand(commandString);
			if (cmd.isEmpty()) {
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
					
				default:
					connection.sendLine("ERROR: Unknown command '" + cmd + "'");
					break;
			}
		} catch (IOException e) {
			// Connection error, will be handled by main loop
		}
	}
}
