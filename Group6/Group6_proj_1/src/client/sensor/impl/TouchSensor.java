package client.sensor.impl;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;

public class TouchSensor extends BaseSampleSensor<EV3TouchSensor> {

    public TouchSensor() {
        this(SensorPort.S2);
    }

    public TouchSensor(Port port) {
        this(new EV3TouchSensor(port));
    }

    private TouchSensor(EV3TouchSensor sensor) {
        super("touch", sensor, sensor.getTouchMode(), new SensorCloser<EV3TouchSensor>() {
            @Override
            public void close(EV3TouchSensor sensor) {
                sensor.close();
            }
        });
    }

    @Override
    protected String formatSample(float[] sample) {
        return "touch=" + (sample[0] > 0.5f ? "1" : "0");
    }
}
