package common;

// Protocol constants for client-server communication
public class ProtocolConstants {
    
    public static final String TICK = "TICK:";
    public static final String TICK_ACK = "TICK_ACK:";
    public static final String SENSOR = "SENSOR:";
    public static final String BATTERY = "BATTERY:";
    public static final String BATTERY_LOGGING = "BATTERY_LOGGING:";
    public static final String LOG = "LOG:";
    public static final String REPLY = "REPLY:";
    public static final String CONTROL = "CONTROL:";
    public static final String MOTOR = "MOTOR:";
    public static final String BYE = "BYE:";
    public static final String CMD_ACK = "CMD_ACK:";
    public static final String BEEP = "BEEP";
    public static final String SCAN = "SCAN";
    public static final String AUTOSEARCH = "AUTOSEARCH";
    
    public static final String MOVE = "MOVE";
    public static final String FWD = "FWD";
    public static final String FORWARD = "FORWARD";
    public static final String BWD = "BWD";
    public static final String BACK = "BACK";
    public static final String BACKWARD = "BACKWARD";
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String STOP = "STOP";
    public static final String ROTATE = "ROTATE";
    
    public static final String ARM = "ARM";
    public static final String ARM_UP = "UP";
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
     */
    public static String getPayload(String message, String prefix) {
        if (message != null && message.startsWith(prefix)) {
            return message.substring(prefix.length()).trim();
        }
        return "";
    }
    
    public static String buildMessage(String prefix, String payload) {
        return prefix + payload;
    }
    
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
    
    public static String buildSensorMessage(SensorData sensorData) {
        if (sensorData == null || sensorData.isEmpty()) {
            return SENSOR;
        }
        return SENSOR + sensorData.toString();
    }
    
    public static String buildSensorMessage(java.util.Map<String, Float> readings) {
        return buildSensorMessage(new SensorData(readings));
    }
    
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
    
    public static String buildTickMessage(int frameNumber) {
        return TICK + frameNumber;
    }
    
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
    
    public static String buildTickAckMessage(int frameNumber) {
        return TICK_ACK + frameNumber;
    }
    
    /**
     * Parse READY: message and extract frame number.
     * Format: READY:frameNumber
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
    
    public static String buildReadyMessage(int frameNumber) {
        return READY + frameNumber;
    }
    
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
    
    public static String buildCmdAckMessage(int frameNumber) {
        return CMD_ACK + frameNumber;
    }
    
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
    
    private ProtocolConstants() {
        throw new AssertionError("ProtocolConstants is a utility class and cannot be instantiated");
    }
}
