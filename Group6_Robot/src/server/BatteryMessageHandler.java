package server;

import java.io.BufferedWriter;

public class BatteryMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public BatteryMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        String formatted = "[EV3][BATTERY] " + msg.substring(8).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }
}