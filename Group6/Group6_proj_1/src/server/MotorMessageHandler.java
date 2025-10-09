package server;

import java.io.BufferedWriter;

public class MotorMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public MotorMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        String formatted = "[EV3][MOTOR] " + msg.substring(6).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }
}