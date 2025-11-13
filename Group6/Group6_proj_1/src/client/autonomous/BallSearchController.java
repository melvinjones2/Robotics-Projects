package client.autonomous;

import client.config.RobotConfig;
import client.motor.MotorFactory;
import client.sensor.ISensor;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.utility.Delay;

// Autonomous ball search - explores environment and approaches detected balls
public class BallSearchController {
    
    private boolean enabled = false;
    private boolean running = false;
    private Thread searchThread = null;
    
    private ISensor ultrasonicSensor;
    private ISensor infraredSensor;
    private ISensor gyroSensor;
    private ISensor colorSensor;
    private BallDetector ballDetector;
    private BaseRegulatedMotor leftMotor;
    private BaseRegulatedMotor rightMotor;
    
    private int ballsFound = 0;
    private int scanAttempts = 0;
    
    public BallSearchController(ISensor ultrasonicSensor, ISensor infraredSensor, ISensor gyroSensor, ISensor colorSensor, java.io.BufferedWriter out) {
        this.ultrasonicSensor = ultrasonicSensor;
        this.infraredSensor = infraredSensor;
        this.gyroSensor = gyroSensor;
        this.colorSensor = colorSensor;
        
        // Initialize ball detector with infrared support and output stream for server logging
        this.ballDetector = new BallDetector(
            ultrasonicSensor != null ? ultrasonicSensor : infraredSensor, 
            infraredSensor,
            gyroSensor, 
            colorSensor,
            out
        );
        
        // Get motors
        char[] driveMotors = RobotConfig.DRIVE_MOTORS;
        if (driveMotors != null && driveMotors.length >= 2) {
            this.leftMotor = MotorFactory.getMotor(driveMotors[0]);
            this.rightMotor = MotorFactory.getMotor(driveMotors[1]);
        }
    }
    
