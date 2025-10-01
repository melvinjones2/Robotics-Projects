package server;

import java.io.BufferedWriter;
import java.io.IOException;

public class BatteryLoggingPayload implements Payload {
    public String getName() { return "Battery Logger"; }

    public void start() {
        LogManager.log("Battery logging started.");
        // Request battery status from client
        BufferedWriter writer = ServerMain.getWriter();
        if (writer != null) {
            try {
                writer.write("GET_BATTERY\n");
                writer.flush();
                LogManager.log("Requested battery status from client.");
            } catch (IOException e) {
                LogManager.log("Failed to request battery status: " + e.getMessage());
            }
        }
    }

    public void stop() {
        LogManager.log("Battery logging stopped.");
    }

    public void handleMessage(String msg, BufferedWriter out, ServerGUI gui) {
        if (msg.startsWith("BATTERY:")) {
            LogManager.log("[PAYLOAD][BATTERY] " + msg.substring(8).trim());
            gui.appendLog(msg, false);
        }
    }
}