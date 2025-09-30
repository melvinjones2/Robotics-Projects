package server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BatteryLoggingPayload implements Payload {

    private boolean running = false;
    private Thread polling;
    private int recordIndex = 0;
    private FileWriter csvWriter;

    public String getName() {
        return "Battery Logger";
    }

    public void start() {
        LogManager.log("Battery logging started.");
        running = true;
        try {
            csvWriter = new FileWriter("battery_log.csv", true); // Append mode
            // Write header if file is new
            csvWriter.write("Date,Time,Record Index/ID,Voltage (volts),Battery Current (amps),Motor Current (amps),Real Movement (inch/cm)\n");
            csvWriter.flush();
        } catch (IOException e) {
            LogManager.log("Failed to open CSV file: " + e.getMessage());
        }
        polling = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        int frame = server.ServerMain.getFrameCount();
                        server.ServerMain.sendMessage("GET_BATTERY", "", frame);
                        LogManager.log("Requested battery status from client.");
                    } catch (IOException e) {
                        LogManager.log("Failed to request battery status: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(5000); // Log every 5 seconds
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        polling.setDaemon(true);
        polling.start();
    }

    public void stop() {
        LogManager.log("Battery logging stopped.");
        running = false;
        if (polling != null) {
            polling.interrupt();
        }
        try {
            if (csvWriter != null) {
                csvWriter.close();
            }
        } catch (IOException ignored) {
        }
    }

    // Parse BATTERY: response and log to CSV
    public void handleMessage(String msg, BufferedWriter out, ServerGUI gui) {
        if (msg.startsWith("BATTERY:")) {
            LogManager.log("[PAYLOAD][BATTERY] " + msg.substring(8).trim());
            gui.appendLog(msg, false);

            // Example expected format: BATTERY: VOLTAGE=7.2 V, CURRENT=0.5 A, LEVEL=80%
            String data = msg.substring(8).trim();
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

            // Parse values (customize as needed for your format)
            String voltage = "", batteryCurrent = "", motorCurrent = "", movement = "";
            String[] parts = data.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("VOLTAGE=")) {
                    voltage = part.replace("VOLTAGE=", "").replace("V", "").trim(); 
                }else if (part.startsWith("CURRENT=")) {
                    batteryCurrent = part.replace("CURRENT=", "").replace("A", "").trim(); 
                }else if (part.startsWith("LEVEL=")) ; // skip
                // TODO: Parse motorCurrent and movement if included in message
            }

            String row = String.format("%s,%s,%d,%s,%s,%s,%s\n",
                    date, time, recordIndex,
                    voltage, batteryCurrent, motorCurrent, movement);

            try {
                if (csvWriter != null) {
                    csvWriter.write(row);
                    csvWriter.flush();
                }
            } catch (IOException e) {
                LogManager.log("Failed to write to CSV: " + e.getMessage());
            }
            recordIndex++;
        }
    }
}
