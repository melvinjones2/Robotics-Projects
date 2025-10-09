package server;

import java.io.BufferedWriter;

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