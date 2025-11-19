package client.network.command;

import common.ParsedCommand;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for command instances following the Command Pattern.
 * 
 * Maps command names to their implementations using a registry pattern.
 * Supports command aliases (e.g., "FWD", "FORWARD", "MOVE" all map to MoveCommand).
 */
public class CommandRegistry {
    
    private final Map<String, ICommand> commands = new HashMap<String, ICommand>();
    
    /**
     * Register a command with one or more names/aliases.
     * 
     * @param command Command implementation
     * @param names Command names (case-insensitive)
     */
    public void register(ICommand command, String... names) {
        for (String name : names) {
            commands.put(name.toUpperCase(), command);
        }
    }
    
    /**
     * Find a command by its parsed command object.
     * 
     * @param parsedCmd Parsed command from protocol
     * @return command implementation, or null if not found
     */
    public ICommand getCommand(ParsedCommand parsedCmd) {
        return commands.get(parsedCmd.getCommand().toUpperCase());
    }
    
    /**
     * Find a command by name.
     * 
     * @param commandName Command name
     * @return command implementation, or null if not found
     */
    public ICommand getCommand(String commandName) {
        return commands.get(commandName.toUpperCase());
    }
    
    /**
     * Check if a command is registered.
     * 
     * @param commandName Command name
     * @return true if registered
     */
    public boolean isRegistered(String commandName) {
        return commands.containsKey(commandName.toUpperCase());
    }
}
