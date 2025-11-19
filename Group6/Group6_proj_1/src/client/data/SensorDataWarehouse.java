package client.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe centralized warehouse for sensor data.
 * 
 * Design Pattern: Repository Pattern
 * - Single source of truth for all sensor readings
 * - Thread-safe concurrent read/write operations
 * - Historical data storage with configurable buffer size
 * - Used by multiple threads: SensorThread (writer), autonomous tasks (readers)
 * 
 * Thread Safety:
 * - ConcurrentHashMap for sensor-to-buffer mapping
 * - CopyOnWriteArrayList for historical readings per sensor
 * - No explicit locks needed - relies on concurrent collections
 */
public class SensorDataWarehouse {
    
    private static final int DEFAULT_HISTORY_SIZE = 100;
    
    // Sensor name -> historical readings buffer
    // Using regular ArrayList with synchronization instead of CopyOnWriteArrayList
    // because our workload is write-heavy (constant sensor updates)
    private final Map<String, java.util.ArrayList<SensorReading>> sensorData;
    
    // Maximum readings to store per sensor
    private final int maxHistorySize;
    
    /**
     * Creates warehouse with default history size (100 readings per sensor).
     */
    public SensorDataWarehouse() {
        this(DEFAULT_HISTORY_SIZE);
    }
    
