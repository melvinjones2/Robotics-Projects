package client;

/**
 * CONFIGURATION GUIDE for Motors and Sensors
 * ============================================
 * 
 * This class demonstrates how to configure motors and sensors for your robot.
 * Copy the examples below and modify RobotConfig.java or your initialization code.
 */
public class ConfigurationGuide {
    
    /**
     * ========================================
     * MOTOR CONFIGURATION
     * ========================================
     * 
     * Motors are configured in RobotConfig.java
     * 
     * 1. CHANGE MOTOR PORT ASSIGNMENTS:
     *    Edit these arrays in RobotConfig.java:
     *    
     *    public static final char[] DRIVE_MOTORS = {'A', 'B', 'C', 'D'};  // All drive motors
     *    public static final char[] LEFT_MOTORS = {'B', 'C'};              // Left side motors
     *    public static final char[] RIGHT_MOTORS = {'A', 'D'};             // Right side motors
     *    public static final char ARM_MOTOR_PORT = 'A';                    // Arm/special motor
     *    
     *    Example: If your left motors are on ports A & B:
     *    public static final char[] LEFT_MOTORS = {'A', 'B'};
     *    public static final char[] RIGHT_MOTORS = {'C', 'D'};
     * 
     * 2. CHANGE MOTOR SPEEDS:
     *    Adjust these values in RobotConfig.java:
     *    
     *    public static final int DEFAULT_MOTOR_SPEED = 100;       // Default speed
     *    public static final int MIN_MOTOR_SPEED = 0;             // Minimum allowed
     *    public static final int MAX_MOTOR_SPEED = 900;           // Maximum allowed
     *    public static final int ARM_SPEED = 200;                 // Arm motor speed
     *    
     * 3. CHANGE MOTOR ACCELERATION:
     *    public static final int DEFAULT_MOTOR_ACCELERATION = 200;
     *    
     *    Higher = faster acceleration (more jerky)
     *    Lower = smoother acceleration (more gradual)
     */
    
    /**
     * ========================================
     * SENSOR CONFIGURATION
     * ========================================
     * 
     * Sensors are configured in SensorFactory.java
     * 
     * 1. CHANGE DEFAULT SENSOR SETUP:
     *    Edit getDefaultSensorConfig() in SensorFactory.java:
     *    
     *    CURRENT DEFAULTS:
     *    - S1: Ultrasonic sensor (mode: "listen")
     *    - S2: Touch sensor
     *    - S3: Gyro sensor (mode: "rate")
     *    - S4: Light/Color sensor (mode: "rgb")
     *    
     *    EXAMPLE - Custom configuration:
     *    
     *    public static List<SensorConfig> getDefaultSensorConfig() {
     *        List<SensorConfig> configs = new ArrayList<>();
     *        
     *        // Ultrasonic on S1
     *        configs.add(new SensorConfig(SensorConfig.SensorType.ULTRASONIC, SensorPort.S1, "listen"));
     *        
     *        // Touch on S2
     *        configs.add(new SensorConfig(SensorConfig.SensorType.TOUCH, SensorPort.S2));
     *        
     *        // Two touch sensors if needed
     *        configs.add(new SensorConfig(SensorConfig.SensorType.TOUCH, SensorPort.S3));
     *        
     *        // Light sensor on S4 with ambient mode
     *        configs.add(new SensorConfig(SensorConfig.SensorType.LIGHT, SensorPort.S4, "ambient"));
     *        
     *        return configs;
     *    }
     * 
     * 2. SENSOR MODES:
     *    Different sensors support different modes:
     *    
     *    ULTRASONIC:
     *    - "listen" - passive listening mode (default, lower power)
     *    - null - active ping mode (more accurate)
     *    
     *    LIGHT/COLOR:
     *    - "rgb" - RGB color values (default)
     *    - "red" - red light reflection
     *    - "ambient" - ambient light sensing
     *    
     *    GYRO:
     *    - "rate" - rotation rate in degrees/sec (default)
     *    - "angle" - absolute angle in degrees
     *    
     *    TOUCH:
     *    - No modes, just pressed/not pressed
     * 
     * 3. CHANGE SENSOR POLLING RATE:
     *    Edit in RobotConfig.java:
     *    
     *    public static final int SENSOR_POLL_INTERVAL_MS = 500;  // Poll every 500ms
     *    
     *    Lower = more frequent updates (higher CPU usage)
     *    Higher = less frequent updates (lower CPU usage)
     */
    
