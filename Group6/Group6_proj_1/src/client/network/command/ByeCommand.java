package client.network.command;

import java.io.IOException;

/**
 * Command to terminate the connection and shut down the robot.
 * 
 * Syntax: BYE
 */
public class ByeCommand implements ICommand {
    
    @Override
    public void execute(CommandContext context) throws IOException {
        context.getOut().write("BYE:\n");
        context.getOut().flush();
        context.getRunning().set(false);
    }
    
    @Override
    public String getName() {
        return "BYE";
    }
}
