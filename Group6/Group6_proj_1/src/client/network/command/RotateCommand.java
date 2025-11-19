package client.network.command;

import common.ParsedCommand;

import java.io.IOException;

/**
 * Command to rotate the robot or a specific motor.
 * 
 * Syntax: ROTATE ROBOT <degrees> | ROTATE <motorPort> <degrees>
 */
public class RotateCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public RotateCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        if (parsedCmd.getArgCount() < 2) {
            return;
        }
        
        String firstArg = parsedCmd.getArg(0);
        int degrees = parsedCmd.getArgAsInt(1, 0);
        
        if ("ROBOT".equalsIgnoreCase(firstArg)) {
            context.getDrive().rotateDegrees(degrees);
        } else if (parsedCmd.isArgChar(0)) {
            char motorPort = parsedCmd.getArgAsChar(0);
            lejos.hardware.motor.BaseRegulatedMotor motor = client.motor.MotorFactory.getMotor(motorPort);
            if (motor != null) {
                motor.rotate(degrees, false);
            }
        }
    }
    
    @Override
    public String getName() {
        return "ROTATE";
    }
}
