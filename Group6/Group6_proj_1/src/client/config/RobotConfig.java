package client.config;

// Robot configuration - all tunable constants in one place
public class RobotConfig {
    
    // Network
    public static final String SERVER_HOST = "10.0.1.8";
    public static final int SERVER_PORT = 9999;
    public static final int TICK_RATE_MS = 50;
    public static final int CONNECTION_TIMEOUT_MS = 5000;
    public static final int SOCKET_TIMEOUT_MS = 10000;
    
    // Motor speeds (deg/sec)
    public static final int DEFAULT_MOTOR_SPEED = 100;
    public static final int COMMAND_DEFAULT_SPEED = 300;
    public static final int COMMAND_TURN_SPEED = 300;
    public static final int ROTATION_SPEED = 200;
    public static final int MIN_MOTOR_SPEED = 0;
    public static final int MAX_MOTOR_SPEED = 900;
    public static final int DEFAULT_MOTOR_ACCELERATION = 6000;
    
    // Arm
    public static final int ARM_SPEED = 200;
    public static final char ARM_MOTOR_PORT = 'A';
    public static final int ARM_UP_POSITION = -90;
    public static final int ARM_DOWN_POSITION = 0;
    
    // Motor ports
    public static final char LEFT_MOTOR_PORT = 'B';
    public static final char RIGHT_MOTOR_PORT = 'C';
    public static final char[] DRIVE_MOTORS = {LEFT_MOTOR_PORT, RIGHT_MOTOR_PORT};
    public static final char[] LEFT_MOTORS = {LEFT_MOTOR_PORT};
    public static final char[] RIGHT_MOTORS = {RIGHT_MOTOR_PORT};
    
    // Robot dimensions (mm)
    public static final float WHEEL_DIAMETER_MM = 43.2f;
    public static final float TRACK_WIDTH_MM = 134.0f;
    public static final double ROTATION_MULTIPLIER = 2.0;
    
    // Sensors
    public static final int SENSOR_POLL_INTERVAL_MS = 500;
    public static final int SENSOR_SAMPLE_INTERVAL_MS = 30;
    public static final int SENSOR_FILTER_SIZE = 7;
    
    // Distances (cm)
    public static final float MAX_DETECTION_DISTANCE_CM = Float.POSITIVE_INFINITY;
    public static final float ALERT_DISTANCE_CM = 50.0f;
    public static final float FINAL_DISTANCE_CM = 12.0f;
    public static final float MIN_SAFE_DISTANCE_CM = 15.0f;
    public static final float OBSTACLE_DISTANCE_CM = 50.0f;
    public static final float EDGE_THRESHOLD_CM = 3.0f;
    
    // Ball detection
    public static final int SCAN_ANGLE_DEGREES = 90;
    public static final int SCAN_SPEED_DEG_PER_SEC = 20;
    public static int TARGET_BALL_COLOR_ID = 5; // Red (can be changed at runtime)
    public static final int COLOR_VERIFY_ATTEMPTS = 5;
    public static final float SMALL_OBJECT_THRESHOLD_CM = 15.0f;
    public static final int COLOR_MATCH_THRESHOLD_PERCENT = 40;
    
    // Autonomous search
    public static final int SEARCH_MOTOR_SPEED = 100;
    public static final int SEARCH_DISTANCE_CM = 100;
    public static final int SEARCH_TURN_ANGLE_DEGREES = 45;
    public static final int SEARCH_SCAN_INTERVAL_MS = 100;
    public static final int SWEEP_ANGLE_DEGREES = 90;
    public static final int SWEEP_SPEED_DEG_PER_SEC = 30;
    public static final int MAX_FORWARD_STEPS = 3;
    
    // Commands
    public static final int COMMAND_BEEP_INTERVAL_MS = 200;
    public static final int COMMAND_MAX_BEEP_COUNT = 5;
    
    // ========== DISPLAY SETTINGS ==========
    public static final int LCD_MAX_WIDTH = 18; // LCD characters per line
    public static final int LCD_COMMAND_LINE = 1; // Line number for displaying commands
    public static final int LCD_STATUS_LINE = 0; // Line number for status messages
    
    // ========== SYSTEM CONFIGURATION ==========
    public static final boolean DEBUG = false;
    public static final int THREAD_JOIN_TIMEOUT_MS = 1000;
    public static final int BUTTON_POLL_INTERVAL_MS = 100;
    
    // ========== LEGACY COMMAND LIMITS ==========
    // (Kept for backward compatibility - prefer COMMAND_MAX_BEEP_COUNT)
    public static final int MIN_BEEP_COUNT = 1;
    public static final int MAX_BEEP_COUNT = 10;
    public static final int BEEP_DELAY_MS = 200;
    
    public static final int MIN_LOG_COUNT = 1;
    public static final int MAX_LOG_COUNT = 100;
    public static final int MIN_LOG_INTERVAL_MS = 100;
    public static final int MAX_LOG_INTERVAL_MS = 10000;
    
    // Prevent instantiation
    private RobotConfig() {
        throw new AssertionError("Cannot instantiate RobotConfig");
    }
}
