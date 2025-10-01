package server;

import java.io.BufferedWriter;

public class MotorMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public MotorMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        LogManager.log("[EV3][MOTOR] " + msg.substring(6).trim());
        gui.appendLog(msg, false);
    }
}