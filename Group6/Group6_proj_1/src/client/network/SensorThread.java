package client.network;

import client.sensor.ISensor;
import lejos.hardware.lcd.LCD;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sensor thread - reads sensors and sends data to server.
 * No processing, just read and send. Displays on LCD lines 2-7.
 */
public class SensorThread implements Runnable {
    
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final List<ISensor> sensors;
    private final int pollIntervalMs;
    
    public SensorThread(BufferedWriter out, AtomicBoolean running, 
                             List<ISensor> sensors, int pollIntervalMs) {
        this.out = out;
        this.running = running;
        this.sensors = sensors;
        this.pollIntervalMs = pollIntervalMs;
    }
    
    @Override
    public void run() {
        // Clear sensor display area once at start
        for (int i = 2; i <= 7; i++) {
            LCD.clear(i);
        }
        
        while (running.get()) {
            try {
                // Collect all sensor readings
                StringBuilder sensorData = new StringBuilder("SENSOR:");
                int lcdLine = 2;  // Start at line 2 (line 0=status, line 1=command)
                int sensorCount = 0;
                
                for (ISensor sensor : sensors) {
                    if (!sensor.isAvailable()) continue;
                    
                    try {
                        String data = sensor.readValue();
                        if (data != null) {
                            // Add to combined message
                            if (sensorCount > 0) {
                                sensorData.append(",");
                            }
                            sensorData.append(data);
                            sensorCount++;
                            
                            // Display on LCD (lines 2-7, don't touch 0-1)
                            if (lcdLine < 8) {
                                LCD.clear(lcdLine);
                                // Truncate to 18 chars for LCD
                                String display = data.substring(0, Math.min(18, data.length()));
                                LCD.drawString(display, 0, lcdLine);
                                lcdLine++;
                            }
                        }
                    } catch (Exception e) {
                        // Skip this sensor reading if error
                    }
                }
                
                // Send combined sensor message to server
                if (sensorCount > 0) {
                    synchronized (out) {
                        out.write(sensorData.toString() + "\n");
                        out.flush();
                    }
                }
                
                Thread.sleep(pollIntervalMs);
            } catch (IOException e) {
                // Connection lost - show error on line 1
                LCD.clear(1);
                LCD.drawString("Conn Lost", 0, 1);
                running.set(false);
                break;
            } catch (InterruptedException e) {
                // Interrupted
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Other error - continue running
            }
        }
        
        // Cleanup
        for (ISensor sensor : sensors) {
            try {
                sensor.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
