package server;

import java.io.BufferedWriter;

public class TickAckMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public TickAckMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        LogManager.log("[EV3][TICK_ACK] " + msg.substring(9).trim());
        gui.appendLog(msg, false);
    }
}