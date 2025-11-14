package server.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import server.gui.ServerGUI;
import server.logging.LogManager;

// Generic handler for simple message logging with format "[EV3][TYPE] message"
public class GenericMessageHandler implements IMessageHandler {
    private final ServerGUI gui;
    private final String messageType;
    private final int prefixLength;

    public GenericMessageHandler(ServerGUI gui, String messageType, int prefixLength) {
        this.gui = gui;
        this.messageType = messageType;
        this.prefixLength = prefixLength;
    }

    @Override
    public void handle(String msg, BufferedWriter out) throws IOException {
        String formatted = "[EV3][" + messageType + "] " + msg.substring(prefixLength).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);
    }
}
