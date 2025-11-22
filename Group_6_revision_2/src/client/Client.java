package client;

import java.io.*;
import shared.SocketConnection;
import client.CommandHandler;
import lejos.hardware.port.SensorPort;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.motor.*;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import static shared.Constants.*;

public class Client {
	private static SocketConnection connection;
	private static EV3UltrasonicSensor ultrasonicSensor;
	private static EV3IRSensor irSensor;
	private static Port ultraSonicPort = SensorPort.S2;
	private static Port irPort = SensorPort.S1;
	private static Port motorPortLeft = MotorPort.B;
	private static Port motorPortRight = MotorPort.C;
	
	private static MovePilot pilot;
	private static Chassis chassis;
	private static NavigationController navController;
	
	public static void main(String[] args)
	{
		try {
			connectToServer();
			initSensors();
			initMotors();
			initNavigation();
			communicate();
		} catch (IOException e) {
			log("Client error: " + e.getMessage());
		} finally {
			closeSensors();
			closeMotors();
			closeConnection();
		}
	}

	private static void connectToServer() throws IOException {
		connection = createConnection();
		log("Client connected to server");
	}

	private static void communicate() throws IOException {
		log("Ready to receive commands");
		
		while (true) {
			String command = connection.readLine();
			if (command == null || command.equals("EXIT")) {
				log("Disconnecting from server");
				break;
			}
			
			if (connection.isLogMessage(command)) {
				continue; // Skip log messages from server
			}
			
			CommandHandler.handleCommand(command, pilot, connection);
		}
	}

	private static SocketConnection createConnection() throws IOException {
		return SocketConnection.createClient();
	}
	
	private static void log(String message) {
		try {
			if (connection != null) {
				connection.sendLog(message);
			}
		} catch (IOException e) {
			// Silent fail - cannot log if connection is down
		}
	}
	
	private static void closeConnection() {
		if (connection != null) {
			connection.close();
		}
	}

	private static void initSensors() {
		try {
			ultrasonicSensor = new EV3UltrasonicSensor(ultraSonicPort);
			ultrasonicSensor.getDistanceMode();
			log("Ultrasonic sensor initialized on S2 (Distance mode)");
		} catch (Exception e) {
			log("Failed to initialize ultrasonic sensor: " + e.getMessage());
		}
		
		try {
			irSensor = new EV3IRSensor(irPort);
			irSensor.getDistanceMode();
			log("IR sensor initialized on S1 (Distance mode)");
		} catch (Exception e) {
			log("Failed to initialize IR sensor: " + e.getMessage());
		}
	}
	
	private static void closeSensors() {
		if (ultrasonicSensor != null) {
			ultrasonicSensor.close();
		}
		if (irSensor != null) {
			irSensor.close();
		}
	}

	private static void initMotors() {
		EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(motorPortLeft);
		EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(motorPortRight);
		
		Wheel leftWheel = WheeledChassis.modelWheel(leftMotor, WHEEL_DIAMETER).offset(-TRACK_WIDTH / 2);
		Wheel rightWheel = WheeledChassis.modelWheel(rightMotor, WHEEL_DIAMETER).offset(TRACK_WIDTH / 2);
		
		chassis = new WheeledChassis(new Wheel[] { leftWheel, rightWheel }, WheeledChassis.TYPE_DIFFERENTIAL);
		pilot = new MovePilot(chassis);
		
		pilot.setLinearSpeed(10);  // cm/s
		pilot.setAngularSpeed(45); // degrees/s
		
		log("MovePilot initialized: Motors on B and C");
	}
	
	private static void closeMotors() {
		if (pilot != null) {
			pilot.stop();
		}
	}
	
	private static void initNavigation() {
		navController = new NavigationController(pilot, ultrasonicSensor, connection);
		CommandHandler.setNavigationController(navController);
		log("Navigation controller initialized");
	}
}
