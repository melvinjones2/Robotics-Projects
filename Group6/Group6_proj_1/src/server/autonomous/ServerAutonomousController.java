package server.autonomous;

import common.ProtocolConstants;
import common.SensorData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Server-side autonomous control - analyzes client sensor data and issues commands
public class ServerAutonomousController {
    
    private final Map<String, SensorReading> sensorData = new ConcurrentHashMap<>();
    
    private static final float OBSTACLE_DISTANCE_CM = 25.0f;
    private static final float SAFE_DISTANCE_CM = 50.0f;
    private static final float TOUCH_PRESSED = 1.0f;
    private static final float DANGEROUS_TILT = 25.0f;
    
    private volatile boolean enabled = false;
    private volatile long lastAnalysisTime = 0;
    private static final long ANALYSIS_INTERVAL_MS = 1000;
    
    public void updateSensor(String sensorName, float value) {
        sensorData.put(sensorName, new SensorReading(sensorName, value, System.currentTimeMillis()));
    }
    
    public void parseSensorMessage(String message) {
        // Use type-safe message parser
        SensorData data = ProtocolConstants.parseSensorMessage(message);
        
        // Debug: log if no sensors parsed
        boolean hasSensors = false;
        for (String sensorName : data.getSensorNames()) {
            hasSensors = true;
            Float value = data.get(sensorName);
            if (value != null) {
                updateSensor(sensorName, value);
            }
        }
        
        if (!hasSensors) {
            System.err.println("WARNING: No sensors parsed from message: " + message);
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String analyzeAndSuggest() {
        if (!enabled) {
            return null;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
            return null;
        }
        lastAnalysisTime = now;
        
        Float distance = getSensorValue("ultrasonic");
        Float touch = getSensorValue("touch");
        Float gyro = getSensorValue("gyro");
        Float light = getSensorValue("light");
        
        if (touch != null && touch >= TOUCH_PRESSED) {
            return "STOP";
        }
        
        if (distance != null && distance < 10.0f) {
            return "STOP";
        }
        
        if (gyro != null && Math.abs(gyro) > DANGEROUS_TILT) {
            return "STOP";
        }
        
        if (distance != null && distance < OBSTACLE_DISTANCE_CM) {
            if (System.currentTimeMillis() % 2 == 0) {
                return "LEFT 200 500";
            } else {
                return "RIGHT 200 500";
            }
        }
        
        if (distance != null && distance > SAFE_DISTANCE_CM) {
            return null;
        }
        
        return null;
    }
    
    public Float getSensorValue(String sensorName) {
        SensorReading reading = sensorData.get(sensorName);
        if (reading == null) {
            return null;
        }
        
        // Check if reading is stale (older than 5 seconds)
        if (System.currentTimeMillis() - reading.timestamp > 5000) {
            return null;
        }
        
        return reading.value;
    }
    
    /**
     * Get a summary of all sensor readings.
     */
    public String getSensorSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Server Sensor Analysis ===\n");
        
        for (Map.Entry<String, SensorReading> entry : sensorData.entrySet()) {
            SensorReading reading = entry.getValue();
            long age = System.currentTimeMillis() - reading.timestamp;
            sb.append(String.format("%s: %.2f (age: %dms)\n", 
                                  reading.name, reading.value, age));
        }
        
        // Add analysis
        Float distance = getSensorValue("ultrasonic");
        if (distance != null) {
            if (distance < OBSTACLE_DISTANCE_CM) {
                sb.append("⚠ OBSTACLE DETECTED at ").append(distance).append("cm\n");
            } else if (distance > SAFE_DISTANCE_CM) {
                sb.append("✓ Path clear (").append(distance).append("cm)\n");
            }
        }
        
        Float touch = getSensorValue("touch");
        if (touch != null && touch >= TOUCH_PRESSED) {
            sb.append("⚠ TOUCH SENSOR ACTIVATED\n");
        }
        
        Float gyro = getSensorValue("gyro");
        if (gyro != null) {
            if (Math.abs(gyro) > DANGEROUS_TILT) {
                sb.append("⚠ DANGEROUS TILT: ").append(gyro).append(" degrees\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Get current threat level based on sensor data.
     */
    public ThreatLevel getThreatLevel() {
        Float distance = getSensorValue("ultrasonic");
        Float touch = getSensorValue("touch");
        Float gyro = getSensorValue("gyro");
        
        // Critical threats
        if (touch != null && touch >= TOUCH_PRESSED) {
            return ThreatLevel.CRITICAL;
        }
        
        if (distance != null && distance < 10.0f) {
            return ThreatLevel.CRITICAL;
        }
        
        if (gyro != null && Math.abs(gyro) > DANGEROUS_TILT) {
            return ThreatLevel.CRITICAL;
        }
        
        // High threats
        if (distance != null && distance < OBSTACLE_DISTANCE_CM) {
            return ThreatLevel.HIGH;
        }
        
        // All clear
        return ThreatLevel.NONE;
    }
    
    /**
     * Clear all sensor data (e.g., on disconnect).
     */
    public void clear() {
        sensorData.clear();
    }
    
    /**
     * Threat level for GUI display.
     */
    public enum ThreatLevel {
        NONE,     // All clear
        HIGH,     // Obstacle nearby
        CRITICAL  // Immediate danger
    }
    
    /**
     * Internal class to store sensor reading with timestamp.
     */
    private static class SensorReading {
        final String name;
        final float value;
        final long timestamp;
        
        SensorReading(String name, float value, long timestamp) {
            this.name = name;
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
