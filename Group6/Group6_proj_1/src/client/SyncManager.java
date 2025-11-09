package client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synchronizes client-server communication with frame tracking and ACK handling.
 * Ensures commands are sent in order and properly acknowledged.
 */
public class SyncManager {
    private final BufferedWriter out;
    private final AtomicInteger frameCount;
    private final BlockingQueue<String> ackQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger lastAckedFrame = new AtomicInteger(-1);
    private volatile long lastServerContactMs = System.currentTimeMillis();
    
    private static final int ACK_TIMEOUT_MS = 2000;
    private static final int SYNC_CHECK_INTERVAL_MS = 5000;
    
    public SyncManager(BufferedWriter out, AtomicInteger frameCount) {
        this.out = out;
        this.frameCount = frameCount;
    }
    
    /**
     * Send a command with frame synchronization.
     * Automatically adds frame number and waits for ACK if required.
     */
    public boolean sendCommand(String command, boolean waitForAck) throws IOException {
        int frame = frameCount.get();
        String fullCommand = command + ":" + frame;
        
        synchronized (out) {
            out.write(fullCommand);
            out.write("\n");
            out.flush();
        }
        
        ClientLogger.debug("Sent: " + fullCommand);
        
        if (waitForAck) {
            return waitForCommandAck(frame);
        }
        
        return true;
    }
    
    /**
     * Send a command without frame number (for legacy support).
     */
    public void sendRaw(String message) throws IOException {
        synchronized (out) {
            out.write(message);
            out.write("\n");
            out.flush();
        }
        ClientLogger.debug("Sent raw: " + message);
    }
    
    /**
     * Wait for an ACK for a specific frame.
     */
    private boolean waitForCommandAck(int expectedFrame) {
        try {
            String ack = ackQueue.poll(ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (ack != null) {
                // Parse ACK: "CMD_ACK:123"
                int ackedFrame = parseAckFrame(ack);
                if (ackedFrame == expectedFrame) {
                    lastAckedFrame.set(ackedFrame);
                    updateServerContact();
                    return true;
                } else {
                    ClientLogger.warn("Frame mismatch: expected " + expectedFrame + ", got " + ackedFrame);
                }
            } else {
                ClientLogger.warn("ACK timeout for frame " + expectedFrame);
            }
        } catch (InterruptedException e) {
            ClientLogger.error("ACK wait interrupted", e);
            Thread.currentThread().interrupt();
        }
        return false;
    }
    
    /**
     * Process an incoming ACK message from server.
     */
    public void receiveAck(String ackMessage) {
        ackQueue.offer(ackMessage);
        updateServerContact();
    }
    
    /**
     * Check if client-server sync is healthy.
     */
    public boolean isSyncHealthy() {
        int currentFrame = frameCount.get();
        int lastAcked = lastAckedFrame.get();
        int frameDrift = currentFrame - lastAcked;
        
        // Allow some drift but not too much
        if (frameDrift > 100) {
            ClientLogger.warn("Large frame drift detected: " + frameDrift);
            return false;
        }
        
        // Check if server is responsive
        long timeSinceContact = System.currentTimeMillis() - lastServerContactMs;
        if (timeSinceContact > SYNC_CHECK_INTERVAL_MS) {
            ClientLogger.warn("No server contact for " + timeSinceContact + "ms");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get current synchronization stats.
     */
    public SyncStats getStats() {
        return new SyncStats(
            frameCount.get(),
            lastAckedFrame.get(),
            System.currentTimeMillis() - lastServerContactMs
        );
    }
    
    private void updateServerContact() {
        lastServerContactMs = System.currentTimeMillis();
    }
    
    private int parseAckFrame(String ack) {
        try {
            // Expected format: "CMD_ACK:123"
            int colonIdx = ack.lastIndexOf(':');
            if (colonIdx > 0) {
                return Integer.parseInt(ack.substring(colonIdx + 1).trim());
            }
        } catch (Exception e) {
            ClientLogger.error("Failed to parse ACK: " + ack, e);
        }
        return -1;
    }
    
    public static class SyncStats {
        public final int currentFrame;
        public final int lastAckedFrame;
        public final long msSinceContact;
        
        public SyncStats(int currentFrame, int lastAckedFrame, long msSinceContact) {
            this.currentFrame = currentFrame;
            this.lastAckedFrame = lastAckedFrame;
            this.msSinceContact = msSinceContact;
        }
        
        public int getFrameDrift() {
            return currentFrame - lastAckedFrame;
        }
        
        @Override
        public String toString() {
            return String.format("Frame:%d Acked:%d Drift:%d LastContact:%dms",
                currentFrame, lastAckedFrame, getFrameDrift(), msSinceContact);
        }
    }
}
