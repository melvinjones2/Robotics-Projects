package client.network.command;

import client.sensor.ISensor;
import common.ParsedCommand;
import java.io.IOException;

/**
 * Build occupancy grid map using 360deg ultrasonic scan.
 * Syntax: NAVGRIDMAP [radius] [interval]
 */
public class NavGridMapCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    
    public NavGridMapCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVGRIDMAP";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        // Parse parameters
        int radius = Math.round(parsedCmd.getArgAsFloat(0, 50.0f));
        int interval = Math.round(parsedCmd.getArgAsFloat(1, 30.0f));
        
        // Check if drive is ready
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }
        
        // Check if ultrasonic sensor is available
        ISensor ultrasonicSensor = context.findSensor("ultrasonic");
        if (ultrasonicSensor == null) {
            sendReply(context, "ERROR: Ultrasonic sensor not available");
            return;
        }
        
        sendReply(context, String.format("Grid mapping: radius=%dcm, interval=%d deg", radius, interval));
        
        int numScans = 360 / interval;
        int validScans = 0;
        float minDistance = Float.POSITIVE_INFINITY;
        float maxDistance = 0;
        float totalDistance = 0;
        
        try {
            for (int i = 0; i < numScans; i++) {
                int heading = i * interval;
                
                float distance = -1;
                try {
                    distance = Float.parseFloat(ultrasonicSensor.readValue());
                } catch (NumberFormatException e) {
                }
                
                if (distance >= 0 && !Float.isInfinite(distance)) {
                    validScans++;
                    totalDistance += distance;
                    if (distance < minDistance) minDistance = distance;
                    if (distance > maxDistance) maxDistance = distance;
                    
                    sendReply(context, String.format("GRIDMAP_DATA: heading=%d distance=%.1f", 
                        heading, distance));
                }
                
                if (i < numScans - 1) {
                    context.getDrive().rotateDegrees(interval);
                    Thread.sleep(500);
                }
            }
            
            sendReply(context, String.format("Complete: %d/%d scans", validScans, numScans));
            
            if (validScans > 0) {
                float avgDistance = totalDistance / validScans;
                sendReply(context, String.format("Range: min=%.1f max=%.1f avg=%.1f cm", 
                    minDistance, maxDistance, avgDistance));
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendReply(context, "ERROR: Mapping interrupted");
        }
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
