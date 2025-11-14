package client.autonomous;

import client.config.RobotConfig;
import client.motor.DifferentialDrive;
import client.motor.MotorFactory;
import client.sensor.ISensor;
import client.sensor.impl.LightSensor;
import lejos.hardware.motor.BaseRegulatedMotor;

import java.io.BufferedWriter;
import java.io.IOException;

// Detects and approaches balls using sensors and motor tacho counts for rotation tracking
public class BallDetector {
    
    private ISensor ultrasonicSensor;
    private ISensor infraredSensor;
    private ISensor gyroSensor;
    private ISensor colorSensor;
    
    private final DifferentialDrive drive;
    private final BaseRegulatedMotor leftMotor;
    private final BaseRegulatedMotor rightMotor;
    private BaseRegulatedMotor armMotor;
    
    private BufferedWriter out;
    private boolean running = false;
    private int confirmApproachSteps = 0;
    
    private enum Phase {
        SCOUTING,
        APPROACHING,
        CONFIRMING
    }
    
    private enum ConfirmationResult {
        FOUND,
        NEED_APPROACH,
        REJECTED
    }
    
    public BallDetector(ISensor ultrasonicSensor, ISensor gyroSensor, ISensor colorSensor, BufferedWriter out) {
        this(ultrasonicSensor, null, gyroSensor, colorSensor, out);
    }
    
    public BallDetector(ISensor ultrasonicSensor, ISensor infraredSensor, ISensor gyroSensor, ISensor colorSensor, BufferedWriter out) {
        this.ultrasonicSensor = ultrasonicSensor;
        this.infraredSensor = infraredSensor;
        this.gyroSensor = null; // Use tacho counts instead
        this.colorSensor = colorSensor;
        this.out = out;
        
        this.drive = new DifferentialDrive();
        this.leftMotor = drive.getLeftMotor();
        this.rightMotor = drive.getRightMotor();
        
        this.armMotor = MotorFactory.getMotor(RobotConfig.ARM_MOTOR_PORT);
        if (this.armMotor != null) {
            this.armMotor.setSpeed(RobotConfig.ARM_SPEED);
        }
    }
    
