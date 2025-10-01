package server;

import java.io.BufferedWriter;

public class BatteryMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public BatteryMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        LogManager.log("[EV3][BATTERY] " + msg.substring(8).trim());
        gui.appendLog(msg, false);
    }
}