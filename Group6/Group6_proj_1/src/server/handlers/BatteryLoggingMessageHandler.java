package server.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import server.gui.ServerGUI;
import server.logging.LogManager;

public class BatteryLoggingMessageHandler implements IMessageHandler {

    private final ServerGUI gui;
    private static final String CSV_FILE = "battery_log.csv";

    public BatteryLoggingMessageHandler(ServerGUI gui) {
        this.gui = gui;
    }

    @Override
    public void handle(String msg, BufferedWriter out) {
        String formatted = "[EV3][BATTERY_LOGGING] " + msg.substring(17).trim();
        LogManager.log(formatted);
        gui.appendLog(formatted, false);

        // Parse and save to CSV
        try {
            BatteryLogEntry entry = parseBatteryMessage(msg);
            if (entry != null) {
                appendToCSV(entry);
            }
        } catch (IOException e) {
            LogManager.log("Battery log parse error: " + e.getMessage());
        }
    }

    private BatteryLogEntry parseBatteryMessage(String msg) {
        // Example message: BATTERY: 8000mV, Voltage: 8.0V, Battery Current: 500mA, Motor Current: 200mA
        try {
            String[] parts = msg.substring(8).split(",");
            String voltageStr = parts[1].split(":")[1].replace("V", "").trim();
            String batteryCurrentStr = parts[2].split(":")[1].replace("mA", "").trim();
            String motorCurrentStr = parts[3].split(":")[1].replace("mA", "").trim();

            double voltage = Double.parseDouble(voltageStr);
            double batteryCurrent = Double.parseDouble(batteryCurrentStr) / 1000.0; // mA to A
            double motorCurrent = Double.parseDouble(motorCurrentStr) / 1000.0; // mA to A

            return new BatteryLogEntry(voltage, batteryCurrent, motorCurrent);
        } catch (Exception e) {
            LogManager.log("Parse error: " + e.getMessage());
            return null;
        }
    }

    private void appendToCSV(BatteryLogEntry entry) throws IOException {
        boolean fileExists = new java.io.File(CSV_FILE).exists();
        try (FileWriter fw = new FileWriter(CSV_FILE, true)) {
            if (!fileExists) {
                fw.write("Date,Time,Record Index/ID,Voltage (volts),Battery Current (amps),Motor Current (amps),Real Movement (inch/cm)\n");
            }
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String id = String.valueOf(System.currentTimeMillis());
            fw.write(String.format("%s,%s,%s,%.3f,%.3f,%.3f,\n",
                    date, time, id, entry.voltage, entry.batteryCurrent, entry.motorCurrent));
        }
    }

    // Helper class to hold parsed values
    private static class BatteryLogEntry {

        double voltage;
        double batteryCurrent;
        double motorCurrent;

        BatteryLogEntry(double voltage, double batteryCurrent, double motorCurrent) {
            this.voltage = voltage;
            this.batteryCurrent = batteryCurrent;
            this.motorCurrent = motorCurrent;
        }
    }
}
