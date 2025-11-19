package common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents sensor data with multiple sensor readings.
 * Provides type-safe access to sensor values.
 */
public class SensorData {
    
    private final Map<String, Float> readings;
    
    public SensorData() {
        this.readings = new LinkedHashMap<>();
    }
    
    public SensorData(Map<String, Float> readings) {
        this.readings = new LinkedHashMap<>(readings);
    }
    
    /**
     * Add or update a sensor reading.
     * @param sensorName Sensor name (e.g., "ultrasonic", "infrared")
     * @param value Sensor value
     */
    public void put(String sensorName, float value) {
        readings.put(sensorName, value);
    }
    
    /**
     * Get sensor value by name.
     * @param sensorName Sensor name
     * @return Sensor value or null if not found
     */
    public Float get(String sensorName) {
        return readings.get(sensorName);
    }
    
    /**
     * Get sensor value with default.
     * @param sensorName Sensor name
     * @param defaultValue Value to return if sensor not found
     * @return Sensor value or defaultValue
     */
    public float get(String sensorName, float defaultValue) {
        Float value = readings.get(sensorName);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Check if sensor reading exists.
     * @param sensorName Sensor name
     * @return true if sensor data exists
     */
    public boolean has(String sensorName) {
        return readings.containsKey(sensorName);
    }
    
    /**
     * Get all sensor names.
     */
    public Iterable<String> getSensorNames() {
        return readings.keySet();
    }
    
    /**
     * Get all readings as map.
     */
    public Map<String, Float> getAll() {
        return new LinkedHashMap<>(readings);
    }
    
    /**
     * Get number of sensor readings.
     */
    public int size() {
        return readings.size();
    }
    
    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return readings.isEmpty();
    }
    
    /**
     * Clear all readings.
     */
    public void clear() {
        readings.clear();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Float> entry : readings.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }
}
