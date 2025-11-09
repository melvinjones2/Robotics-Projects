package server.handlers;

import java.io.BufferedWriter;

import server.gui.ServerGUI;
import server.logging.LogManager;

public class ControlMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public ControlMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        String formatted = "[EV3][CONTROL] " + msg.substring(8).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }
}