    /**
     * ========================================
     * ASIMOV SAFETY THRESHOLDS
     * ========================================
     * 
     * Safety limits are configured in AsimovSafetyChecker.java
     * 
     * Edit these constants:
     * 
     * private static final float HUMAN_PROXIMITY_CM = 30.0f;      // Don't move if human within 30cm
     * private static final float COLLISION_IMMINENT_CM = 15.0f;   // Emergency stop distance
     * private static final float MAX_SAFE_SPEED = 600;            // Maximum safe motor speed
     * private static final float CRITICAL_BATTERY = 10.0f;        // Minimum battery percentage
     * 
     * Example - More conservative (safer):
     * private static final float HUMAN_PROXIMITY_CM = 50.0f;      // Stop earlier
     * private static final float COLLISION_IMMINENT_CM = 25.0f;   // Emergency stop sooner
     * private static final float MAX_SAFE_SPEED = 400;            // Slower max speed
     */
    
    /**
     * ========================================
     * NETWORK CONFIGURATION
     * ========================================
     * 
     * Network settings are in RobotConfig.java
     * 
     * public static final String SERVER_HOST = "10.0.1.8";        // Server IP address
     * public static final int SERVER_PORT = 9999;                 // Server port
     * public static final int TICK_RATE_MS = 50;                  // Communication rate (20 Hz)
     * 
     * CHANGE SERVER IP:
     * 1. Find your server's IP address (ipconfig on Windows, ifconfig on Mac/Linux)
     * 2. Update SERVER_HOST with that IP
     * 
     * CHANGE TICK RATE:
     * Lower TICK_RATE_MS = faster updates (more network traffic)
     * Higher TICK_RATE_MS = slower updates (less network traffic)
     */
    
    /**
     * ========================================
     * RUNTIME CONFIGURATION (No code changes)
     * ========================================
     * 
     * Some things can be configured at runtime via commands:
     * 
     * 1. DEBUG MODE:
     *    Command: SET_DEBUG 1    (enable)
     *    Command: SET_DEBUG 0    (disable)
     * 
     * 2. AUTONOMOUS MODE:
     *    Command: AUTO on        (enable client autonomous)
     *    Command: AUTO off       (disable client autonomous)
     *    
     *    Server GUI: "Enable Server Auto" button (enable server autonomous)
     * 
     * 3. SENSOR ANALYSIS:
     *    Command: ANALYZE        (get full sensor analysis)
     *    Command: NAV_SUGGEST    (get navigation suggestion)
     *    Command: SENSOR_STATS ultrasonic 10  (get stats for specific sensor)
     */
    
    /**
     * ========================================
     * EXAMPLE: Custom Robot Configuration
     * ========================================
     * 
     * Let's say you have a robot with:
     * - 2 motors: left on B, right on C
     * - 1 ultrasonic sensor on S1
     * - 1 touch sensor on S2
     * - No gyro or light sensors
     * 
     * STEP 1: Update RobotConfig.java motor ports:
     * 
     * public static final char[] DRIVE_MOTORS = {'B', 'C'};
     * public static final char[] LEFT_MOTORS = {'B'};
     * public static final char[] RIGHT_MOTORS = {'C'};
     * 
     * STEP 2: Update SensorFactory.getDefaultSensorConfig():
     * 
     * public static List<SensorConfig> getDefaultSensorConfig() {
     *     List<SensorConfig> configs = new ArrayList<>();
     *     configs.add(new SensorConfig(SensorConfig.SensorType.ULTRASONIC, SensorPort.S1, "listen"));
     *     configs.add(new SensorConfig(SensorConfig.SensorType.TOUCH, SensorPort.S2));
     *     return configs;
     * }
     * 
     * STEP 3: (Optional) Adjust safety thresholds in AsimovSafetyChecker.java
     * 
     * That's it! Your robot is now configured.
     */
    
    /**
     * ========================================
     * QUICK REFERENCE: File Locations
     * ========================================
     * 
     * Motor Configuration:     src/client/RobotConfig.java (lines 24-49)
     * Sensor Configuration:    src/client/SensorFactory.java (getDefaultSensorConfig method)
     * Safety Thresholds:       src/client/AsimovSafetyChecker.java (lines 18-21)
     * Network Settings:        src/client/RobotConfig.java (lines 6-16)
     * Server IP:               src/client/RobotConfig.java (line 7)
     */
    
    // Prevent instantiation
    private ConfigurationGuide() {
        throw new AssertionError("This is a documentation class, not meant to be instantiated");
    }
}
