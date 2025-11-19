package client.network.command;

import java.io.IOException;

/**
 * Command interface following the Command Pattern.
 * 
 * Each command encapsulates a specific robot action (move, turn, scan, etc.)
 * and can be executed independently.
 * 
 * Benefits:
 * - Decouples command parsing from execution
 * - Easy to add new commands without modifying CommandHandler
 * - Commands can be queued, logged, or undone
 * - Testable in isolation
 */
public interface ICommand {
    
    /**
     * Execute the command.
     * 
     * @param context Execution context containing robot hardware references
     * @throws IOException if communication with server fails
     */
    void execute(CommandContext context) throws IOException;
    
    /**
     * Get the command name (for logging/debugging).
     * 
     * @return command name
     */
    String getName();
}
