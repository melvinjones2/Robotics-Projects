package client;

import java.io.*;
import shared.SocketConnection;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class Client {
	private static EV3UltrasonicSensor ultrasonicSensor;
	private static EV3IRSensor irSensor;
	
	public static void main(String[] args)
	{
		initSensors();
		connectAndCommunicate();
		closeSensors();
	}

	private static void connectAndCommunicate() {
		try (SocketConnection connection = createConnection()) {
			connection.sendLog("Client connected to server");

			connection.sendLine("Hello, Server!");
			connection.sendLog("Sent greeting to server");
			
			String response = connection.readLine();
			connection.sendLog("Received response: " + response);
			System.out.println("Server response: " + response);

		} catch (IOException e) {
			System.err.println("Client error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static SocketConnection createConnection() throws IOException {
		return SocketConnection.createClient();
	}

	private static void initSensors() {
		ultrasonicSensor = new EV3UltrasonicSensor(SensorPort.S1);
		irSensor = new EV3IRSensor(SensorPort.S2);
		System.out.println("Sensors initialized: Ultrasonic on S1, IR on S2");
	}
	
	private static void closeSensors() {
		if (ultrasonicSensor != null) {
			ultrasonicSensor.close();
		}
		if (irSensor != null) {
			irSensor.close();
		}
	}
}
