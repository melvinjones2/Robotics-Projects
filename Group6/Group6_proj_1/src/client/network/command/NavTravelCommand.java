package client.network.command;

import client.config.RobotConfig;
import common.ParsedCommand;
import java.io.IOException;

// Travel a precise distance. Syntax: NAVTRAVEL <distance> [speed]
public class NavTravelCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    
    public NavTravelCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVTRAVEL";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        // Parse distance argument (required)
        float distanceF = parsedCmd.getArgAsFloat(0, Float.NaN);
        if (Float.isNaN(distanceF)) {
            sendReply(context, "ERROR: NAVTRAVEL requires distance parameter");
            sendReply(context, "Usage: NAVTRAVEL <distance> [speed]");
            return;
        }
        int distanceCm = Math.round(distanceF);
        
        // Optional speed parameter
        int speed = parsedCmd.getArgAsInt(1, RobotConfig.COMMAND_DEFAULT_SPEED);
        
        // Check if drive is ready
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }

        sendReply(context, "Starting travel: " + distanceCm + " cm at speed " + speed);

        // Execute travel (blocking)
        long startTime = System.currentTimeMillis();
        
        if (distanceCm >= 0) {
            context.getDrive().moveForwardCm(distanceCm, speed);
        } else {
            context.getDrive().moveBackwardCm(-distanceCm, speed);
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        double actualSpeed = Math.abs(distanceCm) / duration;

        sendReply(context, String.format("Travel complete: %d cm in %.2f sec (%.1f cm/s)", 
            distanceCm, duration, actualSpeed));
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
