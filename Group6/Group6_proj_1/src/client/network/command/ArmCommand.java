package client.network.command;

import client.config.RobotConfig;
import common.ParsedCommand;
import lejos.hardware.lcd.LCD;

import java.io.IOException;

/**
 * Command to control the robot's arm.
 * 
 * Syntax: ARM UP | ARM DOWN | ARM <degrees>
 */
public class ArmCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public ArmCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        if (parsedCmd.getArgCount() == 0) {
            return;
        }
        
        if (context.getArmController() != null && context.getArmController().isReady()) {
            String parameter = parsedCmd.getArg(0);
            String action;
            
            if ("UP".equalsIgnoreCase(parameter)) {
                context.getArmController().moveUp();
                action = "UP";
            } else if ("DOWN".equalsIgnoreCase(parameter)) {
                context.getArmController().moveDown();
                action = "DOWN";
            } else {
                try {
                    int targetPosition = Integer.parseInt(parameter);
                    context.getArmController().moveTo(targetPosition);
                    action = parameter + "deg";
                } catch (NumberFormatException e) {
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    LCD.drawString("ARM: Invalid", 0, RobotConfig.LCD_COMMAND_LINE);
                    return;
                }
            }
            
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("ARM: " + action, 0, RobotConfig.LCD_COMMAND_LINE);
            
            send(context, "ARM:" + action);
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("ARM: No Motor", 0, RobotConfig.LCD_COMMAND_LINE);
        }
    }
    
    @Override
    public String getName() {
        return "ARM";
    }
    
    private void send(CommandContext context, String msg) {
        try {
            context.getOut().write(msg + "\n");
            context.getOut().flush();
        } catch (IOException e) {
            // Connection failed, ignore
        }
    }
}
