package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class TickMessageHandler implements IMessageHandler {
    private final ServerGUI gui;
    private final AtomicInteger frameCount;
    private long lastTickTime = System.currentTimeMillis();
    private int clientFrame = -1;

    public TickMessageHandler(ServerGUI gui, AtomicInteger frameCount) {
        this.gui = gui;
        this.frameCount = frameCount;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        long now = System.currentTimeMillis();
        long tickDelta = now - lastTickTime;
        lastTickTime = now;
        
        // Extract client frame from TICK:123
        clientFrame = extractFrame(msg);
        int serverFrame = frameCount.incrementAndGet();
        
        // Detect sync issues
        if (Math.abs(clientFrame - serverFrame) > 10) {
            LogManager.warn(String.format("Frame drift detected! Client:%d Server:%d Diff:%d", 
                clientFrame, serverFrame, Math.abs(clientFrame - serverFrame)));
        }
        
        // Log tick with timing info
        if (tickDelta > 100) { // Only log slow ticks
            LogManager.debug(String.format("[TICK] Frame:%d Delta:%dms", clientFrame, tickDelta));
        }
        
        // Send TICK_ACK back to client
        try {
            sendTickAck(out, serverFrame);
        } catch (IOException e) {
            LogManager.error("Failed to send TICK_ACK", e);
        }
    }
    
    private void sendTickAck(BufferedWriter out, int frame) throws IOException {
        synchronized (out) {
            out.write("TICK_ACK:" + frame);
            out.write("\n");
            out.flush();
        }
    }
    
    private int extractFrame(String msg) {
        try {
            // TICK:123
            return Integer.parseInt(msg.substring(5).trim());
        } catch (Exception e) {
            return -1;
        }
    }
    
    public int getClientFrame() {
        return clientFrame;
    }
}