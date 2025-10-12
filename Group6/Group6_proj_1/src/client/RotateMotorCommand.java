/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package client;

/**
 *
 * @author bandn
 */
public class RotateMotorCommand implements ICommand {

    public void execute(String[] args, CommandHandler context) {
        if (args.length == 2) {
            try {
                int angle = Integer.parseInt(args[1]);
                char port = 'D';
                MotorController.rotateArm(port, angle);
                context.say("Motor " + port + " rotated by " + angle + " degrees", false);
                context.sendLog("Rotate motor " + port + " by " + angle + " degrees");
            } catch (NumberFormatException e) {
                context.say("Invalid angle: " + args[1], false);
            }
            return;
        }

        if (args.length != 3) {
            context.say("Usage: ROTATE <port> <angle>", false);
            return;
        }

        char port = args[1].charAt(0);
        int angle;
        try {
            angle = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            context.say("Invalid angle: " + args[2], false);
            return;
        }

        MotorController.rotateArm(port, angle);
        context.say("Motor " + port + " rotated by " + angle + " degrees", false);
        context.sendLog("Rotate motor " + port + " by " + angle + " degrees");
    }

}
