package client;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Client-side logging utility with local display and optional server forwarding.
 */
public class ClientLogger {
    private static boolean localLoggingEnabled = true;
    private static boolean serverLoggingEnabled = false;
    private static CommandHandler context = null;
    
    public static void init(CommandHandler handler, boolean enableServerLogs) {
        context = handler;
        serverLoggingEnabled = enableServerLogs;
        info("Client logger initialized");
    }
    
    public static void setServerLogging(boolean enabled) {
        serverLoggingEnabled = enabled;
        info("Server logging " + (enabled ? "enabled" : "disabled"));
    }
    
    public static void setLocalLogging(boolean enabled) {
        localLoggingEnabled = enabled;
    }
    
    // Log with level
    private static void log(String level, String msg) {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String logMsg = String.format("[%s] [%s] %s", timestamp, level, msg);
        
        // Always display locally on EV3
        if (localLoggingEnabled) {
            System.out.println(logMsg);
        }
        
        // Optionally send to server
        if (serverLoggingEnabled && context != null) {
            context.sendLog(logMsg);
        }
    }
    
    public static void debug(String msg) {
        log("DEBUG", msg);
    }
    
    public static void info(String msg) {
        log("INFO", msg);
    }
    
    public static void warn(String msg) {
        log("WARN", msg);
    }
    
    public static void error(String msg) {
        log("ERROR", msg);
    }
    
    public static void error(String msg, Exception e) {
        error(msg + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
    
    // Performance logging
    public static void logPerformance(String operation, long startTimeMs) {
        long duration = System.currentTimeMillis() - startTimeMs;
        debug(String.format("PERF: %s took %dms", operation, duration));
    }
    
    // Command execution logging
    public static void logCommand(String command, String result) {
        info(String.format("CMD: %s -> %s", command, result));
    }
    
    // Motor operation logging
    public static void logMotor(String motor, String operation, int value) {
        debug(String.format("MOTOR: %s %s %d", motor, operation, value));
    }
    
    // Sensor reading logging
    public static void logSensor(String sensor, float value) {
        debug(String.format("SENSOR: %s = %.2f", sensor, value));
    }
}
