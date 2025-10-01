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
    private final Map<String, MessageHandler> handlers = new HashMap<String, MessageHandler>();

    public MessageDispatcher(ServerGUI gui, AtomicBoolean running, AtomicInteger frameCount) {
        this.gui = gui;
        this.running = running;
        this.frameCount = frameCount;
        initHandlers();
    }

    private interface MessageHandler {

        void handle(String msg, BufferedWriter out) throws IOException;
    }

    private void initHandlers() {
        handlers.put("BATTERY:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) {
                logAndGui("[EV3][BATTERY] ", msg, 8, false);
            }
        });
        handlers.put("REPLY:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) {
                logAndGui("[EV3][REPLY] ", msg, 6, false);
            }
        });
        handlers.put("CONTROL:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) {
                logAndGui("[EV3][CONTROL] ", msg, 8, false);
            }
        });
        handlers.put("MOTOR:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) {
                logAndGui("[EV3][MOTOR] ", msg, 8, false);
            }
        });
        handlers.put("LOG:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) {
                logAndGui("[EV3][LOG] ", msg, 4, false);
            }
        });
        handlers.put("TICK_ACK:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) {
                if (gui.isDebugMode()) {
                    logAndGui("[DEBUG][TICK_ACK] Received ", msg, 0, true);
                }
            }
        });
        handlers.put("TICK:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) throws IOException {
                int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                int serverTick = frameCount.incrementAndGet();
                Server.send(out, "TICK_ACK:" + serverTick);
                if (gui.isDebugMode()) {
                    logAndGui("[DEBUG][TICK_ACK] Sent ", "TICK_ACK:" + serverTick, 0, true);
                }
            }
        });
        handlers.put("BYE:", new MessageHandler() {
            public void handle(String msg, BufferedWriter out) throws IOException {
                try {
                    int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                    LogManager.log("[EV3][BYE] Client frame: " + clientTick + ", Server frame: " + frameCount.get());
                    Server.send(out, "BYE_ACK:" + frameCount.get());
                } catch (Exception e) {
                    Server.send(out, "BYE_ACK:" + frameCount.get());
                }
                running.set(false);
            }
        });
    }

    public void dispatch(String msg, BufferedWriter out) throws IOException {
        boolean handled = false;
        for (Map.Entry<String, MessageHandler> entry : handlers.entrySet()) {
            if (msg.startsWith(entry.getKey())) {
                entry.getValue().handle(msg, out);
                handled = true;
                break;
            }
        }
        if (!handled) {
            logAndGui("[EV3][UNKNOWN] ", msg, 0, false);
        }
        if ("BYE".equalsIgnoreCase(msg)) {
            running.set(false);
        }
    }

    private void logAndGui(String prefix, String msg, int skip, boolean debug) {
        LogManager.log(prefix + msg.substring(skip).trim());
        gui.appendLog(msg, debug);
    }
}
