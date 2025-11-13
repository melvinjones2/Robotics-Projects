package client.sensor.impl;

import client.sensor.ISensor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;

// EV3 Infrared sensor - distance and beacon seeking modes
public class InfraredSensor implements ISensor {
    private EV3IRSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;
    private boolean closed = false;

    public InfraredSensor() {
        this(SensorPort.S4, "distance");
    }

    public InfraredSensor(Port port) {
        this(port, "distance");
    }

    public InfraredSensor(Port port, String mode) {
        sensor = new EV3IRSensor(port);
        this.mode = mode.toLowerCase();
        if ("seek".equals(this.mode)) {
            provider = sensor.getSeekMode();
        } else {
            provider = sensor.getDistanceMode();
            this.mode = "distance";
        }
        sample = new float[provider.sampleSize()];
    }

    @Override
    public String getName() {
        return "infrared";
    }

    @Override
    public String readValue() {
        if (closed || sensor == null) {
            return null;
        }
        
        try {
            provider.fetchSample(sample, 0);
            
            if ("distance".equals(mode)) {
                float distance = sample[0];
                
                if (distance > 100.0f || Float.isNaN(distance)) {
                    distance = 100.0f;
                }
                if (distance < 0) {
                    distance = 0;
                }
                
                return "infrared=" + String.format("%.1f", distance);
            } else {
                if (sample.length >= 2) {
                    float bearing = sample[0];
                    float distance = sample[1];
                    return "infrared=" + String.format("%.0f,%.0f", bearing, distance);
                } else {
                    return "infrared=0,100";
                }
            }
        } catch (Exception e) {
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
