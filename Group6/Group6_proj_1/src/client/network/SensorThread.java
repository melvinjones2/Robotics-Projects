package client.network;

import client.sensor.ISensor;
import lejos.hardware.lcd.LCD;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Reads sensors and sends data to server, displays on LCD lines 2-7
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
        for (int i = 2; i <= 7; i++) {
            LCD.clear(i);
        }
        
        while (running.get()) {
            try {
                StringBuilder sensorData = new StringBuilder("SENSOR:");
                int lcdLine = 2;
                int sensorCount = 0;
                
                for (ISensor sensor : sensors) {
                    if (!sensor.isAvailable()) continue;
                    
                    try {
                        String data = sensor.readValue();
                        if (data != null) {
                            if (sensorCount > 0) {
                                sensorData.append(",");
                            }
                            sensorData.append(data);
                            sensorCount++;
                            
                            if (lcdLine < 8) {
                                LCD.clear(lcdLine);
                                String display = data.substring(0, Math.min(18, data.length()));
                                LCD.drawString(display, 0, lcdLine);
                                lcdLine++;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                
                if (sensorCount > 0) {
                    synchronized (out) {
                        out.write(sensorData.toString() + "\n");
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
