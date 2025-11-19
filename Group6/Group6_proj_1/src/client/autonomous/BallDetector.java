package client.autonomous;

import client.autonomous.fusion.CloseRangeFusionStrategy;
import client.autonomous.fusion.ISensorFusionStrategy;
import client.autonomous.fusion.RangeAdaptiveFusionStrategy;
import client.config.RobotConfig;
import client.data.SensorDataWarehouse;
import client.motor.DifferentialDrive;
import client.motor.MotorFactory;
import client.sensor.ISensor;
import lejos.hardware.motor.BaseRegulatedMotor;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Autonomous ball detection using 360deg scan and sensor fusion.
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
    
    // Sensors (legacy - for direct reading)
    private final ISensor ultrasonicSensor;
    private final ISensor infraredSensor;
    
    // Sensor Fusion Strategies
    private final ISensorFusionStrategy scanStrategy;  // Used for 360° scan
    private final ISensorFusionStrategy approachStrategy;  // Used for final approach
    
    // Data warehouse (preferred - thread-safe access)
    private final SensorDataWarehouse warehouse;
    
    // Motors
    private final DifferentialDrive drive;
    private final BaseRegulatedMotor armMotor;
    
    // Logging
    private final BufferedWriter out;
    private boolean running = false;
    
    // 360° scan data storage (36 positions for 10° steps)
    private final float[] scanDistances = new float[360 / SCAN_STEP_DEGREES];
    private final int[] scanAngles = new int[360 / SCAN_STEP_DEGREES];
    
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
    
    /**
     * Constructor with warehouse for thread-safe sensor data access.
     */
    public BallDetector(ISensor ultrasonicSensor, ISensor infraredSensor, 
                       ISensor gyroSensor, ISensor colorSensor, 
                       BufferedWriter out, SensorDataWarehouse warehouse) {
        this.ultrasonicSensor = ultrasonicSensor;
        this.infraredSensor = infraredSensor;
        this.out = out;
        this.warehouse = warehouse;
        
        // Configure sensor fusion strategies
        // RangeAdaptive for sweeps (uses US≥50cm, IR<50cm for center alignment)
        // CloseRange for final approach (prefers IR<20cm for precision)
        this.scanStrategy = new RangeAdaptiveFusionStrategy();
        this.approachStrategy = new CloseRangeFusionStrategy();
        
        this.drive = new DifferentialDrive();
        
        this.armMotor = MotorFactory.getMotor(RobotConfig.ARM_MOTOR_PORT);
        if (this.armMotor != null) {
            this.armMotor.setSpeed(RobotConfig.ARM_SPEED);
        }
    }
    
    @Deprecated
    public BallDetector(ISensor ultrasonicSensor, ISensor infraredSensor, 
                       ISensor gyroSensor, ISensor colorSensor, BufferedWriter out) {
        this(ultrasonicSensor, infraredSensor, gyroSensor, colorSensor, out, null);
    }
    
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
     * Performs 360° scan while continuously rotating to locate closest object.
     */
    private int scan360ToFindBall() {
        log("Starting continuous 360 scan...");
        
        int scanIndex = 0;
        int targetRotation = 360;
        
        // Get initial gyro reading (if available from warehouse)
        float initialGyroAngle = readGyroAngle();
        int initialTacho = getAverageTachoCount();
        
        // Start continuous slow rotation
        drive.turnInPlace(true, SCAN_SPEED);
        
        long lastSampleTime = System.currentTimeMillis();
        int lastRecordedAngle = 0;
        
        // Continuously scan while rotating
        while (running && scanIndex < scanDistances.length) {
            // Calculate current rotation using gyro + tacho
            float currentGyroAngle = readGyroAngle();
            int currentTacho = getAverageTachoCount();
            
            int estimatedAngle = estimateRotationAngle(
                initialGyroAngle, currentGyroAngle,
                initialTacho, currentTacho
            );
            
            // Record sample at each 10-degree increment
            if (estimatedAngle >= lastRecordedAngle + SCAN_STEP_DEGREES) {
                // Read distance from warehouse using BOTH sensors (non-blocking!)
                // Use minimum distance to ensure we detect ball with either sensor
                float dist = readScanDistance();
                
                scanDistances[scanIndex] = dist;
                scanAngles[scanIndex] = estimatedAngle;
                
                if (dist > 0 && dist < 100.0f) {
                    log("Angle " + estimatedAngle + " deg: " + (int)dist + " cm");
                }
                
                scanIndex++;
                lastRecordedAngle = estimatedAngle;
            }
            
            // Stop when we've rotated 360 degrees
            if (estimatedAngle >= targetRotation) {
                break;
            }
            
            // Small delay to prevent busy-waiting
            sleep(20);
        }
        
        // Stop rotation
        drive.stop();
        
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
     */
    private int quickSweepScan(int sweepRange) {
        log("Quick sweep scan (+/-" + sweepRange + " deg)...");
        
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
            
            // Take reading using range-adaptive sensor fusion
            SensorBuffer buffer = new SensorBuffer();
            for (int j = 0; j < 3; j++) {
                // Use RangeAdaptiveFusionStrategy: US≥50cm, IR<50cm
                // IR is center-mounted, so aligning to closest IR = ball is centered
                float dist = scanStrategy.fuseDistance(ultrasonicSensor, infraredSensor);
                buffer.update(dist);
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
     */
    private boolean approachBall() {
        log("Approaching ball with continuous tracking...");
        
        SensorBuffer distBuffer = new SensorBuffer();
        boolean slowedDown = false;
        long lastSweepTime = System.currentTimeMillis();
        int sweepInterval = 5000;  // Sweep every 5 seconds (more time for forward movement)
        float lastValidDistance = -1;
        
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
            
            if (dist > 0 && dist < 200.0f) {  // Only update if reasonable // I might not need this anymore because we made signals inf
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
                int sweepRange = 45;  // Default: +/-45 degrees
                if (avgDist < 40.0f) {
                    sweepRange = 30;  // Getting closer: +/-30 degrees
                }
                if (avgDist < 30.0f) {
                    sweepRange = 20;  // Close: +/-20 degrees
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
     * Reads distance using sensor fusion strategy.
     * Uses CloseRangeFusionStrategy for final approach (prefers IR <20cm).
     */
    private float readBestDistance() {
        // Prefer warehouse if available (thread-safe, no blocking)
        if (warehouse != null) {
            return readFromWarehouse();
        }
        
        // Fallback to direct sensor reading (legacy)
        return approachStrategy.fuseDistance(ultrasonicSensor, infraredSensor);
    }
    
    /**
     * Reads fused distance from warehouse using CloseRange strategy logic.
     */
    private float readFromWarehouse() {
        try {
            SensorDataWarehouse.SensorReading usReading = warehouse.getLatest("ultrasonic");
            SensorDataWarehouse.SensorReading irReading = warehouse.getLatest("infrared");
            
            float us = (usReading != null) ? usReading.value : -1;
            float ir = (irReading != null) ? irReading.value : -1;
            
            // CloseRange fusion logic: prefer IR <20cm, else US, else IR
            if (ir > 0 && ir < 20.0f) {
                return ir;
            } else if (us > 0) {
                return us;
            } else if (ir > 0) {
                return ir;
            }
            
            return -1;
        } catch (Exception e) {
            // Gracefully handle any errors (e.g., during shutdown)
            return -1;
        }
    }
    
    /**
     * Reads distance for scanning - intelligently combines both sensors.
     * Prioritizes IR (center-mounted, better for ball detection).
     * Uses US as fallback for far objects or when IR returns infinity.
     */
    private float readScanDistance() {
        if (warehouse == null) {
            // Fallback to direct reading using scan strategy
            return scanStrategy.fuseDistance(ultrasonicSensor, infraredSensor);
        }
        
        try {
            SensorDataWarehouse.SensorReading usReading = warehouse.getLatest("ultrasonic");
            SensorDataWarehouse.SensorReading irReading = warehouse.getLatest("infrared");
            
            float us = (usReading != null) ? usReading.value : Float.POSITIVE_INFINITY;
            float ir = (irReading != null) ? irReading.value : Float.POSITIVE_INFINITY;
            
            // Prioritize IR if it sees something (not infinity)
            if (!Float.isInfinite(ir) && ir > 0) {
                return ir;  // IR has a valid reading - use it (center-mounted)
            }
            
            // IR is infinity, use US
            if (!Float.isInfinite(us) && us > 0) {
                return us;  // US has a valid reading
            }
            
            return -1;  // Both sensors see nothing or are invalid
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Reads current gyro angle from warehouse.
     * Returns 0 if gyro not available or warehouse not initialized.
     */
    private float readGyroAngle() {
        if (warehouse == null) {
            return 0;
        }
        
        try {
            SensorDataWarehouse.SensorReading gyroReading = warehouse.getLatest("gyro");
            return (gyroReading != null) ? gyroReading.value : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Gets average tacho count from both drive motors.
     */
    private int getAverageTachoCount() {
        try {
            int leftTacho = drive.getLeftMotor().getTachoCount();
            int rightTacho = drive.getRightMotor().getTachoCount();
            return (Math.abs(leftTacho) + Math.abs(rightTacho)) / 2;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Estimates rotation angle using both gyro and tacho counts for accuracy.
     * Gyro provides direct angle measurement but may drift.
     * Tacho provides reliable relative rotation but requires calibration.
     * This method combines both for best accuracy.
     */
    private int estimateRotationAngle(float initialGyro, float currentGyro, 
                                      int initialTacho, int currentTacho) {
        // Calculate angle from gyro (direct measurement)
        float gyroAngle = currentGyro - initialGyro;
        
        // Calculate angle from tacho counts (motor encoder)
        int tachoChange = currentTacho - initialTacho;
        float tachoAngle = tachoToDegrees(tachoChange);
        
        // If gyro is available (non-zero), use weighted average
        // Favor gyro for accuracy, but use tacho to prevent drift
        if (Math.abs(gyroAngle) > 0.1f) {
            // 70% gyro, 30% tacho (gyro is more accurate for angles)
            return Math.round(gyroAngle * 0.7f + tachoAngle * 0.3f);
        } else {
            // Gyro not available, use tacho only
            return Math.round(tachoAngle);
        }
    }
    
    /**
     * Converts motor tacho count change to robot rotation degrees.
     * Uses track width and wheel diameter from RobotConfig.
     */
    private float tachoToDegrees(int tachoChange) {
        // Convert motor degrees to wheel arc length
        float wheelCircumference = (float) (Math.PI * RobotConfig.WHEEL_DIAMETER_MM);
        float arcLength = (Math.abs(tachoChange) / 360.0f) * wheelCircumference;
        
        // Convert arc length to robot rotation angle
        float trackCircumference = (float) (Math.PI * RobotConfig.TRACK_WIDTH_MM);
        float robotAngle = (arcLength / trackCircumference) * 360.0f;
        
        return robotAngle;
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
