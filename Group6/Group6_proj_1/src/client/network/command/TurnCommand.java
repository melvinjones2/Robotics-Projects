package client.network.command;

import client.config.RobotConfig;
import java.io.IOException;

/**
 * Command to turn the robot in place (left or right).
 * 
 * Syntax: LEFT | RIGHT
 */
public class TurnCommand implements ICommand {
    
    private final boolean turnLeft;
    
    public TurnCommand(boolean turnLeft) {
        this.turnLeft = turnLeft;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        context.getDrive().turnInPlace(turnLeft, RobotConfig.COMMAND_TURN_SPEED);
    }
    
    @Override
    public String getName() {
        return turnLeft ? "TURN_LEFT" : "TURN_RIGHT";
    }
}
