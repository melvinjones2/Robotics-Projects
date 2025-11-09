package server.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import server.gui.ServerGUI;
import server.logging.LogManager;

/**
 * Handles command acknowledgments from server to client.
 * Improves reliability by confirming command receipt.
 */
public class CommandAckHandler implements IMessageHandler {
    private final ServerGUI gui;
    private final AtomicInteger frameCount;
    
    public CommandAckHandler(ServerGUI gui, AtomicInteger frameCount) {
        this.gui = gui;
        this.frameCount = frameCount;
    }
    
    @Override
    public void handle(String msg, BufferedWriter out) {
        // Send ACK back to client for received command
        try {
            // Extract frame from message if present
            int frame = extractFrame(msg);
            if (frame >= 0) {
                sendAck(out, frame);
                LogManager.debug("Sent ACK for frame " + frame);
            }
        } catch (IOException e) {
            LogManager.error("Failed to send ACK", e);
        }
    }
    
    private void sendAck(BufferedWriter out, int frame) throws IOException {
        synchronized (out) {
            out.write("CMD_ACK:" + frame);
            out.write("\n");
            out.flush();
        }
    }
    
    private int extractFrame(String msg) {
        // Extract frame number from "COMMAND:frame" format
        int colonIdx = msg.lastIndexOf(':');
        if (colonIdx > 0) {
            try {
                return Integer.parseInt(msg.substring(colonIdx + 1).trim());
            } catch (NumberFormatException e) {
                // No valid frame number
            }
        }
        return -1;
    }
}
