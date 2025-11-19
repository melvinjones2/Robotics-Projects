package client.network.command;

import java.io.IOException;

/**
 * Each command encapsulates a specific robot action (move, turn, scan, etc.)
 * and can be executed independently.
 */
public interface ICommand {
    
    /**
     * Execute the command.
     */
    void execute(CommandContext context) throws IOException;
    
    /**
     * Get the command name (for logging/debugging).
     */
    String getName();
}
