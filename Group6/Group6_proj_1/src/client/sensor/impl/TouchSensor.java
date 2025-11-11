package client.sensor.impl;

import client.sensor.ISensor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.SampleProvider;

public class TouchSensor implements ISensor {
    private EV3TouchSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private boolean closed = false;

    public TouchSensor() {
        this(SensorPort.S2);
    }

    public TouchSensor(Port port) {
        sensor = new EV3TouchSensor(port);
        provider = sensor.getTouchMode();
        sample = new float[provider.sampleSize()];
    }

    @Override
    public String getName() {
        return "touch";
    }

    @Override
    public String readValue() {
        if (closed || sensor == null) {
            return null;
        }
        
        try {
            provider.fetchSample(sample, 0);
            // Touch sensor returns 1.0 when pressed, 0.0 when released
            // Simple format: touch=1 or touch=0
            return "touch=" + (sample[0] > 0.5f ? "1" : "0");
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