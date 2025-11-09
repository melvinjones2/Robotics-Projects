package client.commands.movement;

import client.commands.BaseCommand;
import client.commands.CommandParser;
import client.motor.MotorController;
import client.network.CommandHandler;

public class RotateMotorCommand extends BaseCommand {

    @Override
    public void execute(String[] args, CommandHandler context) {
        try {
            char port;
            int angle;
            
            if (args.length == 2) {
                // ROTATE <angle> - default to port D
                port = 'D';
                angle = CommandParser.parseInt(args[1], "angle");
            } else if (args.length == 3) {
                // ROTATE <port> <angle>
                port = CommandParser.parsePort(args[1]);
                angle = CommandParser.parseInt(args[2], "angle");
            } else {
                usage(context, "ROTATE <angle> or ROTATE <port> <angle>");
                return;
            }
            
            MotorController.rotateArm(port, angle);
            feedback(context, "Motor " + port + " rotated " + angle + "°");
            
        } catch (IllegalArgumentException e) {
            error(context, e.getMessage());
        }
    }
}
