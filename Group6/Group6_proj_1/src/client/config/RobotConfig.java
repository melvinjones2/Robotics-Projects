package client.config;

// Central configuration for robot parameters
// REFER TO THIS CLASS FOR ALL CONFIGURABLE CONSTANTS
// REFER TO ConfigurationGuide.java for detailed configuration instructions
public class RobotConfig {
    
    // Network Configuration
    public static final String SERVER_HOST = "10.0.1.8";
    public static final int SERVER_PORT = 9999;
    public static final int TICK_RATE_MS = 50; // 20 ticks per second
    public static final int CONNECTION_TIMEOUT_MS = 5000;
    public static final int SOCKET_TIMEOUT_MS = 10000;
    
    // Motor Configuration
    public static final int DEFAULT_MOTOR_SPEED = 100;
    public static final int MIN_MOTOR_SPEED = 0;
    public static final int MAX_MOTOR_SPEED = 900;
    public static final int DEFAULT_MOTOR_ACCELERATION = 6000; // Maximum acceleration for fastest response
    public static final int ARM_SPEED = 200;
    public static final char ARM_MOTOR_PORT = 'A';
    
    // Sensor Configuration
    public static final int SENSOR_POLL_INTERVAL_MS = 500;
    
    // Command Limits
    public static final int MIN_BEEP_COUNT = 1;
    public static final int MAX_BEEP_COUNT = 10;
    public static final int BEEP_DELAY_MS = 200;
    
    public static final int MIN_LOG_COUNT = 1;
    public static final int MAX_LOG_COUNT = 100;
    public static final int MIN_LOG_INTERVAL_MS = 100;
    public static final int MAX_LOG_INTERVAL_MS = 10000;
    
    // System Configuration
    public static final boolean DEBUG = false;
    
    // Motor port assignments (for easy reconfiguration)
    public static final char[] DRIVE_MOTORS = {'B', 'C'}; // Only drive motors, not arm
    public static final char[] LEFT_MOTORS = {'B'};
    public static final char[] RIGHT_MOTORS = {'C'};
    
    // Prevent instantiation
    private RobotConfig() {
        throw new AssertionError("Cannot instantiate RobotConfig");
    }
}
