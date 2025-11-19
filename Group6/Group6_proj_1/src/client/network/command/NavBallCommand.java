package client.network.command;

import client.sensor.ISensor;
import common.ParsedCommand;
import java.io.IOException;

/**
 * Navigate to closest object (ball) using 360deg scan and waypoint strategy.
 * Syntax: NAVBALL [stopDistance]
 */
public class NavBallCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    
    private static final float OBSTACLE_THRESHOLD_CM = 30.0f;
    private static final float WAYPOINT_SEGMENT_CM = 15.0f;
    private static final float SLOW_DOWN_DISTANCE_CM = 20.0f;
    private static final int SCAN_STEP_DEGREES = 10;
    private static final int DRIVE_SPEED = 200;
    private static final int SLOW_SPEED = 80;
    
    public NavBallCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVBALL";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        // Parse stop distance parameter
        float stopDistance = parsedCmd.getArgAsFloat(0, 5.0f);
        
        if (stopDistance < 2.0f || stopDistance > 50.0f) {
            sendReply(context, "ERROR: Stop distance must be 2-50cm");
            return;
        }
        
        // Check drive system
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }
        
        // Get ultrasonic sensor for obstacle detection and scanning
        ISensor ultrasonicSensor = context.findSensor("ultrasonic");
        ISensor infraredSensor = context.findSensor("infrared");
        
        if (ultrasonicSensor == null && infraredSensor == null) {
            sendReply(context, "ERROR: No distance sensor available");
            return;
        }
        
        sendReply(context, String.format("NAVBALL: stop distance %.1fcm", stopDistance));
        
        try {
            sendReply(context, "Scanning 360deg...");
            ScanResult scanResult = scan360(context, ultrasonicSensor, infraredSensor);
            
            if (scanResult == null) {
                sendReply(context, "No target found");
                return;
            }
            
            sendReply(context, String.format("Target: %.1fcm at %d deg", 
                scanResult.distance, scanResult.angle));
            
            sendReply(context, "Navigating to target...");
            navigateToTarget(context, scanResult, stopDistance, ultrasonicSensor, infraredSensor);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendReply(context, "ERROR: Navigation interrupted");
        } finally {
            context.getDrive().stop();
        }
    }
    
    private ScanResult scan360(CommandContext context, ISensor ultrasonicSensor, 
                               ISensor infraredSensor) throws IOException, InterruptedException {
        float bestDistance = Float.POSITIVE_INFINITY;
        int bestAngle = 0;
        int numScans = 360 / SCAN_STEP_DEGREES;
        int validReadings = 0;
        
        for (int i = 0; i < numScans; i++) {
            int currentAngle = i * SCAN_STEP_DEGREES;
            
            float distance = readDistance(infraredSensor, ultrasonicSensor);
            
            if (distance > 0 && !Float.isInfinite(distance)) {
                validReadings++;
                sendReply(context, String.format("%d deg: %.1f cm", currentAngle, distance));
                
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestAngle = currentAngle;
                    sendReply(context, String.format("New closest: %.1f cm", distance));
                }
            }
            
            // Rotate to next scan position
            if (i < numScans - 1) {
                context.getDrive().rotateDegrees(SCAN_STEP_DEGREES);
                Thread.sleep(100);
            }
        }
        
        if (Float.isInfinite(bestDistance) || bestDistance <= 0) {
            return null;
        }
        
        int rotateBack = bestAngle - 360;
        context.getDrive().rotateDegrees(rotateBack);
        Thread.sleep(300);
        
        return new ScanResult(bestDistance, bestAngle);
    }
    
    private boolean navigateToTarget(CommandContext context, ScanResult target, 
                                     float stopDistance, ISensor ultrasonicSensor, 
                                     ISensor infraredSensor) 
            throws IOException, InterruptedException {
        
        float remaining = target.distance;
        int waypointNum = 1;
        boolean slowMode = false;
        
        while (remaining > stopDistance) {
            // Read current distance to target
            float currentDistance = readDistance(infraredSensor, ultrasonicSensor);
            
            if (currentDistance > 0) {
                remaining = currentDistance;
                
                // Check if we've reached stop distance
                if (remaining <= stopDistance) {
                    sendReply(context, String.format("Target reached: %.1fcm", remaining));
                    return true;
                }
                
                if (!slowMode && remaining < SLOW_DOWN_DISTANCE_CM) {
                    slowMode = true;
                }
            }
            
            if (remaining > SLOW_DOWN_DISTANCE_CM && ultrasonicSensor != null) {
                try {
                    float obstacleCheck = Float.parseFloat(ultrasonicSensor.readValue());
                    
                    if (obstacleCheck > 0 && obstacleCheck < OBSTACLE_THRESHOLD_CM 
                        && obstacleCheck < remaining - 5) {
                        
                        sendReply(context, String.format("Obstacle at %.1f cm - avoiding", obstacleCheck));
                        
                        context.getDrive().rotateDegrees(45);
                        Thread.sleep(200);
                        context.getDrive().moveForwardCm(20);
                        Thread.sleep(200);
                        context.getDrive().rotateDegrees(-45);
                        Thread.sleep(200);
                        
                        waypointNum++;
                        continue;
                    }
                } catch (NumberFormatException e) {
                }
            }
            
            float segment = Math.min(remaining - stopDistance, 
                slowMode ? WAYPOINT_SEGMENT_CM / 2 : WAYPOINT_SEGMENT_CM);
            
            if (segment <= 0) {
                break;
            }
            
            sendReply(context, String.format("Moving %.1fcm", segment));
            
            if (slowMode) {
                context.getDrive().moveForwardCm(Math.round(segment), SLOW_SPEED);
                Thread.sleep(400);
            } else {
                context.getDrive().moveForwardCm(Math.round(segment), DRIVE_SPEED);
                Thread.sleep(300);
            }
            
            waypointNum++;
            
            if (waypointNum > 30) {
                break;
            }
        }
        
        float finalDistance = readDistance(infraredSensor, ultrasonicSensor);
        sendReply(context, String.format("Complete: %.1fcm", finalDistance));
        
        return finalDistance > 0 && finalDistance <= stopDistance + 5.0f;
    }
    
    private float readDistance(ISensor infraredSensor, ISensor ultrasonicSensor) {
        if (infraredSensor != null) {
            try {
                String reading = infraredSensor.readValue();
                float distance = Float.parseFloat(reading);
                if (distance > 0 && !Float.isInfinite(distance)) {
                    return distance;
                }
            } catch (NumberFormatException e) {
            }
        }
        
        if (ultrasonicSensor != null) {
            try {
                String reading = ultrasonicSensor.readValue();
                float distance = Float.parseFloat(reading);
                if (distance > 0 && !Float.isInfinite(distance)) {
                    return distance;
                }
            } catch (NumberFormatException e) {
            }
        }
        
        return -1;
    }
    
    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
    
    private static class ScanResult {
        final float distance;
        final int angle;
        
        ScanResult(float distance, int angle) {
            this.distance = distance;
            this.angle = angle;
        }
    }
}