    /**
     * Creates warehouse with custom history size.
     * 
     * @param maxHistorySize maximum readings to store per sensor
     */
    public SensorDataWarehouse(int maxHistorySize) {
        this.sensorData = new ConcurrentHashMap<String, java.util.ArrayList<SensorReading>>();
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Stores a new sensor reading.
     * Thread-safe - can be called concurrently by SensorThread.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor (e.g., "ultrasonic", "gyro")
     * @param value sensor reading value
     * @param timestamp reading timestamp in milliseconds
     */
    public void store(String sensorName, float value, long timestamp) {
        if (sensorName == null) return;
        
        try {
            java.util.ArrayList<SensorReading> buffer = sensorData.get(sensorName);
            if (buffer == null) {
                // Java 7 compatible: manually check and create
                synchronized (sensorData) {
                    buffer = sensorData.get(sensorName);
                    if (buffer == null) {
                        buffer = new java.util.ArrayList<SensorReading>(maxHistorySize + 10);
                        sensorData.put(sensorName, buffer);
                    }
                }
            }
            
            // Synchronize writes to the buffer
            synchronized (buffer) {
                // Add new reading
                SensorReading reading = new SensorReading(value, timestamp);
                buffer.add(reading);
                
                // Trim old readings if buffer exceeds max size
                if (buffer.size() > maxHistorySize) {
                    buffer.remove(0);  // Remove oldest
                }
            }
        } catch (Exception e) {
            // Gracefully handle errors during shutdown (silently fail)
        }
    }
    
    /**
     * Convenience method - stores reading with current timestamp.
     * 
     * @param sensorName name of sensor
     * @param value sensor reading value
     */
    public void store(String sensorName, float value) {
        store(sensorName, value, System.currentTimeMillis());
    }
    
    /**
     * Gets the most recent reading for a sensor.
     * Thread-safe - can be called concurrently by autonomous tasks.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor
     * @return latest reading, or null if no data available or error occurs
     */
    public SensorReading getLatest(String sensorName) {
        try {
            java.util.ArrayList<SensorReading> buffer = sensorData.get(sensorName);
            if (buffer == null) {
                return null;
            }
            
            // Synchronize reads from the buffer
            synchronized (buffer) {
                if (buffer.isEmpty()) {
                    return null;
                }
                return buffer.get(buffer.size() - 1);
            }
        } catch (Exception e) {
            // Gracefully handle any errors during shutdown
            return null;
        }
    }
    
    /**
     * Gets the latest N readings for a sensor.
     * Thread-safe - returns a snapshot of current data.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor
     * @param count number of recent readings to retrieve
     * @return list of readings (most recent last), empty if no data or error occurs
     */
    public List<SensorReading> getLatestN(String sensorName, int count) {
        try {
            java.util.ArrayList<SensorReading> buffer = sensorData.get(sensorName);
            if (buffer == null) {
                return new java.util.ArrayList<SensorReading>();
            }
            
            synchronized (buffer) {
                if (buffer.isEmpty()) {
                    return new java.util.ArrayList<SensorReading>();
                }
                int size = buffer.size();
                int start = Math.max(0, size - count);
                return new java.util.ArrayList<SensorReading>(buffer.subList(start, size));
            }
        } catch (Exception e) {
            // Gracefully handle any errors during shutdown
            return new java.util.ArrayList<SensorReading>();
        }
    }
    
    /**
     * Gets all readings for a sensor.
     * Thread-safe - returns a snapshot of current data.
     * 
     * @param sensorName name of sensor
     * @return list of all readings, empty if no data
     */
    public List<SensorReading> getAll(String sensorName) {
        try {
            java.util.ArrayList<SensorReading> buffer = sensorData.get(sensorName);
            if (buffer == null) {
                return new java.util.ArrayList<SensorReading>();
            }
            synchronized (buffer) {
                return new java.util.ArrayList<SensorReading>(buffer);
            }
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return new java.util.ArrayList<SensorReading>();
        }
    }
    
    /**
     * Calculates average of recent N readings.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor
     * @param count number of recent readings to average
     * @return average value, or Float.NaN if insufficient data
     */
    public float getAverage(String sensorName, int count) {
        try {
            List<SensorReading> readings = getLatestN(sensorName, count);
            if (readings.isEmpty()) {
                return Float.NaN;
            }
            
            float sum = 0;
            for (SensorReading r : readings) {
                sum += r.value;
            }
            return sum / readings.size();
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return Float.NaN;
        }
    }
    
    /**
     * Gets minimum value from recent N readings.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor
     * @param count number of recent readings to check
     * @return minimum value, or Float.NaN if insufficient data
     */
    public float getMin(String sensorName, int count) {
        try {
            List<SensorReading> readings = getLatestN(sensorName, count);
            if (readings.isEmpty()) {
                return Float.NaN;
            }
            
            float min = Float.POSITIVE_INFINITY;
            for (SensorReading r : readings) {
                min = Math.min(min, r.value);
            }
            return min;
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return Float.NaN;
        }
    }
    
    /**
     * Gets maximum value from recent N readings.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor
     * @param count number of recent readings to check
     * @return maximum value, or Float.NaN if insufficient data
     */
    public float getMax(String sensorName, int count) {
        try {
            List<SensorReading> readings = getLatestN(sensorName, count);
            if (readings.isEmpty()) {
                return Float.NaN;
            }
            
            float max = Float.NEGATIVE_INFINITY;
            for (SensorReading r : readings) {
                max = Math.max(max, r.value);
            }
            return max;
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return Float.NaN;
        }
    }
    
    /**
     * Checks if warehouse has data for a sensor.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor
     * @return true if sensor has at least one reading
     */
    public boolean hasSensor(String sensorName) {
        try {
            List<SensorReading> buffer = sensorData.get(sensorName);
            return buffer != null && !buffer.isEmpty();
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return false;
        }
    }
    
    /**
     * Gets list of all sensor names with data.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @return list of sensor names
     */
    public List<String> getSensorNames() {
        try {
            return new java.util.ArrayList<String>(sensorData.keySet());
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return new java.util.ArrayList<String>();
        }
    }
    
    /**
     * Clears all data for a specific sensor.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @param sensorName name of sensor to clear
     */
    public void clearSensor(String sensorName) {
        try {
            sensorData.remove(sensorName);
        } catch (Exception e) {
            // Gracefully handle errors during shutdown (silently fail)
        }
    }
    
    /**
     * Clears all sensor data.
     * Gracefully handles errors to prevent crashes during shutdown.
     */
    public void clearAll() {
        try {
            sensorData.clear();
        } catch (Exception e) {
            // Gracefully handle errors during shutdown (silently fail)
        }
    }
    
    /**
     * Gets total number of readings stored across all sensors.
     * Gracefully handles errors to prevent crashes during shutdown.
     * 
     * @return total reading count
     */
    public int getTotalReadingCount() {
        try {
            int count = 0;
            for (List<SensorReading> buffer : sensorData.values()) {
                count += buffer.size();
            }
            return count;
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return 0;
        }
    }
    
    /**
     * Immutable sensor reading with value and timestamp.
     */
    public static class SensorReading {
        public final float value;
        public final long timestamp;
        
        public SensorReading(float value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        /**
         * Gets age of this reading in milliseconds.
         */
        public long getAge() {
            return System.currentTimeMillis() - timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("%.2f @ %d", value, timestamp);
        }
    }
}
