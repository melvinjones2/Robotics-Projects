package client;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.SampleProvider;

public class LightSensor implements ISensor {
    private EV3ColorSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;

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
     * @param mode "red", "ambient", or "colorid"
     */
    public LightSensor(Port port, String mode) {
        sensor = new EV3ColorSensor(port);
        this.mode = mode.toLowerCase();
        if ("ambient".equals(this.mode)) {
            provider = sensor.getAmbientMode();
        } else if ("colorid".equals(this.mode)) {
            provider = sensor.getColorIDMode();
        } else {
            provider = sensor.getRedMode();
            this.mode = "red";
        }
        sample = new float[provider.sampleSize()];
    }

    public String readValue() {
        provider.fetchSample(sample, 0);
        if ("colorid".equals(mode)) {
            return "SENSOR:LIGHT:COLORID:" + (int)sample[0];
        } else {
            return "SENSOR:LIGHT:" + mode.toUpperCase() + ":" + sample[0];
        }
    }

    public void close() {
        sensor.close();
    }
}