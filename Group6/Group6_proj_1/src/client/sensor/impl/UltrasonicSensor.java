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

    // Default: S1, "distance"
    public UltrasonicSensor() {
        this(SensorPort.S1, "distance");
    }

    // Default mode: "distance"
    public UltrasonicSensor(Port port) {
        this(port, "distance");
    }

    /**
     * @param port SensorPort
     * @param mode "distance" or "listen"
     */
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
            
            // Distance mode returns meters - convert to cm for readability
            if ("distance".equals(mode)) {
                float distanceCm = sample[0] * 100.0f;
                
                // Ultrasonic max range is ~255cm, check for valid range
                if (distanceCm > 255.0f || Float.isNaN(distanceCm)) {
                    distanceCm = 255.0f; // Max out-of-range value
                }
                
                // Simple format: ultrasonic=45.2
                return "ultrasonic=" + String.format("%.1f", distanceCm);
            } else {
                // Listen mode returns 0 or 1 (boolean - detects other ultrasonic sensors)
                return "ultrasonic=" + (sample[0] > 0.5f ? "1" : "0");
            }
        } catch (Exception e) {
            // Sensor read error (disconnected, hardware failure)
            return null;
        }
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