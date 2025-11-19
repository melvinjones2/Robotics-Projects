package common;

/**
 * Protocol constants for client-server communication.
 * Defines all message prefixes, commands, and protocol-related values.
 * 
 * Import this file in any class that needs to work with the protocol:
 *   import static common.ProtocolConstants.*;
 */
public class ProtocolConstants {
    
    // ========== MESSAGE PREFIXES ==========
    
    /** Heartbeat tick from client: TICK:frameNumber */
    public static final String TICK = "TICK:";
    
    /** Tick acknowledgment from server: TICK_ACK:frameNumber */
    public static final String TICK_ACK = "TICK_ACK:";
    
    /** Sensor data from client: SENSOR:sensor1=value1,sensor2=value2,... */
    public static final String SENSOR = "SENSOR:";
    
    /** Battery status: BATTERY:8000mV, Voltage:8.0V, Battery Current:500mA, Motor Current:200mA */
    public static final String BATTERY = "BATTERY:";
    
    /** Battery logging with extended data: BATTERY_LOGGING:... */
    public static final String BATTERY_LOGGING = "BATTERY_LOGGING:";
    
    /** Log message from client: LOG:message */
    public static final String LOG = "LOG:";
    
    /** Reply/acknowledgment: REPLY:message */
    public static final String REPLY = "REPLY:";
    
    /** Control status update: CONTROL:message */
    public static final String CONTROL = "CONTROL:";
    
    /** Motor status update: MOTOR:message */
    public static final String MOTOR = "MOTOR:";
    
    /** Disconnect: BYE: */
    public static final String BYE = "BYE:";
    
    /** Command acknowledgment: CMD_ACK:frameNumber */
    public static final String CMD_ACK = "CMD_ACK:";
    
    /** Beep sound: BEEP [count] */
    public static final String BEEP = "BEEP";
    
    /** Scan result: SCAN:FOUND or SCAN:NOT_FOUND or SCAN:ERROR */
    public static final String SCAN = "SCAN";
    
    /** Auto search status: AUTOSEARCH:ON or AUTOSEARCH:OFF */
    public static final String AUTOSEARCH = "AUTOSEARCH";
    
    // ========== MOVEMENT COMMANDS ==========
    
    /** Move forward: MOVE [speed] or MOVE [motor] [speed] */
    public static final String MOVE = "MOVE";
    
    /** Alias for MOVE */
    public static final String FWD = "FWD";
    
    /** Alias for MOVE */
    public static final String FORWARD = "FORWARD";
    
    /** Move backward: BWD [speed] or BACK [speed] */
    public static final String BWD = "BWD";
    
    /** Alias for BWD */
    public static final String BACK = "BACK";
    
    /** Alias for BWD */
    public static final String BACKWARD = "BACKWARD";
    
    /** Turn left: LEFT [speed] */
    public static final String LEFT = "LEFT";
    
    /** Turn right: RIGHT [speed] */
    public static final String RIGHT = "RIGHT";
    
    /** Stop motors: STOP [motor] or STOP (all) */
    public static final String STOP = "STOP";
    
    /** Rotate robot or motor: ROTATE ROBOT degrees or ROTATE motor degrees */
    public static final String ROTATE = "ROTATE";
    
    // ========== ARM COMMANDS ==========
    
    /** Arm control: ARM UP or ARM DOWN or ARM degrees */
    public static final String ARM = "ARM";
    
    /** Arm up position */
    public static final String ARM_UP = "UP";
    
    /** Arm down position */
    public static final String ARM_DOWN = "DOWN";
    
    // ========== AUTONOMOUS COMMANDS ==========
    
    /** Set target ball color: SETCOLOR colorId (1-6) */
    public static final String SETCOLOR = "SETCOLOR";
    
    // ========== HANDSHAKE ==========
    
    /** Server greeting: HELLO */
    public static final String HELLO = "HELLO";
    
    /** Client ready response: READY:frameNumber */
    public static final String READY = "READY:";
    
    // ========== SCAN RESULTS ==========
    
    /** Scan found ball */
    public static final String SCAN_FOUND = "FOUND";
    
    /** Scan did not find ball */
    public static final String SCAN_NOT_FOUND = "NOT_FOUND";
    
    /** Scan error */
    public static final String SCAN_ERROR = "ERROR";
    
    /** No sensors available */
    public static final String SCAN_NO_SENSORS = "NO_SENSORS";
    
    // ========== AUTOSEARCH STATES ==========
    
    /** Auto search enabled */
    public static final String AUTOSEARCH_ON = "ON";
    
    /** Auto search disabled */
    public static final String AUTOSEARCH_OFF = "OFF";
    
    /** Toggle auto search */
    public static final String AUTOSEARCH_TOGGLE = "TOGGLE";
    
    // ========== COLOR IDs (for SETCOLOR command) ==========
    
    /** Black ball */
    public static final int COLOR_BLACK = 1;
    
    /** Blue ball */
    public static final int COLOR_BLUE = 2;
    
    /** Green ball */
    public static final int COLOR_GREEN = 3;
    
    /** Yellow ball */
    public static final int COLOR_YELLOW = 4;
    
    /** Red ball */
    public static final int COLOR_RED = 5;
    
    /** White ball */
    public static final int COLOR_WHITE = 6;
    
    // ========== HELPER METHODS ==========
    
    /**
     * Extract payload from a message by removing its prefix.
     * 
     * @param message The full message string
     * @param prefix The prefix to remove (e.g., "SENSOR:")
     * @return The message payload without prefix, trimmed
     */
    public static String getPayload(String message, String prefix) {
        if (message != null && message.startsWith(prefix)) {
            return message.substring(prefix.length()).trim();
        }
        return "";
    }
    
