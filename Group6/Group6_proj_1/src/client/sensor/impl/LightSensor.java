package client.sensor.impl;

import client.sensor.ISensor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.SampleProvider;

public class LightSensor implements ISensor {
    private EV3ColorSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;
    private boolean closed = false;

    public LightSensor() {
        this(SensorPort.S4, "red");
    }

    public LightSensor(Port port) {
        this(port, "red");
    }

    public LightSensor(Port port, String mode) {
        sensor = new EV3ColorSensor(port);
        this.mode = mode.toLowerCase();
        if ("ambient".equals(this.mode)) {
            provider = sensor.getAmbientMode();
        } else if ("colorid".equals(this.mode)) {
            provider = sensor.getColorIDMode();
        } else if ("rgb".equals(this.mode)) {
        	provider = sensor.getRGBMode();
        } else {
            provider = sensor.getRedMode();
            this.mode = "red";
        }
        sample = new float[provider.sampleSize()];
    }

    @Override
    public String getName() {
        return "light";
    }

    @Override
    public String readValue() {
        if (closed || sensor == null) {
            return null;
        }
        
        try {
            provider.fetchSample(sample, 0);
            
            if ("colorid".equals(mode)) {
                int colorId = (int)sample[0];
                return "light=" + colorId;
            } else if ("rgb".equals(mode)) {
                float r = sample[0];
                float g = sample.length > 1 ? sample[1] : 0;
                float b = sample.length > 2 ? sample[2] : 0;
                return "light=" + String.format("%.2f,%.2f,%.2f", r, g, b);
            } else {
                return "light=" + String.format("%.2f", sample[0]);
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
            provider.fetchSample(sample, 0);
            return !Float.isNaN(sample[0]);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Convert color ID to human-readable name.
     */
    private String getColorName(int colorId) {
        switch (colorId) {
            case 0: return "None";
            case 1: return "Black";
            case 2: return "Blue";
            case 3: return "Green";
            case 4: return "Yellow";
            case 5: return "Red";
            case 6: return "White";
            case 7: return "Brown";
            default: return "Unknown";
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