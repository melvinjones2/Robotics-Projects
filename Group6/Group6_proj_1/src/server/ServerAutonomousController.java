package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side autonomous controller that analyzes sensor data from the client
 * and can issue commands with SERVER priority to override client-side autonomous mode.
 * 
 * This allows the human operator (server) to make higher-level decisions based on
 * all available sensor data while still respecting Asimov's Three Laws.
 */
public class ServerAutonomousController {
    
    // Store latest sensor readings from client
    private final Map<String, SensorReading> sensorData = new ConcurrentHashMap<>();
    
    // Thresholds for server-side decision making
    private static final float OBSTACLE_DISTANCE_CM = 25.0f;
    private static final float SAFE_DISTANCE_CM = 50.0f;
    private static final float TOUCH_PRESSED = 1.0f;
    private static final float DANGEROUS_TILT = 25.0f;
    
    private volatile boolean enabled = false;
    private volatile long lastAnalysisTime = 0;
    private static final long ANALYSIS_INTERVAL_MS = 1000; // Analyze every 1 second
    
    /**
     * Update sensor reading from client.
     */
    public void updateSensor(String sensorName, float value) {
        sensorData.put(sensorName, new SensorReading(sensorName, value, System.currentTimeMillis()));
    }
    
    /**
     * Parse sensor data message from client.
     * Expected format: "SENSOR:ultrasonic=45.2,touch=0.0,light=35.0"
     */
    public void parseSensorMessage(String message) {
        if (message == null || !message.startsWith("SENSOR:")) {
            return;
        }
        
        String data = message.substring(7); // Remove "SENSOR:" prefix
        String[] sensors = data.split(",");
        
        for (String sensor : sensors) {
            String[] parts = sensor.split("=");
            if (parts.length == 2) {
                try {
                    String name = parts[0].trim();
                    float value = Float.parseFloat(parts[1].trim());
                    updateSensor(name, value);
                } catch (NumberFormatException e) {
                    // Ignore invalid sensor data
                }
            }
        }
    }
    
    /**
     * Enable or disable server-side autonomous control.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Analyze current sensor data and return a command suggestion.
     * Returns null if no action needed or if not enough time has passed.
     */
    public String analyzeAndSuggest() {
        if (!enabled) {
            return null;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
            return null;
        }
        lastAnalysisTime = now;
        
        // Get sensor readings
        Float distance = getSensorValue("ultrasonic");
        Float touch = getSensorValue("touch");
        Float gyro = getSensorValue("gyro");
        Float light = getSensorValue("light");
        
        // Check for emergency conditions (highest priority)
        if (touch != null && touch >= TOUCH_PRESSED) {
            return "STOP"; // Touch sensor activated
        }
        
        if (distance != null && distance < 10.0f) {
            return "STOP"; // Very close obstacle
        }
        
        if (gyro != null && Math.abs(gyro) > DANGEROUS_TILT) {
            return "STOP"; // Dangerous tilt
        }
        
        // Check for navigation needs
        if (distance != null && distance < OBSTACLE_DISTANCE_CM) {
            // Obstacle detected, suggest turning
            // Alternate between left and right for variety
            if (System.currentTimeMillis() % 2 == 0) {
                return "LEFT 200 500";
            } else {
                return "RIGHT 200 500";
            }
        }
        
        // All clear - could suggest forward movement if desired
        if (distance != null && distance > SAFE_DISTANCE_CM) {
            // Path is clear, safe to move forward
            // But don't auto-suggest movement unless explicitly in exploration mode
            return null;
        }
        
        return null; // No suggestion
    }
    
    /**
     * Get the latest value for a sensor.
     */
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
