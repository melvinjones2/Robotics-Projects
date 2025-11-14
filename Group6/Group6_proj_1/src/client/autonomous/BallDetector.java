package client.autonomous;

import client.config.RobotConfig;
import client.motor.DifferentialDrive;
import client.motor.MotorFactory;
import client.sensor.ISensor;
import lejos.hardware.motor.BaseRegulatedMotor;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Autonomous ball detection and approach using 360-degree scanning.
 * 
 * Strategy:
 * 1. Scan 360° in 10° steps to find closest object
 * 2. Rotate to face the object
 * 3. Drive forward with periodic sweep scans for alignment correction
 * 4. Slow down at 15cm, stop sweeping, drive straight to 5cm
 * 
 * Sensors:
 * - Ultrasonic: Long-range detection (50+ cm)
 * - Infrared: Close-range precision (<50cm), center-mounted
 */
public class BallDetector {
    
    // Configuration constants
    private static final float MAX_DISTANCE = Float.POSITIVE_INFINITY;
    private static final float STOP_DISTANCE_CM = 5.0f;
    private static final int SAMPLE_INTERVAL_MS = 40;
    private static final int SCAN_SPEED = 50;
    private static final int DRIVE_SPEED = 200;
    private static final int FILTER_SIZE = 5;
    private static final int SCAN_STEP_DEGREES = 10;
    
    // Sensors
    private final ISensor ultrasonicSensor;
    private final ISensor infraredSensor;
    
    // Motors
    private final DifferentialDrive drive;
    private final BaseRegulatedMotor armMotor;
    
    // Logging
    private final BufferedWriter out;
    private boolean running = false;
    
    // 360° scan data storage (36 positions for 10° steps)
    private final float[] scanDistances = new float[360 / SCAN_STEP_DEGREES];
    private final int[] scanAngles = new int[360 / SCAN_STEP_DEGREES];
    
    /**
     * Circular buffer for filtering noisy sensor readings.
     * Maintains running average and stores most recent filtered value.
     */
    private static class SensorBuffer {
        private final float[] buffer;
        private int index;
        private int size;
        private float average;
        private float sum;
        
        SensorBuffer() {
            buffer = new float[FILTER_SIZE];
            index = 0;
            size = 0;
            average = 0.0f;
            sum = 0.0f;
        }
        
        float getAvg() {
            return average;
        }
        
        float getLatest() {
            return (size > 0) ? buffer[index] : -1.0f;
        }
        
        void update(float value) {
            if (value < 0 || Float.isNaN(value) || Float.isInfinite(value)) {
                return;
            }
            
            if (size == FILTER_SIZE) {
                int oldestIndex = (index + 1) % FILTER_SIZE;
                sum = sum - buffer[oldestIndex] + value;
            } else {
                size++;
                sum += value;
            }
            average = sum / size;
            index = (index + 1) % FILTER_SIZE;
            buffer[index] = value;
        }
    }
    
    public BallDetector(ISensor ultrasonicSensor, ISensor infraredSensor, ISensor gyroSensor, ISensor colorSensor, BufferedWriter out) {
        this.ultrasonicSensor = ultrasonicSensor;
        this.infraredSensor = infraredSensor;
        this.out = out;
        
        this.drive = new DifferentialDrive();
        
        this.armMotor = MotorFactory.getMotor(RobotConfig.ARM_MOTOR_PORT);
        if (this.armMotor != null) {
            this.armMotor.setSpeed(RobotConfig.ARM_SPEED);
        }
    }
    
