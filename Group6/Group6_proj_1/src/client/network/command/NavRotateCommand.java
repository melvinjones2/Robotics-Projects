package client.network.command;

import common.ParsedCommand;
import java.io.IOException;

// Precise rotation. Syntax: NAVROTATE <angle>
public class NavRotateCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    
    public NavRotateCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVROTATE";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        // Parse angle argument (required)
        float angleF = parsedCmd.getArgAsFloat(0, Float.NaN);
        if (Float.isNaN(angleF)) {
            sendReply(context, "ERROR: NAVROTATE requires angle parameter");
            sendReply(context, "Usage: NAVROTATE <angle>");
            return;
        }
        int angle = Math.round(angleF);
        
        // Check if drive is ready
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }

        sendReply(context, "Starting rotation: " + angle + " degrees");

        // Execute rotation (blocking)
        long startTime = System.currentTimeMillis();
        context.getDrive().rotateDegrees(angle);
        long endTime = System.currentTimeMillis();
        
        double duration = (endTime - startTime) / 1000.0;
        double actualSpeed = Math.abs(angle) / duration;

        sendReply(context, String.format("Rotation complete: %d deg in %.2f sec (%.1f deg/s)", 
            angle, duration, actualSpeed));
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
