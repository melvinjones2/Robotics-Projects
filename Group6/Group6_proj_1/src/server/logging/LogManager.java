package server.logging;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {
    private static final int MAX_LOGS = 10;
    private static BufferedWriter logWriter = null;
    private static LogLevel minLevel = LogLevel.INFO; // Default minimum level
    private static long sessionStartTime = System.currentTimeMillis();

    public static void rotateLogs() {
        try {
            Path oldest = Paths.get("server_log" + (MAX_LOGS - 1) + ".txt");
            if (Files.exists(oldest)) Files.delete(oldest);
            for (int i = MAX_LOGS - 2; i >= 0; i--) {
                Path src = Paths.get("server_log" + i + ".txt");
                Path dest = Paths.get("server_log" + (i + 1) + ".txt");
                if (Files.exists(src)) Files.move(src, dest);
            }
        } catch (IOException e) {
            System.err.println("Log rotation error: " + e.getMessage());
        }
    }

    public static void openLog() throws IOException {
        logWriter = new BufferedWriter(new FileWriter("server_log0.txt", false));
        sessionStartTime = System.currentTimeMillis();
        info("=== Log session started ===");
    }

    public static void setLogLevel(LogLevel level) {
        minLevel = level;
        info("Log level set to " + level.getLabel());
    }

    // Main logging method with level support
    public static void log(LogLevel level, String msg) {
        if (!level.isEnabled(minLevel)) {
            return; // Skip if below minimum level
        }
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        String logMsg = String.format("[%s] [%5s] [+%dms] %s", 
            timestamp, level.getLabel(), elapsed, msg);
        
        System.out.println(logMsg);
        if (logWriter != null) {
            try {
                logWriter.write(logMsg);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("Failed to write log: " + e.getMessage());
            }
        }
    }

    // Legacy method for backward compatibility
    public static void log(String msg) {
        log(LogLevel.INFO, msg);
    }

    // Convenience methods for different log levels
    public static void debug(String msg) {
        log(LogLevel.DEBUG, msg);
    }

    public static void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public static void warn(String msg) {
        log(LogLevel.WARN, msg);
    }

    public static void error(String msg) {
        log(LogLevel.ERROR, msg);
    }

    public static void error(String msg, Throwable t) {
        error(msg + " - " + t.getClass().getSimpleName() + ": " + t.getMessage());
    }

    // Log performance metrics
    public static void logPerformance(String operation, long startTimeMs) {
        long duration = System.currentTimeMillis() - startTimeMs;
        debug(String.format("PERF: %s took %dms", operation, duration));
    }

    public static void close() {
        info("=== Log session ended ===");
        try { if (logWriter != null) logWriter.close(); } catch (IOException ignored) {}
    }
}