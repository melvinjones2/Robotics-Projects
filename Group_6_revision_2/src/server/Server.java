package server;

import java.io.*;
import shared.SocketConnection;

public class Server {
	public static void main(String[] args)
	{
		startServer();
	}

	private static void startServer() {
		System.out.println("Server starting on port " + SocketConnection.DEFAULT_PORT + "...");
		
		try (SocketConnection connection = createServerConnection()) {
			System.out.println("Client connected");

			String message = connection.readLine();
			System.out.println("Client message: " + message);
			
			connection.sendLine("Hello, Client!");
			System.out.println("Response sent to client");

		} catch (IOException e) {
			System.err.println("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static SocketConnection createServerConnection() throws IOException {
		return SocketConnection.createServer();
	}
}
