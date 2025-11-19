package client.sensor;

/**
 * Interface for all EV3 sensors with proper resource management.
 * Implements AutoCloseable to enable try-with-resources pattern.
 */
public interface ISensor extends AutoCloseable {
    /**
     * Get the sensor name identifier.
     */
    String getName();
    
    /**
     * Read current sensor value.
     */
    String readValue();
    
    /**
     * Check if sensor is connected and functioning.
     */
    boolean isAvailable();
    
    /**
     * Close sensor and release resources.
     * Safe to call multiple times.
     */
    @Override
    void close();
}