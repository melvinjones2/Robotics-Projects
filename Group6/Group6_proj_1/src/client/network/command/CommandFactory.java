package client.network.command;

import common.ParsedCommand;

/**
 * Factory for creating command instances.
 * 
 * Instantiates commands based on parsed command input.
 * Each command type gets its own factory method for flexibility.
 */
public class CommandFactory {
    
    /**
     * Initialize the command registry with all supported commands.
     * 
     * @return configured command registry
     */
    public static CommandRegistry createRegistry() {
        CommandRegistry registry = new CommandRegistry();
        
        // Register no-op commands (use singleton instances)
        registry.register(new TickAckCommand(), "TICK_ACK");
        registry.register(new ByeCommand(), "BYE");
        
        // Register turn commands (use singleton instances)
        registry.register(new TurnCommand(true), "LEFT");
        registry.register(new TurnCommand(false), "RIGHT");
        
        return registry;
    }
    
    /**
     * Create a command instance from parsed command.
     * Some commands need the parsed command for parameters.
     * 
     * @param parsedCmd Parsed command
     * @return command instance, or null if not recognized
     */
    public static ICommand createCommand(ParsedCommand parsedCmd) {
        String cmdName = parsedCmd.getCommand().toUpperCase();
        
        // Commands that need ParsedCommand for parameters
        switch (cmdName) {
            case "MOVE":
            case "FWD":
            case "FORWARD":
                return new MoveCommand(parsedCmd, true);
                
            case "BWD":
            case "BACK":
            case "BACKWARD":
                return new MoveCommand(parsedCmd, false);
                
            case "STOP":
                return new StopCommand(parsedCmd);
                
            case "ROTATE":
                return new RotateCommand(parsedCmd);
                
            case "BEEP":
                return new BeepCommand(parsedCmd);
                
            case "ARM":
                return new ArmCommand(parsedCmd);
                
            case "SCAN":
                return new ScanCommand(parsedCmd);
                
            case "AUTOSEARCH":
                return new AutoSearchCommand(parsedCmd);
                
            case "SETCOLOR":
                return new SetColorCommand(parsedCmd);
                
            case "LEFT":
                return new TurnCommand(true);
                
            case "RIGHT":
                return new TurnCommand(false);
                
            case "TICK_ACK":
                return new TickAckCommand();
                
            case "BYE":
                return new ByeCommand();
                
            // Navigation commands from course materials
            case "NAVSQUARE":
                return new NavSquareCommand(parsedCmd);
                
            case "NAVROTATE":
                return new NavRotateCommand(parsedCmd);
                
            case "NAVTRAVEL":
                return new NavTravelCommand(parsedCmd);
                
            default:
                return null;
        }
    }
}