    // Sweep, approach, repeat until close, then verify color
    public boolean searchAndApproachBall() {
        if (!isSystemReady()) {
            log("SCAN: System not ready");
            return false;
        }
        
        running = true;
        log("========== BALL DETECTION ==========");
        testSensors();
        raiseArm();
        
        try {
            Phase phase = Phase.SCOUTING;
            float remainingDistance = 0f;
            float distanceSinceSweep = 0f;
            while (running) {
                if (phase == Phase.SCOUTING) {
                    SweepResult sweep = sweepForObject();
                    
                    if (!sweep.found) {
                        log("SCAN: No object found");
                        return false;
                    }
                    
                    log("Target candidate " + sweep.angle + "deg, " + (int)sweep.distance + "cm");
                    
                    confirmApproachSteps = 0;
                    
                    if (Math.abs(sweep.angle) > 2) {
                        log("Turning " + sweep.angle + "deg");
                        if (!rotateSafe(sweep.angle, 40)) {
                            return false;
                        }
                    }
                    
                    fineAlignHeading();
                    
                    remainingDistance = sweep.distance;
                    distanceSinceSweep = 0f;
                    
                    if (remainingDistance <= RobotConfig.IR_CONFIRM_DISTANCE_CM) {
                        phase = Phase.CONFIRMING;
                    } else {
                        phase = Phase.APPROACHING;
                    }
                    continue;
                }
                
                if (phase == Phase.APPROACHING) {
                    float gap = Math.max(0f, remainingDistance - RobotConfig.IR_CONFIRM_DISTANCE_CM);
                    float dynamicStep = gap * 0.6f;
                    float moveChunk = Math.min(RobotConfig.RESWEEP_DISTANCE_CM,
                        Math.max(4f, dynamicStep));
                    
                    if (moveChunk <= 3) {
                        phase = Phase.CONFIRMING;
                        continue;
                    }
                    
                    log("Advancing " + (int)moveChunk + "cm toward target");
                    
                    int steps = Math.max(1, (int) Math.ceil(moveChunk / RobotConfig.SENSOR_POLL_INTERVAL_CM));
                    float stepSize = moveChunk / steps;
                    for (int step = 0; step < steps && running; step++) {
                        if (!moveForwardSafe((int) stepSize, 80)) {
                            return false;
                        }
                        DistanceSample midSample = readDistanceSample();
                        logDistanceSample("Step " + (step + 1) + "/" + steps, midSample);
                        if (midSample.shortRange > 0 && midSample.shortRange <= RobotConfig.IR_APPROACH_DISTANCE_CM) {
                            log("IR indicates close contact");
                            phase = Phase.CONFIRMING;
                            break;
                        }
                    }
                    if (phase == Phase.CONFIRMING) {
                        continue;
                    }
                    
                    distanceSinceSweep += moveChunk;
                    remainingDistance = Math.max(remainingDistance - moveChunk, 0f);
                    
                    DistanceSample current = readDistanceSample();
                    logDistanceSample("After move", current);
                    
                    if (current.shortRange > 0 && current.shortRange <= RobotConfig.IR_APPROACH_DISTANCE_CM) {
                        log("IR indicates close contact");
                        phase = Phase.CONFIRMING;
                        continue;
                    }
                    
                    if (isWallDelta(current)) {
                        log("Wall delta detected, nudging heading");
                        rotateSafe(5, 40);
                    }
                    
                    if (remainingDistance <= RobotConfig.IR_CONFIRM_DISTANCE_CM) {
                        phase = Phase.CONFIRMING;
                        continue;
                    }
                    
                    if (distanceSinceSweep >= RobotConfig.RESWEEP_DISTANCE_CM) {
                        log("Re-sweeping to refine target");
                        confirmApproachSteps = 0;
                        phase = Phase.SCOUTING;
                    }
                    continue;
                }
                
                if (phase == Phase.CONFIRMING) {
                    ConfirmationResult confirmation = confirmTarget();
                    if (confirmation == ConfirmationResult.FOUND) {
                        confirmApproachSteps = 0;
                        return true;
                    } else if (confirmation == ConfirmationResult.REJECTED) {
                        confirmApproachSteps = 0;
                        phase = Phase.SCOUTING;
                    } else {
                        phase = Phase.APPROACHING;
                    }
                    continue;
                }
            }
            
            log("SCAN: Stopped");
            return false;
        } catch (Exception e) {
            log("SCAN: Error - " + e.getMessage());
            return false;
        } finally {
            running = false;
            drive.stop();
            lowerArm();
        }
    }
    
    private static class SweepResult {
        boolean found = false;
        int angle = 0;
        float distance = 999;
        float colorScore = 0f;
        float score = 0f;
    }
    
    private static class DistanceSample {
        float longRange = -1f;
        float shortRange = -1f;
    }
    
