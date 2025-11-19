package client.network.command;

import client.config.RobotConfig;
import common.ParsedCommand;
import java.io.IOException;

/**
 * NAVSQUARE command - Navigate in a square pattern using DifferentialDrive.
 * Based on TestNavigator.java from course materials.
 * 
 * Syntax: NAVSQUARE [size] [speed]
 * - size: side length in cm (default 20)
 * - speed: optional motor speed (default: RobotConfig.COMMAND_DEFAULT_SPEED)
 * - Moves forward, rotates 90°, repeats 4 times to form square
 * 
 * Examples:
 *   NAVSQUARE             - 20cm square at default speed
 *   NAVSQUARE 30          - 30cm square at default speed
 *   NAVSQUARE 25 200      - 25cm square at speed 200
 */
public class NavSquareCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    
    public NavSquareCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVSQUARE";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        // Parse arguments
        int sizeCm = Math.round(parsedCmd.getArgAsFloat(0, 20.0f));
        int speed = parsedCmd.getArgAsInt(1, RobotConfig.COMMAND_DEFAULT_SPEED);
        
        // Check if drive is ready
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }

        sendReply(context, "Starting square pattern: " + sizeCm + " cm sides at speed " + speed);

        // Navigate square: 4 sides with 90° turns
        for (int i = 0; i < 4; i++) {
            // Move forward one side
            sendReply(context, "Side " + (i + 1) + ": moving forward " + sizeCm + " cm");
            context.getDrive().moveForwardCm(sizeCm, speed);
            
            // Pause briefly between movements
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendReply(context, "ERROR: Square pattern interrupted");
                return;
            }
            
            // Rotate 90 degrees (counterclockwise)
            sendReply(context, "Side " + (i + 1) + ": rotating 90 degrees");
            context.getDrive().rotateDegrees(90, speed);
            
            // Pause before next side
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendReply(context, "ERROR: Square pattern interrupted");
                return;
            }
        }

        sendReply(context, "Square pattern complete!");
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
