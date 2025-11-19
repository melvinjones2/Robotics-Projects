package server.client;

import common.ProtocolConstants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import server.autonomous.ServerAutonomousController;
import server.gui.ServerGUI;
import server.handlers.BatteryLoggingMessageHandler;
import server.handlers.ByeMessageHandler;
import server.handlers.GenericMessageHandler;
import server.handlers.IMessageHandler;
import server.handlers.SensorMessageHandler;
import server.handlers.TickAckMessageHandler;
import server.handlers.TickMessageHandler;
import server.logging.LogManager;

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

    private ServerAutonomousController autonomousController;
    
    public void setAutonomousController(ServerAutonomousController controller) {
        this.autonomousController = controller;
        // Re-init handlers to include sensor handler
        if (controller != null) {
            handlers.put("SENSOR:", new SensorMessageHandler(controller));
        }
    }
    
    private void initHandlers() {
        handlers.put("BATTERY:", new GenericMessageHandler(gui, "BATTERY", 8));
        handlers.put("REPLY:", new GenericMessageHandler(gui, "REPLY", 6));
        handlers.put("CONTROL:", new GenericMessageHandler(gui, "CONTROL", 8));
        handlers.put("MOTOR:", new GenericMessageHandler(gui, "MOTOR", 6));
        handlers.put("LOG:", new GenericMessageHandler(gui, "LOG", 4));
        handlers.put("TICK_ACK:", new TickAckMessageHandler(gui));
        handlers.put("TICK:", new TickMessageHandler(gui, frameCount));
        handlers.put("BYE:", new ByeMessageHandler(gui, running, frameCount));
        handlers.put("BEEP:", new GenericMessageHandler(gui, "BEEP", 5));
        handlers.put("BATTERY_LOGGING:", new BatteryLoggingMessageHandler(gui));
    }

    public void dispatch(String msg, BufferedWriter out) throws IOException {
        boolean handled = false;
        boolean isCommand = false;
        
        for (Map.Entry<String, IMessageHandler> entry : handlers.entrySet()) {
            if (msg.startsWith(entry.getKey())) {
                entry.getValue().handle(msg, out);
                handled = true;
                
                // Send ACK for commands (not for TICK, LOG, BATTERY, REPLY, SENSOR)
                String key = entry.getKey();
                isCommand = !key.equals("TICK:") && !key.equals("TICK_ACK:") && 
                           !key.equals("LOG:") && !key.equals("BATTERY:") && 
                           !key.equals("REPLY:") && !key.equals("BATTERY_LOGGING:") &&
                           !key.equals("SENSOR:");
                
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
        // Use type-safe message parser to extract frame
        int frame = ProtocolConstants.parseCmdAckMessage(originalMsg);
        if (frame >= 0) {
            server.Server.sendSafe(out, ProtocolConstants.buildCmdAckMessage(frame));
            LogManager.debug("Sent ACK for frame " + frame);
        }
    }
    
    private int extractFrame(String msg) {
        // Use type-safe message parser
        return ProtocolConstants.parseCmdAckMessage(msg);
    }
}
