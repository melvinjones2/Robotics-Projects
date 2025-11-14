package client.sensor.impl;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class UltrasonicSensor extends BaseSampleSensor<EV3UltrasonicSensor> {

    private static final int FILTER_SIZE = 5;

    private final String mode;
    private final float[] filterBuffer = new float[FILTER_SIZE];
    private int filterIndex = 0;
    private int filterCount = 0;

    public UltrasonicSensor() {
        this(SensorPort.S1, "distance");
    }

    public UltrasonicSensor(Port port) {
        this(port, "distance");
    }

    public UltrasonicSensor(Port port, String mode) {
        this(new EV3UltrasonicSensor(port), normalizeMode(mode));
    }

    private final EV3UltrasonicSensor sensorRef;

    private UltrasonicSensor(EV3UltrasonicSensor sensor, String normalizedMode) {
        super("ultrasonic", sensor, selectProvider(sensor, normalizedMode), new SensorCloser<EV3UltrasonicSensor>() {
            @Override
            public void close(EV3UltrasonicSensor sensor) {
                sensor.close();
            }
        });
        this.sensorRef = sensor;
        this.mode = normalizedMode;
    }

    private static String normalizeMode(String mode) {
        if (mode == null) {
            return "distance";
        }
        String normalized = mode.toLowerCase();
        return "listen".equals(normalized) ? "listen" : "distance";
    }

    private static SampleProvider selectProvider(EV3UltrasonicSensor sensor, String normalizedMode) {
        return "listen".equals(normalizedMode) ? sensor.getListenMode() : sensor.getDistanceMode();
    }

    @Override
    protected String formatSample(float[] sample) {
        if ("distance".equals(mode)) {
            float distanceCm = sample[0] * 100.0f;
            if (Float.isNaN(distanceCm) || distanceCm > 255.0f) {
                distanceCm = 255.0f;
            }
            return "ultrasonic=" + String.format("%.1f", distanceCm);
        }
        return "ultrasonic=" + (sample[0] > 0.5f ? "1" : "0");
    }

    public float readRawDistance() {
        try {
            float[] sample = new float[sensorRef.getDistanceMode().sampleSize()];
            sensorRef.getDistanceMode().fetchSample(sample, 0);
            float distanceCm = sample[0] * 100.0f;
            if (Float.isNaN(distanceCm) || distanceCm > 255.0f) {
                return -1;
            }
            return distanceCm;
        } catch (Exception e) {
            return -1;
        }
    }
}
