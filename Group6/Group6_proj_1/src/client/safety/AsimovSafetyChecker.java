package client.safety;

import client.sensor.data.SensorAnalyzer;
import client.sensor.data.SensorDataStore;

/**
 * Implements Asimov's Three Laws of Robotics as a safety layer.
 * All commands must pass through this checker before execution.
 * 
 * Three Laws:
 * 1. A robot may not injure a human being or, through inaction, allow a human being to come to harm.
 * 2. A robot must obey orders given it by human beings except where such orders would conflict with the First Law.
 * 3. A robot must protect its own existence as long as such protection does not conflict with the First or Second Laws.
 */
public class AsimovSafetyChecker {
    
    private final SensorDataStore dataStore;
    private final SensorAnalyzer analyzer;
    
    // Safety thresholds
    private static final float HUMAN_PROXIMITY_CM = 30.0f; // Don't move if human within 30cm
    private static final float COLLISION_IMMINENT_CM = 15.0f; // Emergency stop distance
    private static final float MAX_SAFE_SPEED = 600; // Maximum safe motor speed
    private static final float CRITICAL_BATTERY = 10.0f; // Minimum battery to preserve self
    
    public AsimovSafetyChecker(SensorDataStore dataStore) {
        this.dataStore = dataStore;
        this.analyzer = new SensorAnalyzer(dataStore);
    }
    
    /**
     * Check if a command violates any of Asimov's Three Laws.
     * Returns a SafetyViolation if the command should be blocked, null if safe.
     */
    public SafetyViolation checkCommand(String command, String[] args) {
        if (command == null) {
            return null;
        }
        
        String cmd = command.toUpperCase();
        
        // FIRST LAW: Don't harm humans
        SafetyViolation firstLawViolation = checkFirstLaw(cmd, args);
        if (firstLawViolation != null) {
            return firstLawViolation;
        }
        
        // SECOND LAW: Obey orders (unless conflicts with First Law - already checked)
        // Second law is satisfied by executing the command if First Law passes
        
        // THIRD LAW: Self-preservation (unless conflicts with First or Second Law)
        SafetyViolation thirdLawViolation = checkThirdLaw(cmd, args);
        if (thirdLawViolation != null) {
            return thirdLawViolation;
        }
        
        return null; // Command is safe
    }
    
    /**
     * First Law: A robot may not injure a human being or allow a human to come to harm.
     */
    private SafetyViolation checkFirstLaw(String cmd, String[] args) {
        // Check for human proximity using touch sensor
        if (analyzer.isTouched()) {
            // Touch sensor activated - possible human contact
            if (isMovementCommand(cmd)) {
                return new SafetyViolation(
                    1,
                    "First Law violation: Touch sensor activated. Possible human contact.",
                    "STOP immediately to prevent harm"
                );
            }
        }
        
        // Check for obstacle that could be a human
        Float distance = dataStore.getLatest("ultrasonic");
        if (distance != null && distance < HUMAN_PROXIMITY_CM) {
            if (isForwardMovement(cmd)) {
                return new SafetyViolation(
                    1,
                    "First Law violation: Obstacle detected at " + distance + "cm. Possible human.",
                    "STOP or change direction to avoid potential harm"
                );
            }
        }
        
        // Check for collision imminent
        if (distance != null && distance < COLLISION_IMMINENT_CM) {
            if (isMovementCommand(cmd) && !cmd.equals("STOP")) {
                return new SafetyViolation(
                    1,
                    "First Law violation: Collision imminent at " + distance + "cm.",
                    "Emergency STOP required"
                );
            }
        }
        
        // Check for excessive speed that could cause harm
        if (isMovementCommand(cmd) && args.length > 1) {
            try {
                int speed = Integer.parseInt(args[1]);
                if (speed > MAX_SAFE_SPEED) {
                    return new SafetyViolation(
                        1,
                        "First Law violation: Speed " + speed + " exceeds safe limit of " + MAX_SAFE_SPEED,
                        "Reduce speed to safe levels"
                    );
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
        
        return null; // No First Law violation
    }
    
    /**
     * Third Law: A robot must protect its own existence.
     * (Only enforced if it doesn't conflict with First or Second Law)
     */
    private SafetyViolation checkThirdLaw(String cmd, String[] args) {
        // Note: Third Law violations are warnings, not hard blocks
        // The command can still proceed if ordered by a human (Second Law)
        
        // Check battery level for self-preservation
        Float battery = dataStore.getLatest("battery");
        if (battery != null && battery < CRITICAL_BATTERY) {
            if (isMovementCommand(cmd)) {
                return new SafetyViolation(
                    3,
                    "Third Law warning: Battery critically low at " + battery + "%",
                    "Consider conserving energy for self-preservation"
                );
            }
        }
        
        // Check for dangerous tilt angles
        Float gyro = dataStore.getLatest("gyro");
        if (gyro != null && Math.abs(gyro) > 30) {
            if (isMovementCommand(cmd)) {
                return new SafetyViolation(
                    3,
                    "Third Law warning: Dangerous tilt angle of " + gyro + " degrees",
                    "Risk of falling/damage to robot"
                );
            }
        }
        
        return null; // No Third Law violation
    }
    
    /**
     * Check if command involves movement.
     */
    private boolean isMovementCommand(String cmd) {
        return cmd.equals("MOVE") || cmd.equals("FORWARD") || cmd.equals("FWD") ||
               cmd.equals("BACKWARD") || cmd.equals("BWD") || cmd.equals("BACK") ||
               cmd.equals("LEFT") || cmd.equals("TURNLEFT") ||
               cmd.equals("RIGHT") || cmd.equals("TURNRIGHT") ||
               cmd.equals("ROTATE") || cmd.equals("MOVE_AND_LOG");
    }
    
    /**
     * Check if command is forward movement (towards potential obstacle).
     */
    private boolean isForwardMovement(String cmd) {
        return cmd.equals("MOVE") || cmd.equals("FORWARD") || cmd.equals("FWD");
    }
    
    /**
     * Represents a violation of one of Asimov's Three Laws.
     */
    public static class SafetyViolation {
        public final int lawNumber; // 1, 2, or 3
        public final String reason;
        public final String recommendation;
        
        public SafetyViolation(int lawNumber, String reason, String recommendation) {
            this.lawNumber = lawNumber;
            this.reason = reason;
            this.recommendation = recommendation;
        }
        
        public boolean isHardBlock() {
            // First Law violations are hard blocks - cannot be overridden
            // Third Law violations are warnings - can be overridden by human orders
            return lawNumber == 1;
        }
        
        @Override
        public String toString() {
            String lawName = lawNumber == 1 ? "FIRST LAW" : 
                           lawNumber == 2 ? "SECOND LAW" : "THIRD LAW";
            return String.format("[%s VIOLATION] %s\nRecommendation: %s", 
                               lawName, reason, recommendation);
        }
    }
}
