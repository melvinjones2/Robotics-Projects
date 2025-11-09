package client.sensor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import client.config.RobotConfig;
import client.sensor.data.SensorDataStore;
import client.util.ClientLogger;

public class SensorThread implements Runnable {

    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final List<ISensor> sensors;
    private final SensorDataStore dataStore;

    public SensorThread(BufferedWriter out, AtomicBoolean running, List<ISensor> sensors, 
                       int frameCount, SensorDataStore dataStore) {
        this.out = out;
        this.running = running;
        this.sensors = sensors;
        this.dataStore = dataStore;
    }

    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            StringBuilder sensorBatch = new StringBuilder();
            
            for (ISensor sensor : sensors) {
                String data = sensor.readValue();
                if (data != null) {
                    // Store sensor data locally
                    storeSensorData(sensor.getName(), data);
                    
                    // Send individual sensor messages (original format)
                    try {
                        out.write(data + "\n");
                        out.flush();
                    } catch (java.io.IOException e) {
                        ClientLogger.error("Sensor send error", e);
                        try {
                            out.write(e.toString());
                        } catch (IOException e1) {
                            ClientLogger.error("Error writing error message", e1);
                        }
                    }
                    
                    // Accumulate for batch message
                    if (sensorBatch.length() > 0) {
                        sensorBatch.append(",");
                    }
                    sensorBatch.append(sensor.getName()).append("=");
                    // Extract numeric value from data
                    String value = extractValue(data);
                    sensorBatch.append(value);
                }
            }
            
            // Send batch message for server autonomous controller
            if (sensorBatch.length() > 0) {
                try {
                    out.write("SENSOR:" + sensorBatch.toString() + "\n");
                    out.flush();
                } catch (IOException e) {
                    ClientLogger.error("Sensor batch send error", e);
                }
            }
            
            try {
                Thread.sleep(RobotConfig.SENSOR_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
        for (ISensor sensor : sensors) {
            sensor.close();
        }
    }
    
    /**
     * Extract numeric value from sensor data string.
     */
    private String extractValue(String data) {
        int colonIdx = data.indexOf(':');
        if (colonIdx > 0) {
            String valueStr = data.substring(colonIdx + 1).trim();
            // Remove units if present
            valueStr = valueStr.replaceAll("[^0-9.\\-]", "");
            return valueStr;
        }
        return "0.0";
    }
    
    /**
     * Parse and store sensor data in the data store.
     */
    private void storeSensorData(String sensorName, String data) {
        if (dataStore == null) return;
        
        try {
            // Parse sensor data format: "SENSOR_TYPE: value"
            int colonIdx = data.indexOf(':');
            if (colonIdx > 0) {
                String valueStr = data.substring(colonIdx + 1).trim();
                
                // Remove units if present (cm, %, etc.)
                valueStr = valueStr.replaceAll("[^0-9.\\-]", "");
                
                float value = Float.parseFloat(valueStr);
                dataStore.store(sensorName.toLowerCase(), value);
                
                ClientLogger.debug(String.format("Stored %s: %.2f", sensorName, value));
            }
        } catch (NumberFormatException e) {
            ClientLogger.debug("Could not parse sensor value: " + data);
        }
    }
}