    // Sweep 120 degrees using tacho counts, find closest object
    private SweepResult sweepForObject() {
        SweepResult result = new SweepResult();
        
        log("Sweeping...");
        
        if (!rotateSafe(-60, 40)) {
            return result;
        }
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        
        if (!running) return result;
        
        float previousDist = -1f;
        
        if (leftMotor == null || rightMotor == null) return result;
        
        float trackCircumference = (float) (Math.PI * RobotConfig.TRACK_WIDTH_MM);
        float arcLength = trackCircumference * 120.0f / 360.0f;
        float wheelCircumference = (float) (Math.PI * RobotConfig.WHEEL_DIAMETER_MM);
        int totalMotorDegrees = (int) (arcLength / wheelCircumference * 360);
        int degreesPerSample = totalMotorDegrees / 40;
        
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        
        leftMotor.setSpeed(30);
        rightMotor.setSpeed(30);
        leftMotor.forward();
        rightMotor.backward();
        
        int currentAngle = -60;
        for (int i = 0; i < 40 && running; i++) {
            DistanceSample sample = readDistanceSample();
            if (sample.longRange > 0 || sample.shortRange > 0) {
                logDistanceSample("Sweep @" + currentAngle, sample);
            }
            float dist = pickSweepDistance(sample);
            
            if (dist > 0 && dist < RobotConfig.OBSTACLE_DISTANCE_CM * 2) {
                float filtered = (previousDist > 0) ? (previousDist + dist) / 2f : dist;
                previousDist = filtered;
                
                float distanceScore = RobotConfig.BALL_SCORE_DISTANCE_WEIGHT / Math.max(filtered, 1f);
                
                if (!result.found || distanceScore >= result.score) {
                    float colorScore = 0f;
                    if (filtered <= RobotConfig.BALL_COLOR_MAX_DISTANCE_CM) {
                        colorScore = measureColorScore();
                    }
                    
                    float totalScore = distanceScore + RobotConfig.BALL_SCORE_COLOR_WEIGHT * colorScore;
                    
                    if (!result.found || totalScore > result.score) {
                        result.found = true;
                        result.angle = currentAngle;
                        result.distance = filtered;
                        result.colorScore = colorScore;
                        result.score = totalScore;
                        log(String.format("  %ddeg: %dcm score=%.2f color=%.2f", currentAngle, (int) filtered, totalScore, colorScore));
                    }
                }
            }
            
            while (running && Math.abs(leftMotor.getTachoCount()) < (i + 1) * degreesPerSample) {
                try { Thread.sleep(20); } catch (InterruptedException e) {}
            }
            
            currentAngle += 3;
        }
        
        drive.stop();
        rotateSafe(-60, 40);
        
        if (result.found) {
            log("Found at " + result.angle + "deg, " + (int)result.distance + "cm (score " + String.format("%.2f", result.score) + ")");
        } else {
            log("No object found in sweep");
        }
        
        return result;
    }
    
    private ConfirmationResult confirmTarget() {
        log("Confirming target...");
        DistanceSample sample = readDistanceSample();
        logDistanceSample("Confirm sample", sample);
        
        if (sample.shortRange <= 0) {
            if (sample.longRange > 0 && sample.longRange <= RobotConfig.IR_COLOR_CONTACT_DISTANCE_CM) {
                sample.shortRange = sample.longRange;
            } else {
                log("IR still not seeing target, keep approaching");
                if (!advanceForConfirmation(4)) {
                    return ConfirmationResult.REJECTED;
                }
                return ConfirmationResult.NEED_APPROACH;
            }
        }
        
        if (sample.shortRange <= RobotConfig.IR_STOP_DISTANCE_CM) {
            drive.stop();
            log("SCAN: Ball reached (IR " + sample.shortRange + "cm)");
            return ConfirmationResult.FOUND;
        }
        
        if (sample.shortRange > RobotConfig.IR_COLOR_CONTACT_DISTANCE_CM) {
            int advance = Math.max(2, Math.min(6, (int)(sample.shortRange - RobotConfig.IR_COLOR_CONTACT_DISTANCE_CM)));
            log("Closing gap (" + sample.shortRange + "cm -> +" + advance + "cm)");
            if (!advanceForConfirmation(advance)) {
                return ConfirmationResult.REJECTED;
            }
            return ConfirmationResult.NEED_APPROACH;
        }
        
        drive.stop();
        log("SCAN: Ball proximity reached (" + sample.shortRange + "cm)");
        return ConfirmationResult.FOUND;
    }
    
    private boolean advanceForConfirmation(int cm) {
        if (++confirmApproachSteps > RobotConfig.MAX_CONFIRM_APPROACH_STEPS) {
            log("Too many blind approach steps, aborting target");
            confirmApproachSteps = 0;
            return false;
        }
        return moveForwardSafe(cm, 60);
    }
    
