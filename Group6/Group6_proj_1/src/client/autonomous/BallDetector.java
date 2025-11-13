package client.autonomous;

import client.config.RobotConfig;
import client.motor.MotorFactory;
import client.sensor.ISensor;
import lejos.hardware.motor.BaseRegulatedMotor;

import java.io.BufferedWriter;
import java.io.IOException;

// Detects and approaches balls using sensors and motor tacho counts for rotation tracking
public class BallDetector {
    
    private ISensor ultrasonicSensor;
    private ISensor infraredSensor;
    private ISensor gyroSensor;
    private ISensor colorSensor;
    
    private BaseRegulatedMotor leftMotor;
    private BaseRegulatedMotor rightMotor;
    private BaseRegulatedMotor armMotor;
    
    private BufferedWriter out;
    private boolean running = false;
    
    public BallDetector(ISensor ultrasonicSensor, ISensor gyroSensor, ISensor colorSensor, BufferedWriter out) {
        this(ultrasonicSensor, null, gyroSensor, colorSensor, out);
    }
    
    public BallDetector(ISensor ultrasonicSensor, ISensor infraredSensor, ISensor gyroSensor, ISensor colorSensor, BufferedWriter out) {
        this.ultrasonicSensor = ultrasonicSensor;
        this.infraredSensor = infraredSensor;
        this.gyroSensor = null; // Use tacho counts instead
        this.colorSensor = colorSensor;
        this.out = out;
        
        char[] driveMotors = RobotConfig.DRIVE_MOTORS;
        if (driveMotors != null && driveMotors.length >= 2) {
            this.leftMotor = MotorFactory.getMotor(driveMotors[0]);
            this.rightMotor = MotorFactory.getMotor(driveMotors[1]);
        }
        
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
            while (running) {
                SweepResult sweep = sweepForObject();
                
                if (!sweep.found) {
                    log("SCAN: No object found");
                    return false;
                }
                
                log("Object at " + sweep.angle + "deg, " + (int)sweep.distance + "cm");
                
                if (sweep.distance < 15) {
                    log("Close enough - checking color");
                    break;
                }
                
                if (Math.abs(sweep.angle) > 3) {
                    log("Turning " + sweep.angle + "deg");
                    turnDegrees(sweep.angle);
                }
                
                if (!running) break;
                
                float moveDistance = Math.min(sweep.distance - 15, 20);
                
                if (moveDistance < 5) {
                    log("Very close - stopping");
                    break;
                }
                
                log("Moving forward " + (int)moveDistance + "cm");
                moveForwardCm((int)moveDistance);
                
                try { Thread.sleep(200); } catch (InterruptedException e) {}
                float currentDist = readDistance();
                
                if (infraredSensor != null && infraredSensor.isAvailable()) {
                    String irValue = infraredSensor.readValue();
                    if (irValue != null && irValue.contains("=")) {
                        try {
                            float irDist = Float.parseFloat(irValue.split("=")[1].trim());
                            if (irDist > 0 && irDist < currentDist) {
                                currentDist = irDist;
                            }
                        } catch (Exception e) {}
                    }
                }
                
                log("After move, distance: " + (int)currentDist + "cm");
                
                if (currentDist > 0 && currentDist < 15) {
                    log("Close enough now - stopping");
                    break;
                }
            }
            
            if (!running) {
                log("SCAN: Stopped");
                return false;
            }
            
            log("Aligning color sensor...");
            turnDegrees(-7);
            try { Thread.sleep(300); } catch (InterruptedException e) {}
            
            if (colorSensor != null && colorSensor.isAvailable()) {
                boolean correctColor = verifyBallColor();
                turnDegrees(7);
                
                if (correctColor) {
                    log("SCAN: Ball found!");
                    return true;
                } else {
                    log("SCAN: Wrong color");
                    return false;
                }
            } else {
                log("SCAN: Object found (no color check)");
                return true;
            }
            
        } catch (Exception e) {
            log("SCAN: Error - " + e.getMessage());
            return false;
        } finally {
            running = false;
            stopMotors();
            lowerArm();
        }
    }
    
    private static class SweepResult {
        boolean found = false;
        int angle = 0;
        float distance = 999;
    }
    
    // Sweep 120 degrees using tacho counts, find closest object
    private SweepResult sweepForObject() {
        SweepResult result = new SweepResult();
        
        log("Sweeping...");
        
        turnDegrees(-60);
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        
        if (!running) return result;
        
        int bestAngle = -60;
        float minDist = 999;
        
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
            float dist = readDistance();
            
            // Prefer closer readings (more likely to be the ball)
            if (dist > 0 && dist < minDist && dist < 200) {
                minDist = dist;
                bestAngle = currentAngle;
                log("  " + currentAngle + "deg: " + (int)dist + "cm");
            }
            
            while (running && Math.abs(leftMotor.getTachoCount()) < (i + 1) * degreesPerSample) {
                try { Thread.sleep(20); } catch (InterruptedException e) {}
            }
            
            currentAngle += 3;
        }
        
        stopMotors();
        turnDegrees(-60);
        
        if (minDist < 200) {
            result.found = true;
            result.angle = bestAngle;
            result.distance = minDist;
            log("Found at " + bestAngle + "deg, " + (int)minDist + "cm");
        } else {
            log("No object found in sweep");
        }
        
        return result;
    }
    
    // Turn using tacho counts (negative = left, positive = right)
    private void turnDegrees(int degrees) {
        if (Math.abs(degrees) < 2) return;
        if (leftMotor == null || rightMotor == null) return;
        
        float trackCircumference = (float) (Math.PI * RobotConfig.TRACK_WIDTH_MM);
        float arcLength = trackCircumference * Math.abs(degrees) / 360.0f;
        float wheelCircumference = (float) (Math.PI * RobotConfig.WHEEL_DIAMETER_MM);
        int motorDegrees = (int) (arcLength / wheelCircumference * 360);
        
        leftMotor.setSpeed(40);
        rightMotor.setSpeed(40);
        
        if (degrees < 0) {
            leftMotor.rotate(-motorDegrees, true);
            rightMotor.rotate(motorDegrees, false);
        } else {
            leftMotor.rotate(motorDegrees, true);
            rightMotor.rotate(-motorDegrees, false);
        }
    }
    
    // Move forward using tacho counts
    private void moveForwardCm(int cm) {
        if (cm < 1 || leftMotor == null || rightMotor == null) return;
        
        float wheelCircumference = (float) (Math.PI * RobotConfig.WHEEL_DIAMETER_MM);
        int motorDegrees = (int) (cm * 10 / wheelCircumference * 360);
        
        leftMotor.setSpeed(80);
        rightMotor.setSpeed(80);
        leftMotor.rotate(motorDegrees, true);
        rightMotor.rotate(motorDegrees, false);
    }
    
    // Verify color with voting - accepts if majority match
    private boolean verifyBallColor() {
        if (colorSensor == null || !colorSensor.isAvailable()) {
            return false;
        }
        
        int matchCount = 0;
        int totalSamples = 0;
        
        for (int i = 0; i < RobotConfig.COLOR_VERIFY_ATTEMPTS; i++) {
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
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        boolean verified = totalSamples > 0 && (matchCount * 100 / totalSamples) >= RobotConfig.COLOR_MATCH_THRESHOLD_PERCENT;
        log("Color verify: " + matchCount + "/" + totalSamples + " = " + verified);
        return verified;
    }
    
    private boolean isSystemReady() {
        if (ultrasonicSensor == null || !ultrasonicSensor.isAvailable()) {
            return false;
        }
        if (leftMotor == null || rightMotor == null) {
            return false;
        }
        return true;
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
    
    // Read distance from ultrasonic/infrared, prefer IR at close range
    private float readDistance() {
        float ultrasonicDist = -1;
        float infraredDist = -1;
        
        if (ultrasonicSensor != null) {
            try {
                String value = ultrasonicSensor.readValue();
                if (value != null && value.contains("=")) {
                    ultrasonicDist = Float.parseFloat(value.split("=")[1]);
                }
            } catch (Exception e) {}
        }
        
        if (infraredSensor != null) {
            try {
                String value = infraredSensor.readValue();
                if (value != null && value.contains("=")) {
                    infraredDist = Float.parseFloat(value.split("=")[1]);
                }
            } catch (Exception e) {}
        }
        
        if (infraredDist > 0 && infraredDist < 20.0f) {
            return infraredDist;
        } else if (ultrasonicDist > 0) {
            return ultrasonicDist;
        } else if (infraredDist > 0) {
            return infraredDist;
        }
        
        return -1;
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
    
    public void stop() {
        log("*** EMERGENCY STOP ***");
        running = false;
        stopMotors();
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

