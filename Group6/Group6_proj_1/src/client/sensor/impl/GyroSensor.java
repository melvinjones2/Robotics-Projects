package client.sensor.impl;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;

public class GyroSensor extends BaseSampleSensor<EV3GyroSensor> {

    private final String mode;

    public GyroSensor() {
        this(SensorPort.S3, "angle");
    }

    public GyroSensor(Port port) {
        this(port, "angle");
    }

    public GyroSensor(Port port, String mode) {
        this(new EV3GyroSensor(port), normalizeMode(mode));
    }

    private GyroSensor(EV3GyroSensor sensor, String mode) {
        super("gyro", sensor, selectProvider(sensor, mode), new SensorCloser<EV3GyroSensor>() {
            @Override
            public void close(EV3GyroSensor sensor) {
                sensor.close();
            }
        });
        this.mode = mode;
    }

    private static String normalizeMode(String mode) {
        if (mode == null) {
            return "angle";
        }
        String normalized = mode.toLowerCase();
        return "rate".equals(normalized) ? "rate" : "angle";
    }

    private static SampleProvider selectProvider(EV3GyroSensor sensor, String normalizedMode) {
        if ("rate".equals(normalizedMode)) {
            return sensor.getRateMode();
        }
        return sensor.getAngleMode();
    }

    @Override
    protected String formatSample(float[] sample) {
        return "gyro=" + String.format("%.1f", sample[0]);
    }

    public void reset() {
        if (isOperational()) {
            try {
                sensor().reset();
            } catch (Exception e) {
                // Ignore reset issues
            }
        }
    }
}
