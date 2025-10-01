package server;

import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ByeMessageHandler implements IMessageHandler {
    private final ServerGUI gui;
    private final AtomicBoolean running;
    private final AtomicInteger frameCount;

    public ByeMessageHandler(ServerGUI gui, AtomicBoolean running, AtomicInteger frameCount) {
        this.gui = gui;
        this.running = running;
        this.frameCount = frameCount;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        LogManager.log("[EV3][BYE] " + msg.trim());
        gui.appendLog(msg, false);
        running.set(false);
        frameCount.set(0);
    }
}