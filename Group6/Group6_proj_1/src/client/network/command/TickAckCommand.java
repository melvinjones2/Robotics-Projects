package client.network.command;

import java.io.IOException;

/**
 * No-op command for TICK_ACK (heartbeat acknowledgment).
 * 
 * Syntax: TICK_ACK
 */
public class TickAckCommand implements ICommand {
    
    @Override
    public void execute(CommandContext context) throws IOException {
        // No action needed - just acknowledgment
    }
    
    @Override
    public String getName() {
        return "TICK_ACK";
    }
}
