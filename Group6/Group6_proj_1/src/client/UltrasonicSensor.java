package client;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class UltrasonicSensor implements ISensor {
    private EV3UltrasonicSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;

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

    public String readValue() {
        provider.fetchSample(sample, 0);
        return "SENSOR:US:" + mode.toUpperCase() + ":" + sample[0];
    }

    public void close() {
        sensor.close();
    }
}