    /**
     * Enable/disable autonomous search mode.
     * 
     * @param enabled true to start searching, false to stop
     */
    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return; // No change
        }
        
        this.enabled = enabled;
        
        if (enabled) {
            startSearch();
        } else {
            stopSearch();
        }
    }
    
    public synchronized void toggle() {
        setEnabled(!enabled);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getBallsFound() {
        return ballsFound;
    }
    
    public int getScanAttempts() {
        return scanAttempts;
    }
    
    private void startSearch() {
        if (running) {
            return;
        }
        
        if (!isSystemReady()) {
            LCD.clear(1);
            LCD.drawString("AUTOSEARCH: No Dist", 0, 1);
            Sound.buzz();
            enabled = false;
            return;
        }
        
        ballsFound = 0;
        scanAttempts = 0;
        
        running = true;
        searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                searchLoop();
            }
        }, "ball-search");
        searchThread.start();
        
        LCD.clear(1);
        LCD.drawString("AUTOSEARCH: ON", 0, 1);
        Sound.beep();
    }
    
    private void stopSearch() {
        running = false;
        
        // Stop ball detector if running
        if (ballDetector != null) {
            ballDetector.stop();
        }
        
        // Wait for thread to finish
        if (searchThread != null) {
            try {
                searchThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            searchThread = null;
        }
        
        stopMotors();
        
        LCD.clear(1);
        LCD.drawString("AUTOSEARCH: OFF", 0, 1);
        LCD.drawString("Found: " + ballsFound, 0, 2);
        Sound.twoBeeps();
    }
    
    private void searchLoop() {
        int forwardSteps = 0;
        
        while (running && !Button.ESCAPE.isDown()) {
            try {
                LCD.clear(1);
                LCD.drawString("SEARCHING...", 0, 1);
                LCD.drawString("Found:" + ballsFound, 0, 2);
                
                float distanceAhead = readDistance();
                if (distanceAhead > 0 && distanceAhead < RobotConfig.MIN_SAFE_DISTANCE_CM) {
                    handleTooClose();
                    forwardSteps = 0;
                    continue;
                }
                
                SweepResult sweepResult = performSweep();
                
                if (sweepResult.objectFound) {
                    LCD.clear(1);
                    LCD.drawString("Obj at " + sweepResult.angle + "deg", 0, 1);
                    LCD.drawString("Dist:" + (int)sweepResult.distance + "cm", 0, 2);
                    
                    rotate(sweepResult.angle);
                    Delay.msDelay(300);
                    
                    handleObjectDetected(sweepResult.distance);
                    forwardSteps = 0;
                    
                } else {
                    LCD.clear(1);
                    LCD.drawString("No object", 0, 1);
                    LCD.drawString("Moving forward", 0, 2);
                    
                    if (forwardSteps >= RobotConfig.MAX_FORWARD_STEPS) {
                        handleNoObjectFound();
                        forwardSteps = 0;
                    } else {
                        moveForward((int) RobotConfig.SEARCH_MOTOR_SPEED);
                        Delay.msDelay(800);
                        stopMotors();
                        
                        forwardSteps++;
                        Delay.msDelay(300);
                    }
                }
                
            } catch (Exception e) {
                LCD.clear(1);
                LCD.drawString("SEARCH ERR", 0, 1);
                Delay.msDelay(1000);
            }
        }
        
        stopMotors();
        running = false;
    }
    
    private SweepResult performSweep() {
        LCD.clear(1);
        LCD.drawString("Sweeping...", 0, 1);
        
        SweepResult result = new SweepResult();
        result.objectFound = false;
        result.distance = Float.MAX_VALUE;
        result.angle = 0;
        
        // Sweep parameters from config
        int sweepAngle = RobotConfig.SWEEP_ANGLE_DEGREES; // Total sweep range
        int sweepSpeed = RobotConfig.SWEEP_SPEED_DEG_PER_SEC; // Degrees per second
        int currentAngle = -sweepAngle / 2; // Start at left
        
        // Rotate to starting position (left)
        rotate(currentAngle);
        Delay.msDelay(300);
        
        // Start sweeping right
        rotateAtSpeed(sweepAngle, sweepSpeed, true); // Non-blocking rotation
        
        // Sample while rotating
        int samples = 0;
        while (isRotating() && running && !Button.ESCAPE.isDown()) {
            float distance = readDistance();
            
            // Track closest valid object
            if (distance > 0 && distance < RobotConfig.OBSTACLE_DISTANCE_CM) {
                if (distance < result.distance) {
                    result.distance = distance;
                    result.angle = currentAngle;
                    result.objectFound = true;
                }
            }
            
            // Update current angle estimate
            currentAngle += sweepSpeed * RobotConfig.SEARCH_SCAN_INTERVAL_MS / 1000;
            samples++;
            
            Delay.msDelay(RobotConfig.SEARCH_SCAN_INTERVAL_MS);
        }
        
        stopMotors();
        
        // Return to center position if no object found
        if (!result.objectFound) {
            rotate(-sweepAngle / 2); // Return to center
            Delay.msDelay(300);
        } else {
            // Rotate back to where we saw the object
            int angleToReturn = result.angle - currentAngle;
            rotate(angleToReturn);
            Delay.msDelay(300);
        }
        
        LCD.clear(1);
        if (result.objectFound) {
            LCD.drawString("Found at " + result.angle + "d", 0, 1);
        } else {
            LCD.drawString("Nothing found", 0, 1);
        }
        
        return result;
    }
    
    private class SweepResult {
        boolean objectFound;
        float distance;
        int angle;
    }
    
    private void handleTooClose() {
        LCD.clear(1);
        LCD.drawString("Too close!", 0, 1);
        
        stopMotors();
        Sound.buzz();
        
        moveBackward((int) RobotConfig.SEARCH_MOTOR_SPEED);
        Delay.msDelay(1000);
        
        stopMotors();
        
        int turnDirection = (Math.random() < 0.5) ? -1 : 1;
        rotate(RobotConfig.SEARCH_TURN_ANGLE_DEGREES * 2 * turnDirection);
        
        Delay.msDelay(500);
    }
    
    private void handleObjectDetected(float distance) {
        LCD.clear(1);
        LCD.drawString("Object at " + (int)distance + "cm", 0, 1);
        Sound.beep();
        
        stopMotors();
        scanAttempts++;
        
        boolean foundBall = ballDetector.searchAndApproachBall();
        
        if (foundBall) {
            ballsFound++;
            LCD.clear(1);
            LCD.drawString("BALL #" + ballsFound + " FOUND!", 0, 1);
            Sound.twoBeeps();
            
            for (int i = 0; i < 3; i++) {
                Sound.beep();
                Delay.msDelay(200);
            }
            
            moveBackward((int) RobotConfig.SEARCH_MOTOR_SPEED);
            Delay.msDelay(1500);
            stopMotors();
            
            rotate(RobotConfig.SEARCH_TURN_ANGLE_DEGREES * 2);
            
        } else {
            LCD.clear(1);
            LCD.drawString("Not a ball", 0, 1);
            
            rotate(RobotConfig.SEARCH_TURN_ANGLE_DEGREES);
        }
        
        Delay.msDelay(500);
    }
    
    private void handleNoObjectFound() {
        LCD.clear(1);
        LCD.drawString("Turning...", 0, 1);
        
        stopMotors();
        
        // Turn to search new area
        rotate(RobotConfig.SEARCH_TURN_ANGLE_DEGREES);
        
        Delay.msDelay(500);
    }
    
    // ===== MOTOR CONTROL METHODS =====
    
    private void moveForward(int speed) {
        if (leftMotor == null || rightMotor == null) return;
        
        try {
            leftMotor.setSpeed(speed);
            rightMotor.setSpeed(speed);
            leftMotor.forward();
            rightMotor.forward();
        } catch (Exception e) {
            // Motor error
        }
    }
    
    private void moveBackward(int speed) {
        if (leftMotor == null || rightMotor == null) return;
        
        try {
            leftMotor.setSpeed(speed);
            rightMotor.setSpeed(speed);
            leftMotor.backward();
            rightMotor.backward();
        } catch (Exception e) {
            // Motor error
        }
    }
    
    private void rotate(int degrees) {
        if (leftMotor == null || rightMotor == null) return;
        
        try {
            // Simple rotation - both motors same speed opposite direction
            int rotationSpeed = 100;
            leftMotor.setSpeed(rotationSpeed);
            rightMotor.setSpeed(rotationSpeed);
            
            // Calculate rotation based on rough estimate
            // This is approximate - tune for your robot
            int motorDegrees = Math.abs(degrees) * 2; // Rough estimate
            
            if (degrees > 0) {
                // Turn right
                leftMotor.rotate(motorDegrees, true);
                rightMotor.rotate(-motorDegrees, false);
            } else {
                // Turn left
                leftMotor.rotate(-motorDegrees, true);
                rightMotor.rotate(motorDegrees, false);
            }
        } catch (Exception e) {
            // Motor error
        }
    }
    
    private void stopMotors() {
        if (leftMotor != null) {
            try {
                leftMotor.stop(true);
            } catch (Exception e) {}
        }
        if (rightMotor != null) {
            try {
                rightMotor.stop(true);
            } catch (Exception e) {}
        }
    }
    
    /**
     * Rotate at a specific speed (non-blocking or blocking).
     * Used for sweeping search pattern.
     * 
     * @param degrees Target angle in degrees (positive=right, negative=left)
     * @param degreesPerSec Rotation speed in degrees per second
     * @param nonBlocking If true, returns immediately; if false, waits for completion
     */
    private void rotateAtSpeed(int degrees, int degreesPerSec, boolean nonBlocking) {
        if (leftMotor == null || rightMotor == null) return;
        
        try {
            // Set motor speed based on degrees per second
            int motorSpeed = Math.abs(degreesPerSec) * 2; // Rough conversion
            leftMotor.setSpeed(motorSpeed);
            rightMotor.setSpeed(motorSpeed);
            
            // Calculate motor degrees (tune ROTATION_MULTIPLIER in RobotConfig if needed)
            int motorDegrees = (int) Math.abs(degrees * RobotConfig.ROTATION_MULTIPLIER);
            
            if (degrees > 0) {
                // Turn right: left forward, right backward
                leftMotor.rotate(motorDegrees, true);
                rightMotor.rotate(-motorDegrees, nonBlocking);
            } else {
                // Turn left: left backward, right forward
                leftMotor.rotate(-motorDegrees, true);
                rightMotor.rotate(motorDegrees, nonBlocking);
            }
        } catch (Exception e) {
            // Motor error
        }
    }
    
    /**
     * Check if motors are still rotating.
     * Used during sweep to sample sensors while rotating.
     * 
     * @return true if either motor is still moving
     */
    private boolean isRotating() {
        try {
            if (leftMotor != null && leftMotor.isMoving()) return true;
            if (rightMotor != null && rightMotor.isMoving()) return true;
        } catch (Exception e) {
            // Motor error
        }
        return false;
    }
    
    // ===== SENSOR METHODS =====
    
    /**
     * Read distance from ultrasonic or infrared sensor.
     * Prefers ultrasonic, falls back to infrared if ultrasonic not available.
     * 
     * @return distance in cm, or -1 if no sensor available
     */
    private float readDistance() {
        // Try ultrasonic first (more accurate for close range)
        if (ultrasonicSensor != null && ultrasonicSensor.isAvailable()) {
            try {
                String value = ultrasonicSensor.readValue();
                if (value != null && value.contains("=")) {
                    return Float.parseFloat(value.split("=")[1]);
                }
            } catch (Exception e) {
                // Sensor read error, try infrared
            }
        }
        
        // Fall back to infrared sensor
        if (infraredSensor != null && infraredSensor.isAvailable()) {
            try {
                String value = infraredSensor.readValue();
                if (value != null && value.contains("=")) {
                    // Infrared returns 0-100 (percentage), treat as cm
                    return Float.parseFloat(value.split("=")[1]);
                }
            } catch (Exception e) {
                // Sensor read error
            }
        }
        
        return -1;
    }
    
    private boolean isSystemReady() {
        // Need at least one distance sensor (ultrasonic OR infrared)
        boolean hasDistanceSensor = 
            (ultrasonicSensor != null && ultrasonicSensor.isAvailable()) ||
            (infraredSensor != null && infraredSensor.isAvailable());
        
        if (!hasDistanceSensor) {
            return false;
        }
        if (leftMotor == null || rightMotor == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Get status summary string.
     */
    public String getStatusSummary() {
        return String.format("AUTOSEARCH: %s | Found: %d | Attempts: %d",
            enabled ? "ON" : "OFF", ballsFound, scanAttempts);
    }
}

