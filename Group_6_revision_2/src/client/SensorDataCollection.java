package client;

import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.NXTUltrasonicSensor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;
import shared.SocketConnection;
import java.io.IOException;

public class SensorDataCollection {
	// EV3 Ultrasonic - high position for obstacle avoidance
	private volatile float ev3UltrasonicDistance;
	private volatile long lastEV3UltrasonicUpdate;
	private EV3UltrasonicSensor ev3UltrasonicSensor;
	private SampleProvider ev3UltrasonicProvider;
	
	// NXT Ultrasonic - low position for ball detection
	private volatile float nxtUltrasonicDistance;
	private volatile long lastNXTUltrasonicUpdate;
	private NXTUltrasonicSensor nxtUltrasonicSensor;
	private SampleProvider nxtUltrasonicProvider;
	
	// IR Sensor - behind NXT sensor (for future use)
	private volatile float irDistance;
	private volatile long lastIRUpdate;
	private EV3IRSensor irSensor;
	private SampleProvider irProvider;
	
	private volatile boolean running;
	private Thread sensorThread;
	private SocketConnection connection;
	
	private static final int UPDATE_INTERVAL_MS = 50; // Update every 50ms
	private static final int LOG_INTERVAL_MS = 1000; // Log every 1 second
	private long lastLogTime = 0;
	
	public SensorDataCollection(EV3UltrasonicSensor ev3Ultrasonic, NXTUltrasonicSensor nxtUltrasonic, 
	                            EV3IRSensor irSensor, SocketConnection connection) {
		this.ev3UltrasonicSensor = ev3Ultrasonic;
		this.nxtUltrasonicSensor = nxtUltrasonic;
		this.irSensor = irSensor;
		this.connection = connection;
		
		this.ev3UltrasonicDistance = -1;
		this.nxtUltrasonicDistance = -1;
		this.irDistance = -1;
		this.running = false;
		
		// Initialize EV3 Ultrasonic (high - obstacle avoidance)
		if (ev3UltrasonicSensor != null) {
			try {
				this.ev3UltrasonicProvider = ev3UltrasonicSensor.getDistanceMode();
			} catch (Exception e) {
				this.ev3UltrasonicSensor = null;
			}
		}
		
		// Initialize NXT Ultrasonic (low - ball detection)
		if (nxtUltrasonicSensor != null) {
			try {
				this.nxtUltrasonicProvider = nxtUltrasonicSensor.getDistanceMode();
			} catch (Exception e) {
				this.nxtUltrasonicSensor = null;
			}
		}
		
		// Initialize IR Sensor (for future use)
		if (irSensor != null) {
			try {
				this.irProvider = irSensor.getDistanceMode();
			} catch (Exception e) {
				this.irSensor = null;
			}
		}
	}
	
	public void startReading() {
		if (running) {
			return;
		}
		
		running = true;
		sensorThread = new Thread(new Runnable() {
			public void run() {
				readSensorLoop();
			}
		});
		sensorThread.setDaemon(true);
		sensorThread.start();
	}
	
	public void stopReading() {
		running = false;
		if (sensorThread != null) {
			try {
				sensorThread.join(1000);
			} catch (InterruptedException e) {
				// Continue cleanup
			}
		}
	}
	
