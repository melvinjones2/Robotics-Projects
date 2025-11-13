package client.network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

// Sends periodic TICK messages to keep connection alive
public class HeartbeatThread implements Runnable {
    
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final int intervalMs;
    private int frameCount = 0;
    
    public HeartbeatThread(BufferedWriter out, AtomicBoolean running, int intervalMs) {
        this.out = out;
        this.running = running;
        this.intervalMs = intervalMs;
    }
    
    @Override
    public void run() {
        while (running.get()) {
            try {
                out.write("TICK:" + frameCount + "\n");
                out.flush();
                frameCount++;
                Thread.sleep(intervalMs);
            } catch (IOException e) {
                running.set(false);
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
