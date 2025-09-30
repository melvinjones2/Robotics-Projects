package server;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private static final int MAX_LOGS = 10;
    private static BufferedWriter logWriter = null;

    public static void rotateLogs() {
        try {
            Path oldest = Paths.get("server_log" + (MAX_LOGS - 1) + ".txt");
            if (Files.exists(oldest)) {
                Files.delete(oldest);
            }
            for (int i = MAX_LOGS - 2; i >= 0; i--) {
                Path src = Paths.get("server_log" + i + ".txt");
                Path dest = Paths.get("server_log" + (i + 1) + ".txt");
                if (Files.exists(src)) {
                    Files.move(src, dest);
                }
            }
        } catch (IOException e) {
            System.err.println("Log rotation error: " + e.getMessage());
        }
    }

    public static void openLog() throws IOException {
        logWriter = new BufferedWriter(new FileWriter("server_log0.txt", false));
    }

    public static void log(String msg) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String logMsg = "[" + timestamp + "] " + msg;
        System.out.println(logMsg);
        if (logWriter != null) {
            try {
                logWriter.write(logMsg);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException ignored) {
            }
        }
    }

    public static void close() {
        try {
            if (logWriter != null) {
                logWriter.close();
        
            }} catch (IOException ignored) {
        }
    }
}
