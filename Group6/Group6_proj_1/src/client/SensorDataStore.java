package client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores historical sensor data with timestamp tracking.
 * Allows querying recent values, averages, and trends.
 */
public class SensorDataStore {
    private static final int MAX_HISTORY_SIZE = 100; // Keep last 100 readings per sensor
    
    private final Map<String, SensorHistory> sensorData = new HashMap<>();
    private final Object lock = new Object();
    
    /**
     * Store a sensor reading.
     */
    public void store(String sensorName, float value) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            if (history == null) {
                history = new SensorHistory(sensorName);
                sensorData.put(sensorName, history);
            }
            history.addReading(value);
        }
    }
    
    /**
     * Get the most recent sensor value.
     */
    public Float getLatest(String sensorName) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            return history != null ? history.getLatest() : null;
        }
    }
    
    /**
     * Get the last N readings for a sensor.
     */
    public List<SensorReading> getRecent(String sensorName, int count) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            return history != null ? history.getRecent(count) : Collections.<SensorReading>emptyList();
        }
    }
    
    /**
     * Get average value over last N readings.
     */
    public float getAverage(String sensorName, int count) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            return history != null ? history.getAverage(count) : 0f;
        }
    }
    
    /**
     * Get min value over last N readings.
     */
    public float getMin(String sensorName, int count) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            return history != null ? history.getMin(count) : 0f;
        }
    }
    
    /**
     * Get max value over last N readings.
     */
    public float getMax(String sensorName, int count) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            return history != null ? history.getMax(count) : 0f;
        }
    }
    
    /**
     * Check if sensor value has changed significantly.
     */
    public boolean hasChangedBy(String sensorName, float threshold) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            if (history == null || history.size() < 2) {
                return false;
            }
            Float latest = history.getLatest();
            Float previous = history.getPrevious();
            return latest != null && previous != null && 
                   Math.abs(latest - previous) >= threshold;
        }
    }
    
    /**
     * Get trend direction (1 = increasing, -1 = decreasing, 0 = stable).
     */
    public int getTrend(String sensorName, int windowSize) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            if (history == null || history.size() < windowSize) {
                return 0;
            }
            return history.getTrend(windowSize);
        }
    }
    
    /**
     * Get all available sensor names.
     */
    public List<String> getSensorNames() {
        synchronized (lock) {
            return new ArrayList<String>(sensorData.keySet());
        }
    }
    
    /**
     * Get summary statistics for a sensor.
     */
    public SensorStats getStats(String sensorName, int windowSize) {
        synchronized (lock) {
            SensorHistory history = sensorData.get(sensorName);
            if (history == null) {
                return null;
            }
            return history.getStats(windowSize);
        }
    }
    
    /**
     * Clear all stored data.
     */
    public void clear() {
        synchronized (lock) {
            sensorData.clear();
        }
    }
    
    /**
     * Clear data for a specific sensor.
     */
    public void clear(String sensorName) {
        synchronized (lock) {
            sensorData.remove(sensorName);
        }
    }
    
    /**
     * Historical data for a single sensor.
     */
    private static class SensorHistory {
        private final String name;
        private final List<SensorReading> readings = new ArrayList<>();
        
        public SensorHistory(String name) {
            this.name = name;
        }
        
        public void addReading(float value) {
            readings.add(new SensorReading(System.currentTimeMillis(), value));
            
            // Trim old readings
            if (readings.size() > MAX_HISTORY_SIZE) {
                readings.remove(0);
            }
        }
        
        public Float getLatest() {
            return readings.isEmpty() ? null : readings.get(readings.size() - 1).value;
        }
        
        public Float getPrevious() {
            return readings.size() < 2 ? null : readings.get(readings.size() - 2).value;
        }
        
        public int size() {
            return readings.size();
        }
        
        public List<SensorReading> getRecent(int count) {
            int start = Math.max(0, readings.size() - count);
            return new ArrayList<>(readings.subList(start, readings.size()));
        }
        
        public float getAverage(int count) {
            List<SensorReading> recent = getRecent(count);
            if (recent.isEmpty()) return 0f;
            
            float sum = 0;
            for (SensorReading r : recent) {
                sum += r.value;
            }
            return sum / recent.size();
        }
        
        public float getMin(int count) {
            List<SensorReading> recent = getRecent(count);
            if (recent.isEmpty()) return 0f;
            
            float min = Float.MAX_VALUE;
            for (SensorReading r : recent) {
                min = Math.min(min, r.value);
            }
            return min;
        }
        
        public float getMax(int count) {
            List<SensorReading> recent = getRecent(count);
            if (recent.isEmpty()) return 0f;
            
            float max = Float.MIN_VALUE;
            for (SensorReading r : recent) {
                max = Math.max(max, r.value);
            }
            return max;
        }
        
        public int getTrend(int windowSize) {
            List<SensorReading> recent = getRecent(windowSize);
            if (recent.size() < 2) return 0;
            
            float first = recent.get(0).value;
            float last = recent.get(recent.size() - 1).value;
            float diff = last - first;
            
            if (Math.abs(diff) < 0.1f) return 0; // Stable
            return diff > 0 ? 1 : -1; // Increasing or decreasing
        }
        
        public SensorStats getStats(int windowSize) {
            List<SensorReading> recent = getRecent(windowSize);
            if (recent.isEmpty()) {
                return new SensorStats(name, 0, 0, 0, 0, 0, 0);
            }
            
            float latest = recent.get(recent.size() - 1).value;
            float avg = getAverage(windowSize);
            float min = getMin(windowSize);
            float max = getMax(windowSize);
            int trend = getTrend(windowSize);
            
            return new SensorStats(name, latest, avg, min, max, trend, recent.size());
        }
    }
    
    /**
     * Single sensor reading with timestamp.
     */
    public static class SensorReading {
        public final long timestamp;
        public final float value;
        
        public SensorReading(long timestamp, float value) {
            this.timestamp = timestamp;
            this.value = value;
        }
        
        public long getAge() {
            return System.currentTimeMillis() - timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("%.2f @ %dms", value, timestamp);
        }
    }
    
    /**
     * Statistical summary for a sensor.
     */
    public static class SensorStats {
        public final String sensorName;
        public final float latest;
        public final float average;
        public final float min;
        public final float max;
        public final int trend; // 1=increasing, 0=stable, -1=decreasing
        public final int sampleCount;
        
        public SensorStats(String sensorName, float latest, float average, 
                          float min, float max, int trend, int sampleCount) {
            this.sensorName = sensorName;
            this.latest = latest;
            this.average = average;
            this.min = min;
            this.max = max;
            this.trend = trend;
            this.sampleCount = sampleCount;
        }
        
        @Override
        public String toString() {
            String trendStr = trend > 0 ? "↑" : (trend < 0 ? "↓" : "→");
            return String.format("%s: %.2f (avg:%.2f min:%.2f max:%.2f %s n=%d)",
                sensorName, latest, average, min, max, trendStr, sampleCount);
        }
    }
}
