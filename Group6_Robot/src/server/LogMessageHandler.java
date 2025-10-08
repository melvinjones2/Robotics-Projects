package server;

import java.io.BufferedWriter;

public class LogMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public LogMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        String formatted = "[EV3][LOG] " + msg.substring(4).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }
}