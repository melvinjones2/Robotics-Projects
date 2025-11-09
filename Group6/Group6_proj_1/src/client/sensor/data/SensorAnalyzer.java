package client.sensor.data;

import client.sensor.data.SensorDataStore.SensorStats;

/**
 * Analyzes sensor data to make decisions about robot behavior.
 * Uses stored historical data to determine appropriate actions.
 */
public class SensorAnalyzer {
    private final SensorDataStore dataStore;
    
    // Thresholds for decision making
    private static final float OBSTACLE_DISTANCE_CM = 20.0f;
    private static final float TOUCH_PRESSED = 1.0f;
    private static final float LIGHT_THRESHOLD_LOW = 0.2f;
    private static final float LIGHT_THRESHOLD_HIGH = 0.8f;
    private static final int ANALYSIS_WINDOW = 5; // Last 5 readings
    
    public SensorAnalyzer(SensorDataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    /**
     * Check if there's an obstacle ahead.
     */
    public boolean hasObstacleAhead() {
        Float distance = dataStore.getLatest("ultrasonic");
        if (distance == null) return false;
        
        // Check if consistently detecting close obstacle
        float avgDistance = dataStore.getAverage("ultrasonic", ANALYSIS_WINDOW);
        return avgDistance < OBSTACLE_DISTANCE_CM;
    }
    
    /**
     * Get obstacle distance with confidence.
     */
    public ObstacleInfo getObstacleInfo() {
        SensorStats stats = dataStore.getStats("ultrasonic", ANALYSIS_WINDOW);
        if (stats == null || stats.sampleCount < 2) {
            return new ObstacleInfo(false, Float.MAX_VALUE, 0);
        }
        
        boolean hasObstacle = stats.average < OBSTACLE_DISTANCE_CM;
        float variance = stats.max - stats.min;
        int confidence = calculateConfidence(stats.sampleCount, variance);
        
        return new ObstacleInfo(hasObstacle, stats.average, confidence);
    }
    
    /**
     * Check if touch sensor is pressed.
     */
    public boolean isTouched() {
        Float value = dataStore.getLatest("touch");
        return value != null && value >= TOUCH_PRESSED;
    }
    
    /**
     * Check if touch was just pressed (transition).
     */
    public boolean wasTouched() {
        if (dataStore.getRecent("touch", 2).size() < 2) {
            return false;
        }
        return dataStore.hasChangedBy("touch", 0.5f) && isTouched();
    }
    
    /**
     * Get light level category.
     */
    public LightLevel getLightLevel() {
        Float light = dataStore.getLatest("light");
        if (light == null) return LightLevel.UNKNOWN;
        
        float avgLight = dataStore.getAverage("light", ANALYSIS_WINDOW);
        
        if (avgLight < LIGHT_THRESHOLD_LOW) {
            return LightLevel.DARK;
        } else if (avgLight > LIGHT_THRESHOLD_HIGH) {
            return LightLevel.BRIGHT;
        } else {
            return LightLevel.NORMAL;
        }
    }
    
    /**
     * Check if robot is tilting (using gyro).
     */
    public boolean isTilting() {
        Float gyro = dataStore.getLatest("gyro");
        if (gyro == null) return false;
        
        return Math.abs(gyro) > 15.0f; // More than 15 degrees
    }
    
    /**
     * Get rotation trend (spinning left/right).
     */
    public int getRotationTrend() {
        return dataStore.getTrend("gyro", ANALYSIS_WINDOW);
    }
    
    /**
     * Analyze if robot should stop (emergency conditions).
     */
    public boolean shouldEmergencyStop() {
        // Stop if touched
        if (isTouched()) {
            return true;
        }
        
        // Stop if very close obstacle
        Float distance = dataStore.getLatest("ultrasonic");
        if (distance != null && distance < 10.0f) {
            return true;
        }
        
        // Stop if severely tilted
        Float gyro = dataStore.getLatest("gyro");
        if (gyro != null && Math.abs(gyro) > 45.0f) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Suggest direction to avoid obstacle.
     */
    public NavigationSuggestion suggestNavigation() {
        ObstacleInfo obstacle = getObstacleInfo();
        
        if (!obstacle.detected) {
            return new NavigationSuggestion(Action.FORWARD, "Clear path");
        }
        
        if (obstacle.distance < 10.0f) {
            return new NavigationSuggestion(Action.BACKWARD, "Too close, back up");
        }
        
        if (obstacle.distance < OBSTACLE_DISTANCE_CM) {
            // Simple heuristic: turn right by default
            // In real implementation, could use multiple sensors
            return new NavigationSuggestion(Action.TURN_RIGHT, "Obstacle ahead, turn right");
        }
        
        return new NavigationSuggestion(Action.FORWARD, "Path clear");
    }
    
    /**
     * Get comprehensive sensor summary.
     */
    public String getSensorSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Sensor Analysis ===\n");
        
        for (String sensorName : dataStore.getSensorNames()) {
            SensorStats stats = dataStore.getStats(sensorName, 10);
            if (stats != null) {
                sb.append(stats.toString()).append("\n");
            }
        }
        
        sb.append("\nDecisions:\n");
        sb.append("- Obstacle: ").append(hasObstacleAhead() ? "YES" : "NO").append("\n");
        sb.append("- Touched: ").append(isTouched() ? "YES" : "NO").append("\n");
        sb.append("- Light: ").append(getLightLevel()).append("\n");
        sb.append("- Emergency Stop: ").append(shouldEmergencyStop() ? "YES" : "NO").append("\n");
        
        NavigationSuggestion nav = suggestNavigation();
        sb.append("- Suggestion: ").append(nav.action).append(" - ").append(nav.reason);
        
        return sb.toString();
    }
    
    private int calculateConfidence(int sampleCount, float variance) {
        // More samples and lower variance = higher confidence
        int confidence = Math.min(100, (sampleCount * 20)); // Max 100%
        if (variance > 5.0f) {
            confidence -= 20; // Reduce for high variance
        }
        return Math.max(0, confidence);
    }
    
    // Helper classes
    public static class ObstacleInfo {
        public final boolean detected;
        public final float distance;
        public final int confidence; // 0-100%
        
        public ObstacleInfo(boolean detected, float distance, int confidence) {
            this.detected = detected;
            this.distance = distance;
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return String.format("Obstacle: %s, Distance: %.1fcm, Confidence: %d%%",
                detected ? "YES" : "NO", distance, confidence);
        }
    }
    
    public enum Action {
        FORWARD, BACKWARD, TURN_LEFT, TURN_RIGHT, STOP
    }
    
    public static class NavigationSuggestion {
        public final Action action;
        public final String reason;
        
        public NavigationSuggestion(Action action, String reason) {
            this.action = action;
            this.reason = reason;
        }
        
        @Override
        public String toString() {
            return action + ": " + reason;
        }
    }
    
    public enum LightLevel {
        DARK, NORMAL, BRIGHT, UNKNOWN
    }
}
