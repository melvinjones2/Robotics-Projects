package client.sensor.impl;

import client.sensor.ISensor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class UltrasonicSensor implements ISensor {
    private EV3UltrasonicSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;
    private boolean closed = false;
    
    private static final int FILTER_SIZE = 5;
    private float[] filterBuffer = new float[FILTER_SIZE];
    private int filterIndex = 0;
    private int filterCount = 0;

    public UltrasonicSensor() {
        this(SensorPort.S1, "distance");
    }

    public UltrasonicSensor(Port port) {
        this(port, "distance");
    }

    public UltrasonicSensor(Port port, String mode) {
        sensor = new EV3UltrasonicSensor(port);
        this.mode = mode.toLowerCase();
        if ("listen".equals(this.mode)) {
            provider = sensor.getListenMode();
        } else {
            provider = sensor.getDistanceMode();
            this.mode = "distance";
        }
        sample = new float[provider.sampleSize()];
    }

    @Override
    public String getName() {
        return "ultrasonic";
    }

    @Override
    public String readValue() {
        if (closed || sensor == null) {
            return null;
        }
        
        try {
            provider.fetchSample(sample, 0);
            
            if ("distance".equals(mode)) {
                float distanceCm = sample[0] * 100.0f;
                
                if (distanceCm > 255.0f || Float.isNaN(distanceCm)) {
                    distanceCm = 255.0f;
                }
                
                float filteredDist = applyMedianFilter(distanceCm);
                
                return "ultrasonic=" + String.format("%.1f", filteredDist);
            } else {
                return "ultrasonic=" + (sample[0] > 0.5f ? "1" : "0");
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    private float applyMedianFilter(float newValue) {
        filterBuffer[filterIndex] = newValue;
        filterIndex = (filterIndex + 1) % FILTER_SIZE;
        if (filterCount < FILTER_SIZE) {
            filterCount++;
        }
        
        if (filterCount < 3) {
            return newValue;
        }
        
        float[] sorted = new float[filterCount];
        System.arraycopy(filterBuffer, 0, sorted, 0, filterCount);
        
        for (int i = 0; i < filterCount - 1; i++) {
            for (int j = 0; j < filterCount - i - 1; j++) {
                if (sorted[j] > sorted[j + 1]) {
                    float temp = sorted[j];
                    sorted[j] = sorted[j + 1];
                    sorted[j + 1] = temp;
                }
            }
        }
        
        return sorted[filterCount / 2];
    }

    @Override
    public boolean isAvailable() {
        if (closed || sensor == null) {
            return false;
        }
        
        try {
            // Try to fetch a sample - if it works, sensor is available
            provider.fetchSample(sample, 0);
            return !Float.isNaN(sample[0]);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (!closed && sensor != null) {
            try {
                sensor.close();
            } catch (Exception e) {
                // Ignore errors during close
            }
            closed = true;
        }
    }
}