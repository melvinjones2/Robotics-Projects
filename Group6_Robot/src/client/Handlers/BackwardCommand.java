package client.Handlers;

import client.MotorController;
import client.Interfaces.*;

public class BackwardCommand implements ICommand {
    public void execute(String[] args, CommandHandler context) {
        if (args.length == 2) {
            // BWD <speed> -- move all motors backward
            int speed;
            try {
                speed = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                context.say("Invalid speed: " + args[1], false);
                return;
            }
            MotorController.moveAllBackward(speed);
            context.say("All motors backward at " + speed, false);
            context.sendLog("Backward all motors at speed " + speed);
        } else if (args.length == 3) {
            // BWD <port> <speed>
            char port = args[1].charAt(0);
            int speed;
            try {
                speed = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                context.say("Invalid speed: " + args[2], false);
                return;
            }
            MotorController.moveBackward(port, speed);
            context.say("Motor " + port + " backward at " + speed, false);
            context.sendLog("Backward motor " + port + " at speed " + speed);
        } else {
            context.say("Usage: BWD <speed> or BWD <port> <speed>", false);
        }
    }
}