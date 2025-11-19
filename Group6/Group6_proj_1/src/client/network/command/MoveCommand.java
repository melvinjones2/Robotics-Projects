package client.network.command;

import client.config.RobotConfig;
import common.ParsedCommand;
import java.io.IOException;

/**
 * Command to move the robot forward or backward.
 * 
 * Syntax: MOVE [speed] | FWD [speed] | BWD [speed]
 * Speed is optional, defaults to configured speed.
 */
public class MoveCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    private final boolean forward;
    
    public MoveCommand(ParsedCommand parsedCmd, boolean forward) {
        this.parsedCmd = parsedCmd;
        this.forward = forward;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        int speed = parsedCmd.getArgAsInt(0, RobotConfig.COMMAND_DEFAULT_SPEED);
        context.getDrive().move(forward, speed);
    }
    
    @Override
    public String getName() {
        return forward ? "MOVE_FORWARD" : "MOVE_BACKWARD";
    }
}
