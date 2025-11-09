package client.commands.movement;

import client.commands.CommandParser;
import client.commands.ICommand;
import client.motor.MotorController;
import client.network.CommandHandler;

// Legacy command - use UnifiedMoveCommand instead
public class BackwardCommand implements ICommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        try {
            if (args.length == 2) {
                // BWD <speed>
                int speed = CommandParser.parseSpeed(args[1]);
                MotorController.moveAllBackward(speed);
                context.say("All motors backward at " + speed, false);
                context.sendLog("Backward all motors at speed " + speed);
            } else if (args.length == 3) {
                // BWD <port> <speed>
                char port = CommandParser.parsePort(args[1]);
                int speed = CommandParser.parseSpeed(args[2]);
                MotorController.moveBackward(port, speed);
                context.say("Motor " + port + " backward at " + speed, false);
                context.sendLog("Backward motor " + port + " at speed " + speed);
            } else {
                context.say("Usage: BWD <speed> or BWD <port> <speed>", false);
            }
        } catch (IllegalArgumentException e) {
            context.say("Error: " + e.getMessage(), false);
        }
    }
}