	private void readSensorLoop() {
		float[] ev3UltrasonicSample = null;
		float[] nxtUltrasonicSample = null;
		float[] irSample = null;
		
		if (ev3UltrasonicProvider != null) {
			ev3UltrasonicSample = new float[ev3UltrasonicProvider.sampleSize()];
		}
		
		if (nxtUltrasonicProvider != null) {
			nxtUltrasonicSample = new float[nxtUltrasonicProvider.sampleSize()];
		}
		
		if (irProvider != null) {
			irSample = new float[irProvider.sampleSize()];
		}
		
		while (running) {
			try {
				// Read EV3 Ultrasonic (high - obstacle avoidance)
				if (ev3UltrasonicProvider != null && ev3UltrasonicSample != null) {
					try {
						ev3UltrasonicProvider.fetchSample(ev3UltrasonicSample, 0);
						float rawValue = ev3UltrasonicSample[0] * 100; // Convert meters to cm
						
						if (Float.isInfinite(rawValue) || rawValue > 255) {
							ev3UltrasonicDistance = 255; // Max range
						} else if (rawValue < 0) {
							ev3UltrasonicDistance = -1; // Invalid
						} else {
							ev3UltrasonicDistance = rawValue;
						}
						lastEV3UltrasonicUpdate = System.currentTimeMillis();
					} catch (Exception e) {
						// Read failed, keep old value
					}
				}
				
				// Read NXT Ultrasonic (low - ball detection)
				if (nxtUltrasonicProvider != null && nxtUltrasonicSample != null) {
					try {
						nxtUltrasonicProvider.fetchSample(nxtUltrasonicSample, 0);
						float rawValue = nxtUltrasonicSample[0] * 100; // Convert meters to cm
						
						if (Float.isInfinite(rawValue) || rawValue > 255) {
							nxtUltrasonicDistance = 255; // Max range
						} else if (rawValue < 0) {
							nxtUltrasonicDistance = -1; // Invalid
						} else {
							nxtUltrasonicDistance = rawValue;
						}
						lastNXTUltrasonicUpdate = System.currentTimeMillis();
					} catch (Exception e) {
						// Read failed, keep old value
					}
				}
				
				// Read IR Sensor (for future use)
				if (irProvider != null && irSample != null) {
					try {
						irProvider.fetchSample(irSample, 0);
						float rawValue = irSample[0];
						
						if (rawValue >= 0 && rawValue <= 100) {
							irDistance = rawValue; // 0-100 scale
						}
						lastIRUpdate = System.currentTimeMillis();
					} catch (Exception e) {
						// Read failed, keep old value
					}
				}
				
				// Log sensor data periodically
				long currentTime = System.currentTimeMillis();
				if (currentTime - lastLogTime >= LOG_INTERVAL_MS) {
					logSensorData();
					lastLogTime = currentTime;
				}
				
				Thread.sleep(UPDATE_INTERVAL_MS);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	// ========== EV3 ULTRASONIC (HIGH - OBSTACLE AVOIDANCE) ==========
	
	public float getObstacleDistance() {
		if (ev3UltrasonicDistance < 0 || Float.isInfinite(ev3UltrasonicDistance) || ev3UltrasonicDistance > 255) {
			return 255; // Max range or invalid
		}
		return ev3UltrasonicDistance;
	}
	
	public long getObstacleDataAge() {
		if (lastEV3UltrasonicUpdate == 0) {
			return -1; // No data yet
		}
		return System.currentTimeMillis() - lastEV3UltrasonicUpdate;
	}
	
	public boolean hasValidObstacleData() {
		return ev3UltrasonicProvider != null && lastEV3UltrasonicUpdate > 0 && getObstacleDataAge() < 500;
	}
	
	// ========== NXT ULTRASONIC (LOW - BALL DETECTION) ==========
	
	public float getBallDistance() {
		if (nxtUltrasonicDistance < 0 || Float.isInfinite(nxtUltrasonicDistance) || nxtUltrasonicDistance > 255) {
			return 255; // Max range or invalid
		}
		return nxtUltrasonicDistance;
	}
	
	public long getBallDataAge() {
		if (lastNXTUltrasonicUpdate == 0) {
			return -1; // No data yet
		}
		return System.currentTimeMillis() - lastNXTUltrasonicUpdate;
	}
	
	public boolean hasValidBallData() {
		return nxtUltrasonicProvider != null && lastNXTUltrasonicUpdate > 0 && getBallDataAge() < 500;
	}
	
	// ========== IR SENSOR (FUTURE USE) ==========
	
	public float getIRDistance() {
		if (irDistance < 0 || Float.isInfinite(irDistance) || irDistance > 100) {
			return 100; // Max range for IR or invalid
		}
		return irDistance;
	}
	
	public long getIRDataAge() {
		if (lastIRUpdate == 0) {
			return -1; // No data yet
		}
		return System.currentTimeMillis() - lastIRUpdate;
	}
	
	public boolean hasValidIRData() {
		return irProvider != null && lastIRUpdate > 0 && getIRDataAge() < 500;
	}
	
	// ========== LEGACY COMPATIBILITY METHODS ==========
	
	/**
	 * @deprecated Use getObstacleDistance() instead - this returns the high EV3 ultrasonic for obstacle avoidance
	 */
	public float getUltrasonicDistance() {
		return getObstacleDistance();
	}
	
	/**
	 * @deprecated Use getObstacleDataAge() instead
	 */
	public long getUltrasonicDataAge() {
		return getObstacleDataAge();
	}
	
	/**
	 * @deprecated Use hasValidObstacleData() instead
	 */
	public boolean hasValidUltrasonicData() {
		return hasValidObstacleData();
	}
	
	/**
	 * Get the best distance for general navigation/obstacle avoidance
	 * Uses the high EV3 ultrasonic sensor
	 */
	public float getBestDistance() {
		return getObstacleDistance();
	}
	
	/**
	 * Get distance with reason for debugging
	 */
	public float getBestDistanceWithReason(String[] reasonOut) {
		float obstacleDistance = getObstacleDistance();
		
		if (hasValidObstacleData() && obstacleDistance > 0) {
			if (reasonOut != null && reasonOut.length > 0) {
				reasonOut[0] = "EV3 Ultrasonic (high)";
			}
			return obstacleDistance;
		}
		
		if (reasonOut != null && reasonOut.length > 0) {
			reasonOut[0] = "No valid sensor data";
		}
		return 255;
	}
	
	// ========== LOGGING ==========
	
	private void logSensorData() {
		if (connection == null) {
			return;
		}
		
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("[SENSORS] ");
			
			// EV3 Ultrasonic (high - obstacle avoidance)
			if (ev3UltrasonicProvider != null) {
				sb.append("EV3-High: ");
				if (hasValidObstacleData()) {
					sb.append(String.format("%.1f cm", ev3UltrasonicDistance));
				} else {
					sb.append("NO DATA");
				}
			} else {
				sb.append("EV3-High: DISABLED");
			}
			
			sb.append(" | ");
			
			// NXT Ultrasonic (low - ball detection)
			if (nxtUltrasonicProvider != null) {
				sb.append("NXT-Low: ");
				if (hasValidBallData()) {
					sb.append(String.format("%.1f cm", nxtUltrasonicDistance));
				} else {
					sb.append("NO DATA");
				}
			} else {
				sb.append("NXT-Low: DISABLED");
			}
			
			sb.append(" | ");
			
			// IR Sensor (future use)
			if (irProvider != null) {
				sb.append("IR: ");
				if (hasValidIRData()) {
					sb.append(String.format("%.1f cm", irDistance));
				} else {
					sb.append("NO DATA");
				}
			} else {
				sb.append("IR: DISABLED");
			}
			
			connection.sendLog(sb.toString());
		} catch (IOException e) {
			// Logging failed, continue
		}
	}
}