    /**
     * Build a message with prefix and payload.
     * 
     * @param prefix The message prefix (e.g., "LOG:")
     * @param payload The message content
     * @return Complete message string
     */
    public static String buildMessage(String prefix, String payload) {
        return prefix + payload;
    }
    
    // ========== TYPE-SAFE MESSAGE PARSING ==========
    
    /**
     * Parse a command string into a ParsedCommand object.
     * 
     * @param commandLine The command line (e.g., "MOVE 200" or "ROTATE ROBOT 90")
     * @return ParsedCommand object with command name and arguments
     */
    public static ParsedCommand parseCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return new ParsedCommand("", new String[0]);
        }
        
        String[] parts = commandLine.trim().split("\\s+");
        String command = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        return new ParsedCommand(command, args);
    }
    
    /**
     * Parse a SENSOR: message into SensorData object.
     * Format: SENSOR:sensor1=value1,sensor2=value2,...
     * 
     * @param message The SENSOR: message
     * @return SensorData with all sensor readings, or empty if invalid
     */
    public static SensorData parseSensorMessage(String message) {
        SensorData data = new SensorData();
        
        if (message == null || !message.startsWith(SENSOR)) {
            return data;
        }
        
        String payload = getPayload(message, SENSOR);
        if (payload.isEmpty()) {
            return data;
        }
        
        String[] sensors = payload.split(",");
        for (String sensor : sensors) {
            String[] parts = sensor.split("=");
            if (parts.length == 2) {
                try {
                    String name = parts[0].trim();
                    float value = Float.parseFloat(parts[1].trim());
                    data.put(name, value);
                } catch (NumberFormatException e) {
                    // Ignore invalid sensor data
                }
            }
        }
        
        return data;
    }
    
    /**
     * Build a SENSOR: message from SensorData.
     * Format: SENSOR:sensor1=value1,sensor2=value2,...
     * 
     * @param sensorData The sensor data to encode
     * @return SENSOR: message string
     */
    public static String buildSensorMessage(SensorData sensorData) {
        if (sensorData == null || sensorData.isEmpty()) {
            return SENSOR;
        }
        return SENSOR + sensorData.toString();
    }
    
    /**
     * Build a SENSOR: message from individual readings.
     * Format: SENSOR:sensor1=value1,sensor2=value2,...
     * 
     * @param readings Map of sensor names to values
     * @return SENSOR: message string
     */
    public static String buildSensorMessage(java.util.Map<String, Float> readings) {
        return buildSensorMessage(new SensorData(readings));
    }
    
    /**
     * Parse TICK: message and extract frame number.
     * Format: TICK:frameNumber
     * 
     * @param message The TICK: message
     * @return Frame number or -1 if invalid
     */
    public static int parseTickMessage(String message) {
        if (message == null || !message.startsWith(TICK)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(getPayload(message, TICK));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Build a TICK: message.
     * Format: TICK:frameNumber
     * 
     * @param frameNumber The frame number
     * @return TICK: message string
     */
    public static String buildTickMessage(int frameNumber) {
        return TICK + frameNumber;
    }
    
    /**
     * Parse TICK_ACK: message and extract frame number.
     * Format: TICK_ACK:frameNumber
     * 
     * @param message The TICK_ACK: message
     * @return Frame number or -1 if invalid
     */
    public static int parseTickAckMessage(String message) {
        if (message == null || !message.startsWith(TICK_ACK)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(getPayload(message, TICK_ACK));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Build a TICK_ACK: message.
     * Format: TICK_ACK:frameNumber
     * 
     * @param frameNumber The frame number
     * @return TICK_ACK: message string
     */
    public static String buildTickAckMessage(int frameNumber) {
        return TICK_ACK + frameNumber;
    }
    
    /**
     * Parse READY: message and extract frame number.
     * Format: READY:frameNumber
     * 
     * @param message The READY: message
     * @return Frame number or -1 if invalid
     */
    public static int parseReadyMessage(String message) {
        if (message == null || !message.startsWith(READY)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(getPayload(message, READY));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Build a READY: message.
     * Format: READY:frameNumber
     * 
     * @param frameNumber The frame number
     * @return READY: message string
     */
    public static String buildReadyMessage(int frameNumber) {
        return READY + frameNumber;
    }
    
    /**
     * Parse CMD_ACK: message and extract frame number.
     * Format: CMD_ACK:frameNumber
     * 
     * @param message The CMD_ACK: message
     * @return Frame number or -1 if invalid
     */
    public static int parseCmdAckMessage(String message) {
        if (message == null || !message.startsWith(CMD_ACK)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(getPayload(message, CMD_ACK));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Build a CMD_ACK: message.
     * Format: CMD_ACK:frameNumber
     * 
     * @param frameNumber The frame number
     * @return CMD_ACK: message string
     */
    public static String buildCmdAckMessage(int frameNumber) {
        return CMD_ACK + frameNumber;
    }
    
    /**
     * Build a simple command message (e.g., "MOVE 200", "STOP", "BEEP 3").
     * 
     * @param command Command name
     * @param args Optional command arguments
     * @return Command string
     */
    public static String buildCommand(String command, String... args) {
        if (args == null || args.length == 0) {
            return command;
        }
        
        StringBuilder sb = new StringBuilder(command);
        for (String arg : args) {
            sb.append(' ').append(arg);
        }
        return sb.toString();
    }
    
    // Prevent instantiation
    private ProtocolConstants() {
        throw new AssertionError("ProtocolConstants is a utility class and cannot be instantiated");
    }
}
