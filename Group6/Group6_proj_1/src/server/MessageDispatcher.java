package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageDispatcher {

    private final ServerGUI gui;
    private final AtomicBoolean running;
    private final AtomicInteger frameCount;
    private final Map<String, IMessageHandler> handlers = new HashMap<>();

    public MessageDispatcher(ServerGUI gui, AtomicBoolean running, AtomicInteger frameCount) {
        this.gui = gui;
        this.running = running;
        this.frameCount = frameCount;
        initHandlers();
    }

    private void initHandlers() {
        handlers.put("BATTERY:", new BatteryMessageHandler(gui));
        handlers.put("REPLY:", new ReplyMessageHandler(gui));
        handlers.put("CONTROL:", new ControlMessageHandler(gui));
        handlers.put("MOTOR:", new MotorMessageHandler(gui));
        handlers.put("LOG:", new LogMessageHandler(gui));
        handlers.put("TICK_ACK:", new TickAckMessageHandler(gui));
        handlers.put("TICK:", new TickMessageHandler(gui, frameCount));
        handlers.put("BYE:", new ByeMessageHandler(gui, running, frameCount));
        handlers.put("BEEP:", new BeepMessageHandler(gui));
        handlers.put("BATTERY_LOGGING:", new BatteryLoggingMessageHandler(gui));
    }

    public void dispatch(String msg, BufferedWriter out) throws IOException {
        boolean handled = false;
        boolean isCommand = false;
        
        for (Map.Entry<String, IMessageHandler> entry : handlers.entrySet()) {
            if (msg.startsWith(entry.getKey())) {
                entry.getValue().handle(msg, out);
                handled = true;
                
                // Send ACK for commands (not for TICK, LOG, BATTERY, REPLY)
                String key = entry.getKey();
                isCommand = !key.equals("TICK:") && !key.equals("TICK_ACK:") && 
                           !key.equals("LOG:") && !key.equals("BATTERY:") && 
                           !key.equals("REPLY:") && !key.equals("BATTERY_LOGGING:");
                
                if (isCommand) {
                    sendCommandAck(msg, out);
                }
                break;
            }
        }
        
        if (!handled) {
            LogManager.warn("[EV3][UNKNOWN] " + msg.trim());
            gui.appendLog(msg, false);
        }
        
        if ("BYE".equalsIgnoreCase(msg)) {
            running.set(false);
        }
    }
    
    private void sendCommandAck(String originalMsg, BufferedWriter out) throws IOException {
        int frame = extractFrame(originalMsg);
        if (frame >= 0) {
            synchronized (out) {
                out.write("CMD_ACK:" + frame);
                out.write("\n");
                out.flush();
            }
            LogManager.debug("Sent ACK for frame " + frame);
        }
    }
    
    private int extractFrame(String msg) {
        int colonIdx = msg.lastIndexOf(':');
        if (colonIdx > 0) {
            try {
                String potentialFrame = msg.substring(colonIdx + 1).trim();
                return Integer.parseInt(potentialFrame);
            } catch (NumberFormatException e) {
                // No valid frame number
            }
        }
        return -1;
    }
}
