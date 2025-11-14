package client.sensor.impl;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;

// EV3 Infrared sensor - distance and beacon seeking modes
public class InfraredSensor extends BaseSampleSensor<EV3IRSensor> {

    private final String mode;

    public InfraredSensor() {
        this(SensorPort.S4, "distance");
    }

    public InfraredSensor(Port port) {
        this(port, "distance");
    }

    public InfraredSensor(Port port, String mode) {
        this(new EV3IRSensor(port), normalizeMode(mode));
    }

    private InfraredSensor(EV3IRSensor sensor, String normalizedMode) {
        super("infrared", sensor, selectProvider(sensor, normalizedMode), new SensorCloser<EV3IRSensor>() {
            @Override
            public void close(EV3IRSensor sensor) {
                sensor.close();
            }
        });
        this.mode = normalizedMode;
    }

    private static String normalizeMode(String mode) {
        if (mode == null) {
            return "distance";
        }
        String normalized = mode.toLowerCase();
        return "seek".equals(normalized) ? "seek" : "distance";
    }

    private static SampleProvider selectProvider(EV3IRSensor sensor, String normalizedMode) {
        return "seek".equals(normalizedMode) ? sensor.getSeekMode() : sensor.getDistanceMode();
    }

    @Override
    protected String formatSample(float[] sample) {
        if ("distance".equals(mode)) {
            float distance = sample[0];
            if (Float.isNaN(distance) || distance > 100.0f) {
                distance = 100.0f;
            }
            if (distance < 0) {
                distance = 0;
            }
            return "infrared=" + String.format("%.1f", distance);
        }

        if (sample.length >= 2) {
            float bearing = sample[0];
            float distance = sample[1];
            return "infrared=" + String.format("%.0f,%.0f", bearing, distance);
        }
        return "infrared=0,100";
    }
}
