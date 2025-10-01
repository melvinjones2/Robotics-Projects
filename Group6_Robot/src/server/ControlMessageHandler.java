package server;

import java.io.BufferedWriter;

public class ControlMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public ControlMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        LogManager.log("[EV3][CONTROL] " + msg.substring(8).trim());
        gui.appendLog(msg, false);
    }
}