package client.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Thread-safe centralized repository for sensor data with history
public class SensorDataWarehouse {
    
    private static final int DEFAULT_HISTORY_SIZE = 100;
    
    // Sensor name -> historical readings buffer
    // Using regular ArrayList with synchronization instead of CopyOnWriteArrayList
    // because our workload is write-heavy (constant sensor updates)
    private final Map<String, java.util.ArrayList<SensorReading>> sensorData;
    
    // Maximum readings to store per sensor
    private final int maxHistorySize;
    
    public SensorDataWarehouse() {
        this(DEFAULT_HISTORY_SIZE);
    }
    
    public SensorDataWarehouse(int maxHistorySize) {
        this.sensorData = new ConcurrentHashMap<String, java.util.ArrayList<SensorReading>>();
        this.maxHistorySize = maxHistorySize;
    }
    
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
     */
    public void store(String sensorName, float value) {
        store(sensorName, value, System.currentTimeMillis());
    }
    
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
    
    public boolean hasSensor(String sensorName) {
        try {
            List<SensorReading> buffer = sensorData.get(sensorName);
            return buffer != null && !buffer.isEmpty();
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return false;
        }
    }
    
    public List<String> getSensorNames() {
        try {
            return new java.util.ArrayList<String>(sensorData.keySet());
        } catch (Exception e) {
            // Gracefully handle errors during shutdown
            return new java.util.ArrayList<String>();
        }
    }
    
    public void clearSensor(String sensorName) {
        try {
            sensorData.remove(sensorName);
        } catch (Exception e) {
            // Gracefully handle errors during shutdown (silently fail)
        }
    }
    
    public void clearAll() {
        try {
            sensorData.clear();
        } catch (Exception e) {
            // Gracefully handle errors during shutdown (silently fail)
        }
    }
    
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
    
    public static class SensorReading {
        public final float value;
        public final long timestamp;
        
        public SensorReading(float value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        public long getAge() {
            return System.currentTimeMillis() - timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("%.2f @ %d", value, timestamp);
        }
    }
}
