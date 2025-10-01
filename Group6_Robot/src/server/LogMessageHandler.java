package server;

import java.io.BufferedWriter;

public class LogMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public LogMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        LogManager.log("[EV3][LOG] " + msg.substring(4).trim());
        gui.appendLog(msg, false);
    }
}