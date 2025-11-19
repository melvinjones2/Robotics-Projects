package client.network;

import client.data.SensorDataWarehouse;
import client.sensor.ISensor;
import common.ProtocolConstants;
import common.SensorData;
import lejos.hardware.lcd.LCD;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads sensors periodically and:
 */
public class SensorThread implements Runnable {
    
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final List<ISensor> sensors;
    private final int pollIntervalMs;
    private final SensorDataWarehouse warehouse;
    
    /**
     * Constructor with warehouse dependency injection.
     */
    public SensorThread(BufferedWriter out, AtomicBoolean running, 
                        List<ISensor> sensors, int pollIntervalMs, 
                        SensorDataWarehouse warehouse) {
        this.out = out;
        this.running = running;
        this.sensors = sensors;
        this.pollIntervalMs = pollIntervalMs;
        this.warehouse = warehouse;
    }
    
    @Override
    public void run() {
        for (int i = 2; i <= 7; i++) {
            LCD.clear(i);
        }
        
        while (running.get()) {
            try {
                // Use type-safe sensor data builder
                SensorData data = new SensorData();
                int lcdLine = 2;
                
                for (ISensor sensor : sensors) {
                    if (!sensor.isAvailable()) continue;
                    
                    try {
                        String sensorReading = sensor.readValue();
                        if (sensorReading != null) {
                            // Parse sensor name and value
                            String[] parts = sensorReading.split("=");
                            if (parts.length == 2) {
                                try {
                                    String name = parts[0].trim();
                                    float value = Float.parseFloat(parts[1].split(",")[0].trim());
                                    
                                    // Store in warehouse for autonomous task access
                                    warehouse.store(name, value);
                                    
                                    // Add to network message
                                    data.put(name, value);
                                    
                                    // Display on LCD
                                    if (lcdLine < 8) {
                                        LCD.clear(lcdLine);
                                        String display = sensorReading.substring(0, Math.min(18, sensorReading.length()));
                                        LCD.drawString(display, 0, lcdLine);
                                        lcdLine++;
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignore invalid sensor data
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                
                if (!data.isEmpty()) {
                    // Thread-safe write (synchronized to prevent interleaving with other threads)
                    synchronized (out) {
                        out.write(ProtocolConstants.buildSensorMessage(data) + "\n");
                        out.flush();
                    }
                }
                
                Thread.sleep(pollIntervalMs);
            } catch (IOException e) {
                LCD.clear(1);
                LCD.drawString("Conn Lost", 0, 1);
                running.set(false);
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
            }
        }
        
        for (ISensor sensor : sensors) {
            try {
                sensor.close();
            } catch (Exception e) {
            }
        }
    }
}
