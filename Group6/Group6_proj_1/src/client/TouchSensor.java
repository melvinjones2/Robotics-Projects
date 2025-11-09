package client;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.SampleProvider;

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

    @Override
    public String getName() {
        return "touch";
    }

    @Override
    public String readValue() {
        provider.fetchSample(sample, 0);
        return "SENSOR:TOUCH:" + ((sample[0] > 0) ? "PRESSED" : "RELEASED");
    }

    @Override
    public void close() {
        sensor.close();
    }
}