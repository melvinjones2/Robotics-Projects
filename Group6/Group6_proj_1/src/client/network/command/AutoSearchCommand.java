package client.network.command;

import client.autonomous.BallSearchController;
import client.config.RobotConfig;
import client.sensor.ISensor;
import common.ParsedCommand;
import lejos.hardware.lcd.LCD;

import java.io.IOException;

// Control autonomous ball search. Syntax: AUTOSEARCH [ON|OFF|TOGGLE]
public class AutoSearchCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public AutoSearchCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        // Initialize ball search controller if needed
        if (context.getBallSearchController() == null && context.getSensors() != null) {
            ISensor ultrasonicSensor = context.findSensor("ultrasonic");
            ISensor infraredSensor = context.findSensor("infrared");
            ISensor gyroSensor = context.findSensor("gyro");
            ISensor colorSensor = context.findSensor("light");
            
            // Create controller with warehouse for thread-safe sensor access
            BallSearchController controller = new BallSearchController(
                ultrasonicSensor, infraredSensor, gyroSensor, colorSensor, 
                context.getOut(), context.getWarehouse());
            context.setBallSearchController(controller);
        }
        
        if (context.getBallSearchController() != null) {
            String mode = parsedCmd.getArgCount() > 0 ? parsedCmd.getArg(0).toUpperCase() : "TOGGLE";
            
            switch (mode) {
                case "ON":
                    context.getBallSearchController().setEnabled(true);
                    send(context, "AUTOSEARCH:ON");
                    break;
                    
                case "OFF":
                    context.getBallSearchController().setEnabled(false);
                    send(context, "AUTOSEARCH:OFF");
                    break;
                    
                case "TOGGLE":
                default:
                    context.getBallSearchController().toggle();
                    send(context, "AUTOSEARCH:" + (context.getBallSearchController().isEnabled() ? "ON" : "OFF"));
                    break;
            }
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("AUTOSEARCH: No Dist", 0, RobotConfig.LCD_COMMAND_LINE);
            send(context, "AUTOSEARCH:NO_SENSORS");
        }
    }
    
    @Override
    public String getName() {
        return "AUTOSEARCH";
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
