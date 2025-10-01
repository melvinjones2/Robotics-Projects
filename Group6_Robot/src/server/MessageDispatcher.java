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
    }

    public void dispatch(String msg, BufferedWriter out) throws IOException {
        boolean handled = false;
        for (Map.Entry<String, IMessageHandler> entry : handlers.entrySet()) {
            if (msg.startsWith(entry.getKey())) {
                entry.getValue().handle(msg, out);
                handled = true;
                break;
            }
        }
        if (!handled) {
            LogManager.log("[EV3][UNKNOWN] " + msg.trim());
            gui.appendLog(msg, false);
        }
        if ("BYE".equalsIgnoreCase(msg)) {
            running.set(false);
        }
    }
}
