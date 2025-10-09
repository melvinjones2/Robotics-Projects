package server;

import java.io.BufferedWriter;
import java.io.IOException;

public class BeepMessageHandler implements IMessageHandler {
    private final ServerGUI gui;

    public BeepMessageHandler(ServerGUI gui) {
        this.gui = gui; 
    }

    @Override
    public void handle(String msg, BufferedWriter out) throws IOException {
        String formatted = "[EV3][BEEP] " + msg.substring(5).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }

}
