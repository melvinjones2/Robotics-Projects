package client.network.command;

import client.config.RobotConfig;
import common.ParsedCommand;
import lejos.hardware.lcd.LCD;

import java.io.IOException;

/**
 * Command to set the target ball color for detection.
 * 
 * Syntax: SETCOLOR <colorId>
 * Color IDs: 1=Black, 2=Blue, 3=Green, 4=Yellow, 5=Red, 6=White
 */
public class SetColorCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public SetColorCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        if (parsedCmd.getArgCount() == 0) {
            return;
        }
        
        int colorId = parsedCmd.getArgAsInt(0, 0);
        
        if (colorId >= 1 && colorId <= 6) {
            RobotConfig.TARGET_BALL_COLOR_ID = colorId;
            
            String[] colorNames = {"", "Black", "Blue", "Green", "Yellow", "Red", "White"};
            String colorName = colorNames[colorId];
            
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("Color: " + colorName, 0, RobotConfig.LCD_COMMAND_LINE);
            
            send(context, "SETCOLOR:" + colorName);
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("Color: Invalid", 0, RobotConfig.LCD_COMMAND_LINE);
        }
    }
    
    @Override
    public String getName() {
        return "SETCOLOR";
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
