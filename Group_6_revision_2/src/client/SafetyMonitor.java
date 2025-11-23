package client;

import lejos.robotics.navigation.MovePilot;
import shared.SocketConnection;
import java.io.IOException;

public class SafetyMonitor {
	private MovePilot pilot;
	private SensorDataCollection sensorData;
	private SocketConnection connection;
	private Thread monitorThread;
	private volatile boolean running;
	private volatile boolean emergencyStopRequested;
	
	private static final double EMERGENCY_STOP_DISTANCE = 5.0; // cm - stop if closer
	private static final int CHECK_INTERVAL_MS = 50; // Check every 50ms
	
	public SafetyMonitor(MovePilot pilot, SensorDataCollection sensorData, SocketConnection connection) {
		this.pilot = pilot;
		this.sensorData = sensorData;
		this.connection = connection;
		this.running = false;
		this.emergencyStopRequested = false;
	}
	
	public void startMonitoring() {
		if (running) {
			return;
		}
		
		running = true;
		emergencyStopRequested = false;
		
		monitorThread = new Thread(new Runnable() {
			public void run() {
				safetyLoop();
			}
		});
		monitorThread.setDaemon(true);
		monitorThread.start();
		
		log("Safety monitor started");
	}
	
	public void stopMonitoring() {
		running = false;
		if (monitorThread != null) {
			try {
				monitorThread.join(1000);
			} catch (InterruptedException e) {
				// Continue cleanup
			}
		}
		log("Safety monitor stopped");
	}
	
	public void requestEmergencyStop() {
		emergencyStopRequested = true;
		pilot.stop();
		log("EMERGENCY STOP REQUESTED");
	}
	
	public void clearEmergencyStop() {
		emergencyStopRequested = false;
		log("Emergency stop cleared");
	}
	
	public boolean isEmergencyStopped() {
		return emergencyStopRequested;
	}
	
	private void safetyLoop() {
		while (running) {
			try {
				// Check if emergency stop was requested externally
				if (emergencyStopRequested) {
					if (pilot.isMoving()) {
						pilot.stop();
					}
					Thread.sleep(CHECK_INTERVAL_MS);
					continue;
				}
				
				// Check if robot is moving forward
				if (pilot.isMoving()) {
					float distance = sensorData.getBestDistance();
					
					// Emergency stop if too close to obstacle
					if (distance < EMERGENCY_STOP_DISTANCE) {
						pilot.stop();
						log("SAFETY STOP: Obstacle detected at " + distance + " cm");
						emergencyStopRequested = true;
					}
				}
				
				Thread.sleep(CHECK_INTERVAL_MS);
				
			} catch (InterruptedException e) {
				break;
			} catch (Exception e) {
				// Continue monitoring even on errors
			}
		}
	}
	
	private void log(String message) {
		try {
			if (connection != null) {
				connection.sendLog(message);
			}
		} catch (IOException e) {
			// Silent fail
		}
	}
}
