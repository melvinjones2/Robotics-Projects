package client;

import java.io.BufferedWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SensorThread implements Runnable {

    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final List<ISensor> sensors;

    public SensorThread(BufferedWriter out, AtomicBoolean running, List<ISensor> sensors) {
        this.out = out;
        this.running = running;
        this.sensors = sensors;
    }

    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            for (ISensor sensor : sensors) {
                String data = sensor.readValue();
                if (data != null) {
                    try {
                        out.write(data + "\n");
                        out.flush();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(500); // Adjust as needed
            } catch (InterruptedException e) {
                break;
            }
        }
        for (ISensor sensor : sensors) {
            sensor.close();
        }
    }
}