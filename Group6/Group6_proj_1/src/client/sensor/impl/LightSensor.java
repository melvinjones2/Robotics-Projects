package client.sensor.impl;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.SampleProvider;

public class LightSensor extends BaseSampleSensor<EV3ColorSensor> {

    private final String mode;
    private SampleProvider rgbProvider;
    private float[] rgbSample;

    public LightSensor() {
        this(SensorPort.S4, "red");
    }

    public LightSensor(Port port) {
        this(port, "red");
    }

    public LightSensor(Port port, String mode) {
        this(new EV3ColorSensor(port), normalizeMode(mode));
    }

    private LightSensor(EV3ColorSensor sensor, String normalizedMode) {
        super("light", sensor, selectProvider(sensor, normalizedMode), new SensorCloser<EV3ColorSensor>() {
            @Override
            public void close(EV3ColorSensor sensor) {
                sensor.close();
            }
        });
        this.mode = normalizedMode;
    }

    private static String normalizeMode(String mode) {
        if (mode == null) {
            return "red";
        }
        String normalized = mode.toLowerCase();
        switch (normalized) {
            case "ambient":
            case "colorid":
            case "rgb":
                return normalized;
            default:
                return "red";
        }
    }

    private static SampleProvider selectProvider(EV3ColorSensor sensor, String normalizedMode) {
        switch (normalizedMode) {
            case "ambient":
                return sensor.getAmbientMode();
            case "colorid":
                return sensor.getColorIDMode();
            case "rgb":
                return sensor.getRGBMode();
            default:
                return sensor.getRedMode();
        }
    }

    @Override
    protected String formatSample(float[] sample) {
        if ("colorid".equals(mode)) {
            return "light=" + (int) sample[0];
        }
        if ("rgb".equals(mode)) {
            float r = sample[0];
            float g = sample.length > 1 ? sample[1] : 0;
            float b = sample.length > 2 ? sample[2] : 0;
            return "light=" + String.format("%.2f,%.2f,%.2f", r, g, b);
        }
        return "light=" + String.format("%.2f", sample[0]);
    }

    /**
     * Fetch a raw RGB sample regardless of the configured mode.
     * Returns null if the sensor is unavailable.
     */
    public float[] readRgbSample() {
        if (!isOperational()) {
            return null;
        }

        try {
            if (rgbProvider == null) {
                rgbProvider = sensor().getRGBMode();
                rgbSample = new float[rgbProvider.sampleSize()];
            }
            rgbProvider.fetchSample(rgbSample, 0);
            float[] copy = new float[rgbSample.length];
            System.arraycopy(rgbSample, 0, copy, 0, rgbSample.length);
            return copy;
        } catch (Exception e) {
            return null;
        }
    }
}
