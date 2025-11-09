/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client;

public class RotateMotorCommand implements ICommand {

    @Override
    public void execute(String[] args, CommandHandler context) {
        try {
            if (args.length == 2) {
                // ROTATE <angle> - default to port D
                int angle = CommandParser.parseInt(args[1], "angle");
                char port = 'D';
                MotorController.rotateArm(port, angle);
                context.say("Motor " + port + " rotated by " + angle + " degrees", false);
                context.sendLog("Rotate motor " + port + " by " + angle + " degrees");
                return;
            }

            if (args.length != 3) {
                context.say("Usage: ROTATE <angle> or ROTATE <port> <angle>", false);
                return;
            }

            // ROTATE <port> <angle>
            char port = CommandParser.parsePort(args[1]);
            int angle = CommandParser.parseInt(args[2], "angle");
            
            MotorController.rotateArm(port, angle);
            context.say("Motor " + port + " rotated by " + angle + " degrees", false);
            context.sendLog("Rotate motor " + port + " by " + angle + " degrees");
        } catch (IllegalArgumentException e) {
            context.say("Error: " + e.getMessage(), false);
        }
    }
}
