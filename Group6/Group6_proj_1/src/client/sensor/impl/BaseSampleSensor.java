package client.sensor.impl;

import client.sensor.ISensor;
import lejos.robotics.SampleProvider;

/**
 * Shared template for sensors that expose a {@link SampleProvider}.
 * Handles sample buffer allocation, availability checks, and cleanup.
 */
abstract class BaseSampleSensor<S> implements ISensor {

    interface SensorCloser<S> {
        void close(S sensor);
    }

    private final String name;
    private final S sensor;
    private final SampleProvider provider;
    private final float[] sample;
    private final SensorCloser<S> closer;
    private boolean closed;

    protected BaseSampleSensor(String name, S sensor, SampleProvider provider, SensorCloser<S> closer) {
        this.name = name;
        this.sensor = sensor;
        this.provider = provider;
        this.sample = provider != null ? new float[provider.sampleSize()] : new float[0];
        this.closer = closer != null ? closer : new SensorCloser<S>() {
            @Override
            public void close(S sensor) {
                // no-op
            }
        };
    }

    @Override
    public String getName() {
        return name;
    }

    protected S sensor() {
        return sensor;
    }

    protected float[] sampleBuffer() {
        return sample;
    }

    protected SampleProvider provider() {
        return provider;
    }

    protected boolean isOperational() {
        return !closed && sensor != null && provider != null;
    }

    @Override
    public String readValue() {
        if (!isOperational()) {
            return null;
        }
        try {
            provider.fetchSample(sample, 0);
            return formatSample(sample);
        } catch (Exception e) {
            return null;
        }
    }

    protected abstract String formatSample(float[] sample);

    @Override
    public boolean isAvailable() {
        if (!isOperational()) {
            return false;
        }
        try {
            provider.fetchSample(sample, 0);
            return !Float.isNaN(sample[0]);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (!closed && sensor != null) {
            try {
                closer.close(sensor);
            } catch (Exception e) {
                // Ignore shutdown issues
            }
            closed = true;
        }
    }
}
