package client.network.command;

import client.sensor.ISensor;
import common.ParsedCommand;
import java.io.IOException;

/**
 * Waypoint navigation with obstacle avoidance.
 * Syntax: NAVLINEMAP <startX> <startY> <endX> <endY>
 */
public class NavLineMapCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    private static final float OBSTACLE_THRESHOLD_CM = 30.0f;
    private static final float WAYPOINT_TOLERANCE_CM = 5.0f;
    
    public NavLineMapCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVLINEMAP";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        // Parse start and end coordinates
        if (parsedCmd.getArgCount() < 4) {
            sendReply(context, "ERROR: NAVLINEMAP requires 4 parameters");
            sendReply(context, "Usage: NAVLINEMAP <startX> <startY> <endX> <endY>");
            return;
        }
        
        float startX = parsedCmd.getArgAsFloat(0, Float.NaN);
        float startY = parsedCmd.getArgAsFloat(1, Float.NaN);
        float endX = parsedCmd.getArgAsFloat(2, Float.NaN);
        float endY = parsedCmd.getArgAsFloat(3, Float.NaN);
        
        if (Float.isNaN(startX) || Float.isNaN(startY) || Float.isNaN(endX) || Float.isNaN(endY)) {
            sendReply(context, "ERROR: Invalid coordinates");
            return;
        }
        
        // Check drive system
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }
        
        // Get ultrasonic sensor for obstacle detection
        ISensor ultrasonicSensor = context.findSensor("ultrasonic");
        if (ultrasonicSensor == null) {
            sendReply(context, "WARNING: Ultrasonic sensor not available - no obstacle avoidance");
        }
        
        sendReply(context, "========== LINEMAP NAVIGATION START ==========");
        sendReply(context, String.format("Start position: (%.1f, %.1f) cm", startX, startY));
        sendReply(context, String.format("End position: (%.1f, %.1f) cm", endX, endY));
        
        // Calculate direct path parameters
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float targetAngle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
        
        sendReply(context, String.format("LINEMAP_DATA: start_x=%.1f start_y=%.1f end_x=%.1f end_y=%.1f", 
            startX, startY, endX, endY));
        sendReply(context, String.format("LINEMAP_DATA: distance=%.1f angle=%.1f", distance, targetAngle));
        sendReply(context, String.format("Path calculated: %.1f cm at %.1f deg", distance, targetAngle));
        
        try {
            // Rotate to target heading
            sendReply(context, String.format("Rotating to heading %.1f deg", targetAngle));
            context.getDrive().rotateDegrees(Math.round(targetAngle));
            Thread.sleep(500);
            float remaining = distance;
            int waypointNum = 1;
            
            while (remaining > WAYPOINT_TOLERANCE_CM) {
                // Check for obstacles
                if (ultrasonicSensor != null) {
                    try {
                        float obstacleDistance = Float.parseFloat(ultrasonicSensor.readValue());
                        if (obstacleDistance > 0 && obstacleDistance < OBSTACLE_THRESHOLD_CM) {
                            sendReply(context, String.format("Obstacle at %.1f cm - avoiding", obstacleDistance));
                            sendReply(context, "LINEMAP_DATA: obstacle_detected=true");
                            
                            context.getDrive().rotateDegrees(45);
                            Thread.sleep(300);
                            context.getDrive().moveForwardCm(20);
                            Thread.sleep(300);
                            context.getDrive().rotateDegrees(-45);
                            Thread.sleep(300);
                            
                            remaining -= 20;
                            waypointNum++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid sensor reading, continue navigation
                    }
                }
                
                float segment = Math.min(remaining, 20.0f);
                sendReply(context, String.format("Moving %.1fcm (%.1fcm remaining)", segment, remaining));
                
                context.getDrive().moveForwardCm(Math.round(segment));
                Thread.sleep(500);
                
                remaining -= segment;
                waypointNum++;
                
                if (waypointNum > 20) {
                    sendReply(context, "Max waypoints reached");
                    break;
                }
            }
            
            sendReply(context, "Navigation complete");
            sendReply(context, String.format("LINEMAP_DATA: waypoints=%d status=complete", waypointNum - 1));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendReply(context, "ERROR: Navigation interrupted");
        }
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
