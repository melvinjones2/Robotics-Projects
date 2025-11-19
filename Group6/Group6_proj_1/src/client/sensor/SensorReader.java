package client.sensor;

/**
 * Utility class for reading and parsing sensor values.
 * Handles common sensor reading patterns and error cases.
 */
public class SensorReader {
    
    /**
     * Read distance value from a sensor in centimeters.
     * Handles parsing of "sensorName=value[,...]" format commonly used by distance sensors.
     * 
     * @param sensor The sensor to read from
     * @return Distance in cm, or -1 if unavailable/error
     */
    public static float readDistance(ISensor sensor) {
        if (sensor == null || !sensor.isAvailable()) {
            return -1.0f;
        }
        
        try {
            String value = sensor.readValue();
            if (value != null && value.contains("=")) {
                String number = value.substring(value.indexOf('=') + 1).trim();
                // Handle multi-value sensors (e.g., "distance=25.3,angle=45")
                if (number.contains(",")) {
                    number = number.split(",")[0];
                }
                float parsed = Float.parseFloat(number);
                // Validate: must be positive and finite
                return (parsed > 0 && !Float.isInfinite(parsed) && !Float.isNaN(parsed)) ? parsed : -1.0f;
            }
        } catch (Exception e) {
            // Parse error or sensor read error - return invalid
        }
        
        return -1.0f;
    }
    
    /**
     * Read a generic float value from a sensor.
     * Handles parsing of "sensorName=value" format.
     * Unlike readDistance, this allows negative values.
     * 
     * @param sensor The sensor to read from
     * @return Parsed float value, or Float.NaN if unavailable/error
     */
    public static float readFloat(ISensor sensor) {
        if (sensor == null || !sensor.isAvailable()) {
            return Float.NaN;
        }
        
        try {
            String value = sensor.readValue();
            if (value != null && value.contains("=")) {
                String number = value.substring(value.indexOf('=') + 1).trim();
                if (number.contains(",")) {
                    number = number.split(",")[0];
                }
                float parsed = Float.parseFloat(number);
                return Float.isFinite(parsed) ? parsed : Float.NaN;
            }
        } catch (Exception e) {
            // Parse error
        }
        
        return Float.NaN;
    }
    
    /**
     * Read raw sensor value string without parsing.
     * 
     * @param sensor The sensor to read from
     * @return Raw sensor value string, or null if unavailable/error
     */
    public static String readRaw(ISensor sensor) {
        if (sensor == null || !sensor.isAvailable()) {
            return null;
        }
        
        try {
            return sensor.readValue();
        } catch (Exception e) {
            return null;
        }
    }
    
    // Prevent instantiation
    private SensorReader() {
        throw new AssertionError("SensorReader is a utility class and cannot be instantiated");
    }
}