    /**
     * Main entry point for ball detection and approach.
     * 
     * @return true if ball was successfully reached, false otherwise
     */
    public boolean searchAndApproachBall() {
        if (!isSystemReady()) {
            log("System not ready");
            return false;
        }
        
        running = true;
        log("========== BALL DETECTION START ==========");
        raiseArm();
        
        try {
            // Step 1: Perform 360 scan to find closest object
            int turnAngle = scan360ToFindBall();
            
            if (turnAngle == Integer.MAX_VALUE) {
                log("Could not locate ball in 360 scan");
                return false;
            }
            
            log("Turning " + turnAngle + " degrees to face ball");
            
            // Step 2: Turn to face ball using rotation command
            drive.rotateDegrees(turnAngle, SCAN_SPEED);
            drive.stop();  // Ensure motors stop
            sleep(200);
            
            // Step 3: Approach ball with continuous correction scans
            boolean reached = approachBall();
            
            if (reached) {
                log("========== BALL REACHED ==========");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log("Error: " + e.getMessage());
            return false;
        } finally {
            running = false;
            drive.stop();
            lowerArm();
        }
    }
    
    /**
     * Performs 360° scan in 10° increments to locate closest object.
     * 
     * @return angle to rotate back to face closest object, or Integer.MAX_VALUE if none found
     */
    private int scan360ToFindBall() {
        log("Starting 360 degree scan...");
        
        int scanIndex = 0;
        int totalRotation = 0;
        
        // Scan full 360 degrees in 10-degree steps (36 readings total)
        while (totalRotation < 360 && running) {
            // Rotate by step amount using your rotation command
            drive.rotateDegrees(SCAN_STEP_DEGREES, SCAN_SPEED);
            drive.stop();  // Ensure motors stop after rotate
            totalRotation += SCAN_STEP_DEGREES;
            
            // Wait for rotation to complete and robot to settle
            sleep(100);
            
            // Take multiple distance readings at this angle
            SensorBuffer buffer = new SensorBuffer();
            for (int i = 0; i < 3; i++) {
                float dist = readBestDistance();
                buffer.update(dist);
                sleep(SAMPLE_INTERVAL_MS);
            }
            
            // Store the reading for this angle
            scanDistances[scanIndex] = buffer.getLatest();
            scanAngles[scanIndex] = totalRotation;
            
            if (scanDistances[scanIndex] > 0 && scanDistances[scanIndex] < 100.0f) {
                log("Angle " + totalRotation + " deg: " + (int)scanDistances[scanIndex] + " cm");
            }
            
            scanIndex++;
        }
        
        // Find the closest object from all scan readings
        float bestDistance = MAX_DISTANCE;
        int bestAngleIndex = -1;
        
        for (int i = 0; i < scanIndex; i++) {
            if (scanDistances[i] > 0 && scanDistances[i] < bestDistance) {
                bestDistance = scanDistances[i];
                bestAngleIndex = i;
            }
        }
        
        if (bestAngleIndex >= 0) {
            int bestAngle = scanAngles[bestAngleIndex];
            log("Closest object: " + (int)bestDistance + "cm at " + bestAngle + " degrees");
            
            // Calculate how much to rotate back (we're at 360, need to get to bestAngle)
            int turnBack = bestAngle - 360;
            return turnBack;
        }
        
        log("No valid object found in scan");
        return Integer.MAX_VALUE;
    }
    
    /**
     * Quick sweep scan to verify alignment with ball.
     * Uses ultrasonic for far (≥50cm), IR for close (<50cm).
     * IR is center-mounted so aligning to IR = ball is centered.
     * 
     * @param sweepRange degrees to scan left/right (e.g., 45 = -45° to +45°)
     * @return correction angle applied
     */
    private int quickSweepScan(int sweepRange) {
        log("Quick sweep scan (±" + sweepRange + " deg)...");
        
        // Adjust number of positions based on sweep range
        int numPositions = (sweepRange <= 20) ? 3 : 5;  // 3 positions for small sweeps, 5 for larger
        int step = sweepRange / (numPositions / 2);  // Calculate step size
        
        float[] distances = new float[numPositions];
        int[] angles = new int[numPositions];
        
        // Build angle array: e.g., for range=20, step=10: {-20, -10, 0, 10, 20} (5 positions)
        // or for range=15, step=15: {-15, 0, 15} (3 positions)
        int angleIdx = 0;
        for (int angle = -sweepRange; angle <= sweepRange; angle += step) {
            if (angleIdx < numPositions) {
                angles[angleIdx++] = angle;
            }
        }
        // Ensure we always include 0 and the exact sweep range endpoints
        if (numPositions == 3) {
            angles[0] = -sweepRange;
            angles[1] = 0;
            angles[2] = sweepRange;
        } else if (numPositions == 5) {
            angles[0] = -sweepRange;
            angles[1] = -sweepRange / 2;
            angles[2] = 0;
            angles[3] = sweepRange / 2;
            angles[4] = sweepRange;
        }
        
        // Scan from left to right
        for (int i = 0; i < angles.length; i++) {
            if (i == 0) {
                // First rotation from center
                drive.rotateDegrees(angles[i], SCAN_SPEED);
            } else {
                // Incremental rotations
                drive.rotateDegrees(angles[i] - angles[i-1], SCAN_SPEED);
            }
            drive.stop();
            sleep(100);
            
            // Take reading - use best sensor for the range
            SensorBuffer buffer = new SensorBuffer();
            for (int j = 0; j < 3; j++) {
                float ir = readInfraredDistance();
                float us = readUltrasonicDistance();
                
                // Use ultrasonic for far (50+cm), IR for close (<50cm)
                // IR is centered and more accurate at close range
                if (us > 0 && us >= 50.0f) {
                    // Far away: use ultrasonic
                    buffer.update(us);
                } else if (ir > 0 && ir < 50.0f) {
                    // Close range: use IR (centered sensor)
                    buffer.update(ir);
                } else if (ir > 0 && ir < 100.0f) {
                    // IR available but might be close to 50cm threshold
                    buffer.update(ir);
                } else if (us > 0 && us < 100.0f) {
                    // Fallback to ultrasonic
                    buffer.update(us);
                }
                sleep(SAMPLE_INTERVAL_MS);
            }
            distances[i] = buffer.getLatest();
        }
        
        // Find closest reading - this aligns the IR sensor (center) with ball
        float bestDist = MAX_DISTANCE;
        int bestIndex = numPositions / 2;  // Default to center
        
        for (int i = 0; i < numPositions; i++) {
            if (distances[i] > 0 && distances[i] < bestDist) {
                bestDist = distances[i];
                bestIndex = i;
            }
        }
        
        // Return to best angle (currently at +sweepRange, need to get to best)
        int correction = angles[bestIndex] - sweepRange;
        drive.rotateDegrees(correction, SCAN_SPEED);
        drive.stop();
        
        log("Sweep correction: " + correction + " degrees");
        return correction;
    }
    
    /**
     * Drive forward toward ball with periodic alignment sweeps.
     * 
     * Behavior:
     * - >20cm: Sweep every 5s for alignment correction
     * - 15-20cm: Slow down to 1/3 speed, NO MORE SWEEPS
     * - ≤5cm: Stop
     * 
     * @return true if ball reached successfully
     */
    private boolean approachBall() {
        log("Approaching ball with continuous tracking...");
        
        SensorBuffer distBuffer = new SensorBuffer();
        boolean slowedDown = false;
        long lastSweepTime = System.currentTimeMillis();
        int sweepInterval = 5000;  // Sweep every 5 seconds (more time for forward movement)
        float lastValidDistance = -1;
        
        // Use DifferentialDrive move command (not manual motor control)
        drive.move(true, DRIVE_SPEED);
        
        int loopCount = 0;
        while (running) {
            float dist = readBestDistance();
            
            // Reject sudden large jumps (likely sensor error or lost target)
            if (dist > 0 && lastValidDistance > 0) {
                float distChange = Math.abs(dist - lastValidDistance);
                // If distance jumps more than 100cm, it's probably an error
                if (distChange > 100.0f) {
                    log("WARNING: Distance jump detected (" + (int)lastValidDistance + "cm -> " + (int)dist + "cm), using last valid");
                    dist = lastValidDistance;
                }
            }
            
            if (dist > 0 && dist < 200.0f) {  // Only update if reasonable
                distBuffer.update(dist);
                lastValidDistance = dist;
            }
            float avgDist = distBuffer.getLatest();
            
            // Log distance every 25 loops (about once per second)
            if (loopCount % 25 == 0) {
                log("Distance: " + (avgDist > 0 ? (int)avgDist + "cm" : "invalid"));
            }
            loopCount++;
            
            // CRITICAL: Check stop distance FIRST before anything else
            if (avgDist > 0 && avgDist <= STOP_DISTANCE_CM) {
                drive.stop();
                log("STOPPING - Ball reached at " + avgDist + "cm");
                return true;
            }
            
            // Emergency stop if raw sensor reading is too close
            if (dist > 0 && dist <= 3.0f) {
                drive.stop();
                log("EMERGENCY STOP - Too close: " + dist + "cm");
                return true;
            }
            
            // Slow down when getting close for precision - NO MORE SWEEPS after this point
            if (!slowedDown && avgDist > 0 && avgDist < 15.0f) {
                drive.stop();
                int slowSpeed = DRIVE_SPEED / 3;
                drive.move(true, slowSpeed);
                slowedDown = true;
                log("Slowing down: " + (int)avgDist + "cm away - NO MORE SWEEPS, driving straight");
            }
            
            // Perform sweep scan periodically to stay aligned (ONLY when far away AND not yet slowed down)
            long now = System.currentTimeMillis();
            if (!slowedDown && now - lastSweepTime > sweepInterval && avgDist > 20.0f) {
                log("Distance check: " + (int)avgDist + "cm - performing sweep");
                drive.stop();
                sleep(200);  // Brief pause to ensure motors stopped
                
                // Use smaller sweep range when getting closer
                int sweepRange = 45;  // Default: ±45 degrees
                if (avgDist < 40.0f) {
                    sweepRange = 30;  // Getting closer: ±30 degrees
                }
                if (avgDist < 30.0f) {
                    sweepRange = 20;  // Close: ±20 degrees
                }
                
                quickSweepScan(sweepRange);
                lastSweepTime = now;
                
                // Resume forward movement using command handler
                int resumeSpeed = slowedDown ? (DRIVE_SPEED / 3) : DRIVE_SPEED;
                drive.move(true, resumeSpeed);
                
                sleep(200);  // Give motors time to stabilize forward movement
            }
            
            sleep(SAMPLE_INTERVAL_MS);
        }
        
        drive.stop();
        return false;
    }
    
    /**
     * Reads distance using best sensor for current range.
     * Prefers IR at very close range (<20cm) for precision.
     * 
     * @return distance in cm, or -1 if no valid reading
     */
    private float readBestDistance() {
        float us = readUltrasonicDistance();
        float ir = readInfraredDistance();
        
        // Prefer infrared at close range (<20cm), otherwise use ultrasonic
        if (ir > 0 && ir < 20.0f) {
            return ir;
        } else if (us > 0) {
            return us;
        } else if (ir > 0) {
            return ir;
        }
        
        return -1;
    }
    
    // Read infrared sensor distance in cm
    private float readInfraredDistance() {
        if (infraredSensor == null || !infraredSensor.isAvailable()) {
            return -1;
        }
        
        try {
            String value = infraredSensor.readValue();
            if (value != null && value.contains("=")) {
                String number = value.substring(value.indexOf('=') + 1).trim();
                if (number.contains(",")) {
                    number = number.split(",")[0];
                }
                float parsed = Float.parseFloat(number);
                return (parsed > 0 && !Float.isInfinite(parsed)) ? parsed : -1;
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        
        return -1;
    }
    
    // Read ultrasonic sensor distance in cm
    private float readUltrasonicDistance() {
        if (ultrasonicSensor == null || !ultrasonicSensor.isAvailable()) {
            return -1;
        }
        
        try {
            String value = ultrasonicSensor.readValue();
            if (value != null && value.contains("=")) {
                String number = value.substring(value.indexOf('=') + 1).trim();
                if (number.contains(",")) {
                    number = number.split(",")[0];
                }
                float parsed = Float.parseFloat(number);
                return (parsed > 0 && !Float.isInfinite(parsed)) ? parsed : -1;
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        
        return -1;
    }
    
    // ========== System Management ==========
    
    /**
     * Verifies that at least one distance sensor is available.
     */
    private boolean isSystemReady() {
        boolean hasUltrasonic = ultrasonicSensor != null && ultrasonicSensor.isAvailable();
        boolean hasInfrared = infraredSensor != null && infraredSensor.isAvailable();
        boolean hasDistance = hasUltrasonic || hasInfrared;
        
        if (!hasDistance) {
            log("Error: No distance sensor available");
            return false;
        }
        
        if (hasUltrasonic && hasInfrared) {
            log("System ready: Ultrasonic + Infrared sensors");
        } else if (hasUltrasonic) {
            log("System ready: Ultrasonic sensor only");
        } else {
            log("System ready: Infrared sensor only");
        }
        
        return hasDistance && drive.isReady();
    }
    
    /**
     * Emergency stop - halts all motor movement immediately.
     */
    public void stop() {
        log("*** EMERGENCY STOP ***");
        running = false;
        drive.stop();
    }
    
    // Raise arm
    private void raiseArm() {
        if (armMotor != null) {
            try {
                armMotor.rotateTo(RobotConfig.ARM_UP_POSITION, false);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // Lower arm
    private void lowerArm() {
        if (armMotor != null) {
            try {
                armMotor.rotateTo(RobotConfig.ARM_DOWN_POSITION, false);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    // Sleep helper
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    
    // Log message
    private void log(String message) {
        if (out != null) {
            try {
                out.write("LOG:" + message + "\n");
                out.flush();
            } catch (IOException e) {
                System.out.println(message);
            }
        } else {
            System.out.println(message);
        }
    }
}
