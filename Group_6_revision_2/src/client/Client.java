package client;

import java.io.*;
import shared.SocketConnection;
import client.CommandHandler;
import lejos.hardware.port.SensorPort;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.NXTUltrasonicSensor;
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
	private static NXTUltrasonicSensor nxtUltrasonicSensor;
	private static Port nxtUltraSonicPort = SensorPort.S4;
	private static Port ultraSonicPort = SensorPort.S2;
	private static Port irPort = SensorPort.S1;
	private static Port motorPortLeft = MotorPort.B;
	private static Port motorPortRight = MotorPort.C;
	
	private static MovePilot pilot;
	private static Chassis chassis;
	private static NavigationController navController;
	private static SensorDataCollection sensorData;
	private static BallTracker ballTracker;
	private static SafetyMonitor safetyMonitor;
	
	public static void main(String[] args)
	{
		try {
			connectToServer();
			initSensors();
			initSensorDataCollection();
			initMotors();
			initSafety();
			initNavigation();
			communicate();
		} catch (IOException e) {
			log("Client error: " + e.getMessage());
		} finally {
			stopSafety();
			stopSensorDataCollection();
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
			try {
				String command = connection.readLine();
				if (command == null || command.equals("EXIT")) {
					log("Disconnecting from server");
					break;
				}
				
				if (connection.isLogMessage(command)) {
					continue; // Skip log messages from server
				}
				
				CommandHandler.handleCommand(command, pilot, connection);
			} catch (java.net.SocketTimeoutException e) {
				// Timeout is normal when waiting for commands, just continue
				continue;
			} catch (IOException e) {
				// Other IO errors are real connection problems
				log("Connection error: " + e.getMessage());
				throw e;
			}
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
		// EV3 Ultrasonic (high - obstacle avoidance) on S2
		try {
			ultrasonicSensor = new EV3UltrasonicSensor(ultraSonicPort);
			log("EV3 Ultrasonic (high) initialized on S2 - obstacle avoidance");
		} catch (Exception e) {
			log("Failed to initialize EV3 ultrasonic sensor: " + e.getMessage());
			ultrasonicSensor = null;
		}

		// NXT Ultrasonic (low - ball detection) on S4
		try {
			nxtUltrasonicSensor = new NXTUltrasonicSensor(nxtUltraSonicPort);
			log("NXT Ultrasonic (low) initialized on S4 - ball detection");
		} catch (Exception e) {
			log("Failed to initialize NXT ultrasonic sensor: " + e.getMessage());
			nxtUltrasonicSensor = null;
		}
		
		// IR Sensor (behind NXT) on S1 - for future use
		try {
			irSensor = new EV3IRSensor(irPort);
			log("IR sensor initialized on S1 - future use");
		} catch (Exception e) {
			log("IR sensor not available: " + e.getMessage());
			irSensor = null;
		}
	}
	
	private static void closeSensors() {
		if (ultrasonicSensor != null) {
			ultrasonicSensor.close();
		}
		
		if (nxtUltrasonicSensor != null) {
			nxtUltrasonicSensor.close();
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
		navController = new NavigationController(pilot, sensorData, connection);
		CommandHandler.setNavigationController(navController);
		log("Navigation controller initialized");
		
		ballTracker = new BallTracker(pilot, sensorData, connection);
		CommandHandler.setBallTracker(ballTracker);
		log("Ball tracker initialized");
	}
	
	private static void initSensorDataCollection() {
		sensorData = new SensorDataCollection(ultrasonicSensor, nxtUltrasonicSensor, irSensor, connection);
		sensorData.startReading();
		log("Sensor data collection started (3 sensors: EV3-High, NXT-Low, IR)");
	}
	
	private static void stopSensorDataCollection() {
		if (sensorData != null) {
			sensorData.stopReading();
		}
	}
	
	public static SensorDataCollection getSensorData() {
		return sensorData;
	}
	
	private static void initSafety() {
		safetyMonitor = new SafetyMonitor(pilot, sensorData, connection);
		safetyMonitor.startMonitoring();
		CommandHandler.setSafetyMonitor(safetyMonitor);
		log("Safety monitor initialized");
	}
	
	private static void stopSafety() {
		if (safetyMonitor != null) {
			safetyMonitor.stopMonitoring();
		}
	}
	
	public static SafetyMonitor getSafetyMonitor() {
		return safetyMonitor;
	}
}
