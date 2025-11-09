package server.handlers;

import java.io.BufferedWriter;

import server.gui.ServerGUI;
import server.logging.LogManager;

public class ReplyMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public ReplyMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        String formatted = "[EV3][REPLY] " + msg.substring(6).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }
}