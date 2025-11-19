package client.network.command;

import common.ParsedCommand;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for command instances following the Command Pattern.
 */
public class CommandRegistry {
    
    private final Map<String, ICommand> commands = new HashMap<String, ICommand>();
    
    /**
     * Register a command with one or more names/aliases.
     */
    public void register(ICommand command, String... names) {
        for (String name : names) {
            commands.put(name.toUpperCase(), command);
        }
    }
    
    /**
     * Find a command by its parsed command object.
     */
    public ICommand getCommand(ParsedCommand parsedCmd) {
        return commands.get(parsedCmd.getCommand().toUpperCase());
    }
    
    /**
     * Find a command by name.
     */
    public ICommand getCommand(String commandName) {
        return commands.get(commandName.toUpperCase());
    }
    
    /**
     * Check if a command is registered.
     */
    public boolean isRegistered(String commandName) {
        return commands.containsKey(commandName.toUpperCase());
    }
}
