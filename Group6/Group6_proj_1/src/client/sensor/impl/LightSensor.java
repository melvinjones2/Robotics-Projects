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

    // Default: S4, "red"
    public LightSensor() {
        this(SensorPort.S4, "red");
    }

    // Default mode: "red"
    public LightSensor(Port port) {
        this(port, "red");
    }

    /**
     * @param port SensorPort
     * @param mode "red", "ambient", "colorid", or "rgb"
     */
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
            
            // Simple format: light=value
            if ("colorid".equals(mode)) {
                // Color ID: 0=None, 1=Black, 2=Blue, 3=Green, 4=Yellow, 5=Red, 6=White, 7=Brown
                int colorId = (int)sample[0];
                return "light=" + colorId;
            } else if ("rgb".equals(mode)) {
                // RGB mode returns 3 values (R, G, B) normalized 0-1
                float r = sample[0];
                float g = sample.length > 1 ? sample[1] : 0;
                float b = sample.length > 2 ? sample[2] : 0;
                return "light=" + String.format("%.2f,%.2f,%.2f", r, g, b);
            } else {
                // Red or ambient mode - single value 0-1 (normalized)
                return "light=" + String.format("%.2f", sample[0]);
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