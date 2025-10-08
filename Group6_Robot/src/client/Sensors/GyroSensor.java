package client.Sensors;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;
import client.Interfaces.ISensor;

public class GyroSensor implements ISensor {
    private EV3GyroSensor sensor;
    private SampleProvider provider;
    private float[] sample;
    private String mode;

    // Default: S3, "angle"
    public GyroSensor() {
        this(SensorPort.S3, "angle");
    }

    // Default mode: "angle"
    public GyroSensor(Port port) {
        this(port, "angle");
    }

    /**
     * @param port SensorPort
     * @param mode "angle" or "rate"
     */
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

    public String readValue() {
        provider.fetchSample(sample, 0);
        return "SENSOR:GYRO:" + mode.toUpperCase() + ":" + sample[0];
    }

    public void close() {
        sensor.close();
    }
}