    private float measureColorScore() {
        if (!(colorSensor instanceof LightSensor)) {
            return 0f;
        }
        LightSensor ls = (LightSensor) colorSensor;
        if (!ls.isAvailable()) {
            return 0f;
        }
        
        int samples = Math.max(1, RobotConfig.BALL_COLOR_SAMPLES);
        float totalRatio = 0f;
        int collected = 0;
        
        for (int i = 0; i < samples; i++) {
            float[] rgb = ls.readRgbSample();
            if (rgb == null || rgb.length < 3) {
                continue;
            }
            float total = rgb[0] + rgb[1] + rgb[2];
            if (total <= 0) {
                continue;
            }
            totalRatio += rgb[0] / total;
            collected++;
            
            try {
                Thread.sleep((long) RobotConfig.BALL_COLOR_SAMPLE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            if (!running) {
                break;
            }
        }
        
        if (collected == 0 || !running) {
            return 0f;
        }
        
        float average = totalRatio / collected;
        return average >= RobotConfig.BALL_COLOR_RATIO_THRESHOLD ? average : 0f;
    }

    private boolean rotateSafe(int degrees, int speed) {
        if (!running || degrees == 0) {
            return running;
        }
        drive.rotateDegrees(degrees, speed);
        return running;
    }

    private boolean moveForwardSafe(int cm, int speed) {
        if (!running || cm <= 0) {
            return running;
        }
        drive.moveForwardCm(cm, speed);
        return running;
    }

    private void fineAlignHeading() {
        if (!running) return;
        
        int[] offsets = {-6, -3, 0, 3, 6};
        int applied = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        int bestOffset = 0;
        
        for (int offset : offsets) {
            if (!running) break;
            int delta = offset - applied;
            if (delta != 0 && !rotateSafe(delta, 30)) {
                return;
            }
            applied = offset;
            DistanceSample sample = readDistanceSample();
            float score = headingScore(sample);
            if (score > bestScore) {
                bestScore = score;
                bestOffset = offset;
            }
        }
        
        int correction = bestOffset - applied;
        if (correction != 0) {
            rotateSafe(correction, 30);
        }
        log("Fine alignment offset " + bestOffset + "deg (score " + String.format("%.2f", bestScore) + ")");
    }
    
    private float headingScore(DistanceSample sample) {
        float distance = pickSweepDistance(sample);
        if (distance <= 0) {
            return Float.NEGATIVE_INFINITY;
        }
        return -distance;
    }
    
    // Verify color with voting - accepts if majority match
    private boolean verifyBallColor() {
        if (colorSensor == null || !colorSensor.isAvailable()) {
            return false;
        }
        
        if (colorSensor instanceof LightSensor) {
            LightSensor ls = (LightSensor) colorSensor;
            int matchCount = 0;
            int samples = 0;
            for (int i = 0; i < RobotConfig.COLOR_VERIFY_ATTEMPTS && running; i++) {
                float[] rgb = ls.readRgbSample();
                if (rgb != null && rgb.length >= 3) {
                    float total = rgb[0] + rgb[1] + rgb[2];
                    if (total > 0) {
                        float ratio = rgb[0] / total;
                        samples++;
                        if (ratio >= RobotConfig.BALL_COLOR_RATIO_THRESHOLD) {
                            matchCount++;
                        }
                        log(String.format("Color sample %d: ratio=%.2f", i, ratio));
                    }
                }
                try { Thread.sleep(80); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            boolean verified = samples > 0 && matchCount > 0;
            log("Color verify RGB: " + matchCount + "/" + samples + " = " + verified);
            return verified;
        }
        
        int matchCount = 0;
        int totalSamples = 0;
        
        for (int i = 0; i < RobotConfig.COLOR_VERIFY_ATTEMPTS && running; i++) {
            String colorValue = colorSensor.readValue();
            if (colorValue != null && colorValue.contains("=")) {
                try {
                    String[] parts = colorValue.split("=");
                    if (parts.length > 1) {
                        String valueStr = parts[1].trim();
                        if (!valueStr.contains(",")) {
                            int colorId = (int) Float.parseFloat(valueStr);
                            totalSamples++;
                            
                            log("Color sample " + i + ": " + colorId);
                            
                            if (colorId == RobotConfig.TARGET_BALL_COLOR_ID) {
                                matchCount++;
                            } else if (RobotConfig.TARGET_BALL_COLOR_ID == 5 && (colorId == 4 || colorId == 6)) {
                                matchCount++;
                                log("  (Close match)");
                            }
                        }
                    }
                } catch (NumberFormatException e) {}
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        boolean verified = totalSamples > 0 && (matchCount * 100 / totalSamples) >= RobotConfig.COLOR_MATCH_THRESHOLD_PERCENT;
        log("Color verify: " + matchCount + "/" + totalSamples + " = " + verified);
        return verified;
    }
    
    private boolean isSystemReady() {
        boolean hasUltrasonic = ultrasonicSensor != null && ultrasonicSensor.isAvailable();
        boolean hasInfrared = infraredSensor != null && infraredSensor.isAvailable();
        return (hasUltrasonic || hasInfrared) && drive.isReady();
    }
    
    private void testSensors() {
        if (ultrasonicSensor != null && ultrasonicSensor.isAvailable()) {
            String value = ultrasonicSensor.readValue();
            log("Ultrasonic: " + (value != null ? value : "NULL"));
        } else {
            log("Ultrasonic: NOT AVAILABLE");
        }
        
        if (infraredSensor != null && infraredSensor.isAvailable()) {
            String value = infraredSensor.readValue();
            log("Infrared: " + (value != null ? value : "NULL"));
        } else {
            log("Infrared: NOT AVAILABLE");
        }
        
        if (colorSensor != null && colorSensor.isAvailable()) {
            String value = colorSensor.readValue();
            log("Color: " + (value != null ? value : "NULL"));
        } else {
            log("Color: NOT AVAILABLE");
        }
        
        if (gyroSensor != null && gyroSensor.isAvailable()) {
            String value = gyroSensor.readValue();
            log("Gyro: " + (value != null ? value : "NULL"));
        } else {
            log("Gyro: NOT AVAILABLE");
        }
        
        log("Sensor test complete");
    }
    
    private DistanceSample readDistanceSample() {
        DistanceSample sample = new DistanceSample();
        sample.longRange = readSensorDistance(ultrasonicSensor);
        sample.shortRange = readSensorDistance(infraredSensor);
        return sample;
    }
    
    private float readSensorDistance(ISensor sensor) {
        if (sensor == null || !sensor.isAvailable()) {
            return -1;
        }
        try {
            String value = sensor.readValue();
            if (value != null && value.contains("=")) {
                String number = value.substring(value.indexOf('=') + 1).trim();
                if (number.contains(",")) {
                    number = number.split(",")[0];
                }
                return Float.parseFloat(number);
            }
        } catch (Exception e) {
        }
        return -1;
    }
    
    private float pickSweepDistance(DistanceSample sample) {
        float combined = chooseDistance(sample);
        if (sample.shortRange > 0 && sample.longRange > 0) {
            float delta = sample.longRange - sample.shortRange;
            if (delta >= RobotConfig.BALL_NEAR_WALL_DELTA_CM) {
                combined = sample.shortRange;
            }
        }
        return combined;
    }
    
    private boolean isWallDelta(DistanceSample sample) {
        return sample.shortRange > 0 && sample.longRange > 0 &&
               (sample.longRange - sample.shortRange) >= RobotConfig.BALL_NEAR_WALL_DELTA_CM;
    }
    
    private void logDistanceSample(String prefix, DistanceSample sample) {
        log(String.format("%s: US=%.1fcm IR=%.1fcm", prefix,
            sample.longRange, sample.shortRange));
    }
    
    // Read distance from ultrasonic/infrared, prefer IR at close range
    private float readDistance() {
        return chooseDistance(readDistanceSample());
    }
    
    private float chooseDistance(DistanceSample sample) {
        if (sample.shortRange > 0 && sample.shortRange < 20.0f) {
            return sample.shortRange;
        }
        if (sample.longRange > 0) {
            return sample.longRange;
        }
        if (sample.shortRange > 0) {
            return sample.shortRange;
        }
        return -1;
    }
    
    public void stop() {
        log("*** EMERGENCY STOP ***");
        running = false;
        drive.stop();
    }
    
    private void raiseArm() {
        if (armMotor != null) {
            try {
                armMotor.rotateTo(RobotConfig.ARM_UP_POSITION, false);
            } catch (Exception e) {}
        }
    }
    
    private void lowerArm() {
        if (armMotor != null) {
            try {
                armMotor.rotateTo(RobotConfig.ARM_DOWN_POSITION, false);
            } catch (Exception e) {}
        }
    }
    
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

