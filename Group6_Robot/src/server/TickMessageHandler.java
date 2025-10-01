package server;

import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class TickMessageHandler implements IMessageHandler {
    private final ServerGUI gui;
    private final AtomicInteger frameCount;

    public TickMessageHandler(ServerGUI gui, AtomicInteger frameCount) {
        this.gui = gui;
        this.frameCount = frameCount;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        frameCount.incrementAndGet();
        LogManager.log("[EV3][TICK] " + msg.substring(4).trim());
        gui.appendLog(msg, false);
    }
}