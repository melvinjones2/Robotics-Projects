package server;

import java.io.*;
import shared.CommandBuilder;
import static shared.Constants.*;
import shared.SocketConnection;

public class Server {
	public static void main(String[] args)
	{
		// Check if CLI mode is requested
		if (args.length > 0 && args[0].equals("--cli")) {
			startServer();
		} else {
			// Default to GUI
			ServerGUI.main(args);
		}
	}

	private static void startServer() {
		System.out.println("Server starting on port " + DEFAULT_PORT + "...");
		
		try (SocketConnection connection = createServerConnection()) {
			System.out.println("Client connected");
			
			// Listen for initial client logs
			listenForLogs(connection, 5);
			
			// Choose which test to run:
			// testCommands(connection);           // Basic movement commands
			testNavigationStrategies(connection);  // Navigation with obstacle avoidance

		} catch (IOException | InterruptedException e) {
			System.err.println("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void testCommands(SocketConnection connection) throws IOException {
		System.out.println("\n=== Testing Basic Robot Commands ===\n");
		
		// Test 1: Set speed
		sendCommand(connection, CommandBuilder.build(CommandBuilder.SPEED, 15));
		
		// Test 2: Move forward
		sendCommand(connection, CommandBuilder.build(CommandBuilder.MOVE, 50));
		
		// Test 3: Rotate
		sendCommand(connection, CommandBuilder.build(CommandBuilder.ROTATE, 90));
		
		// Test 4: Move backward
		sendCommand(connection, CommandBuilder.build(CommandBuilder.MOVE, -30));
		
		// Test 5: Rotate back
		sendCommand(connection, CommandBuilder.build(CommandBuilder.ROTATE, -90));
		
		// Test 6: Stop
		sendCommand(connection, CommandBuilder.build(CommandBuilder.STOP));
		
		// Test 7: Exit
		System.out.println("\nSending EXIT command...");
		connection.sendLine(CommandBuilder.build(CommandBuilder.EXIT));
		
		System.out.println("\n=== Test Complete ===");
	}
	
	private static void testNavigationStrategies(SocketConnection connection) throws IOException, InterruptedException {
		System.out.println("\n=== Testing Navigation Strategies ===\n");
		
		// Set a moderate speed for navigation
		sendCommand(connection, CommandBuilder.build(CommandBuilder.SPEED, 10));
		
		// Strategy 1: Simple Obstacle Avoidance
		System.out.println("--- Test 1: Simple Obstacle Avoidance ---");
		sendCommandWithLogs(connection, CommandBuilder.build(CommandBuilder.NAV_SIMPLE, 100));
		
		// Wait and rotate for next test
		Thread.sleep(2000);
		sendCommand(connection, CommandBuilder.build(CommandBuilder.ROTATE, 90));
		
		// Strategy 2: Dynamic Navigation
		System.out.println("\n--- Test 2: Dynamic Navigation ---");
		sendCommandWithLogs(connection, CommandBuilder.build(CommandBuilder.NAV_DYNAMIC, 80));
		
		// Wait and rotate for next test
		Thread.sleep(2000);
		sendCommand(connection, CommandBuilder.build(CommandBuilder.ROTATE, 90));
		
		// Strategy 3: Navigate to Goal (Waypoint)
		System.out.println("\n--- Test 3: Navigate to Goal (50, 50) ---");
		sendCommandWithLogs(connection, CommandBuilder.build(CommandBuilder.NAV_GOTO, 50, 50));
		
		// Stop and exit
		System.out.println("\n--- Stopping Robot ---");
		sendCommand(connection, CommandBuilder.build(CommandBuilder.STOP));
		
		System.out.println("\nSending EXIT command...");
		connection.sendLine(CommandBuilder.build(CommandBuilder.EXIT));
		
		System.out.println("\n=== Navigation Test Complete ===");
	}
	
	private static void sendCommandWithLogs(SocketConnection connection, String command) throws IOException {
		System.out.println("Sending: " + command);
		connection.sendLine(command);
		
		// Wait for immediate response
		String response = connection.readLine();
		System.out.println("Response: " + response);
		
		// Listen for logs until completion message
		System.out.println("Monitoring navigation logs...");
		while (true) {
			String line = connection.readLine();
			if (line == null) break;
			
			if (connection.isLogMessage(line)) {
				String log = connection.extractLogMessage(line);
				System.out.println("[CLIENT LOG] " + log);
				
				// Stop when we see completion message
				if (log.contains("complete")) {
					break;
				}
			} else {
				System.out.println("[CLIENT] " + line);
			}
		}
		System.out.println();
	}
	
	private static void sendCommand(SocketConnection connection, String command) throws IOException {
		System.out.println("Sending: " + command);
		connection.sendLine(command);
		
		// Wait for response
		String response = connection.readLine();
		System.out.println("Response: " + response);
		
		// Listen for any logs
		listenForLogs(connection, 2);
		System.out.println();
	}
	
	private static void listenForLogs(SocketConnection connection, int maxLogs) throws IOException {
		for (int i = 0; i < maxLogs; i++) {
			String line = connection.readLine();
			if (line == null) break;
			
			if (connection.isLogMessage(line)) {
				String log = connection.extractLogMessage(line);
				System.out.println("[CLIENT LOG] " + log);
			} else {
				System.out.println("[CLIENT] " + line);
			}
		}
	}

	private static SocketConnection createServerConnection() throws IOException {
		return SocketConnection.createServer();
	}
}
