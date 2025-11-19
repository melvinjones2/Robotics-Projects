package client.network.command;

import common.ParsedCommand;

public class CommandFactory {
    
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
    
    public static ICommand createCommand(ParsedCommand parsedCmd) {
        String cmdName = parsedCmd.getCommand().toUpperCase();
        
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
                
            case "NAVSQUARE":
                return new NavSquareCommand(parsedCmd);
                
            case "NAVROTATE":
                return new NavRotateCommand(parsedCmd);
                
            case "NAVTRAVEL":
                return new NavTravelCommand(parsedCmd);
                
            case "NAVLINEMAP":
                return new NavLineMapCommand(parsedCmd);
                
            case "NAVGRIDMAP":
                return new NavGridMapCommand(parsedCmd);
                
            case "NAVBALL":
                return new NavBallCommand(parsedCmd);
                
            case "LOADCUSTOMMAP":
                return new LoadCustomMapCommand(parsedCmd);
                
            case "NAVCUSTOM":
                return new NavCustomMapCommand(parsedCmd);
                
            default:
                return null;
        }
    }
}
