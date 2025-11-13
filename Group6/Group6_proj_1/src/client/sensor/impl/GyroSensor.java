package client.sensor.impl;

import client.sensor.ISensor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;

public class GyroSensor implements ISensor {
    private EV3GyroSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;
    private boolean closed = false;

    public GyroSensor() {
        this(SensorPort.S3, "angle");
    }

    public GyroSensor(Port port) {
        this(port, "angle");
    }

    public GyroSensor(Port port, String mode) {
        sensor = new EV3GyroSensor(port);
        this.mode = mode.toLowerCase();
        if ("rate".equals(this.mode)) {
            provider = sensor.getRateMode();
        } else {
            provider = sensor.getAngleMode();
            this.mode = "angle";
        }
        sample = new float[provider.sampleSize()];
    }

    @Override
    public String getName() {
        return "gyro";
    }

    @Override
    public String readValue() {
        if (closed || sensor == null) {
            return null;
        }
        
        try {
            provider.fetchSample(sample, 0);
            
            return "gyro=" + String.format("%.1f", sample[0]);
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
            provider.fetchSample(sample, 0);
            return !Float.isNaN(sample[0]);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void reset() {
        if (!closed && sensor != null) {
            try {
                sensor.reset();
            } catch (Exception e) {
                // Ignore reset errors
            }
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