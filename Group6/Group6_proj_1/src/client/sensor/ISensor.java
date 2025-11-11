package client.sensor;

/**
 * Interface for all EV3 sensors with proper resource management.
 * Implements AutoCloseable to enable try-with-resources pattern.
 */
public interface ISensor extends AutoCloseable {
    /**
     * Get the sensor name identifier.
     * @return sensor name (e.g., "ultrasonic", "touch", "light", "gyro")
     */
    String getName();
    
    /**
     * Read current sensor value.
     * @return formatted sensor data string, or null if not available or sensor error
     */
    String readValue();
    
    /**
     * Check if sensor is connected and functioning.
     * @return true if sensor is available, false if disconnected or error
     */
    boolean isAvailable();
    
    /**
     * Close sensor and release resources.
     * Safe to call multiple times.
     */
    @Override
    void close();
}