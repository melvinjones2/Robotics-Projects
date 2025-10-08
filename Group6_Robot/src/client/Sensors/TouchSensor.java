package client.Sensors;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.SampleProvider;
import client.Interfaces.ISensor;

public class TouchSensor implements ISensor {
    private EV3TouchSensor sensor;
    private SampleProvider provider;
    private float[] sample;

    public TouchSensor() {
        this(SensorPort.S2);
    }

    public TouchSensor(Port port) {
        sensor = new EV3TouchSensor(port);
        provider = sensor.getTouchMode();
        sample = new float[provider.sampleSize()];
    }

    public String readValue() {
        provider.fetchSample(sample, 0);
        return "SENSOR:TOUCH:" + ((sample[0] > 0) ? "PRESSED" : "RELEASED");
    }

    public void close() {
        sensor.close();
    }
}