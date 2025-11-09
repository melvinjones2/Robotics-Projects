package client.commands.movement;

import client.commands.CommandParser;
import client.commands.ICommand;
import client.motor.MotorController;
import client.network.CommandHandler;

// Legacy command - use UnifiedMoveCommand instead
public class MoveCommand implements ICommand {

    @Override
    public void execute(String[] args, CommandHandler context) {
        try {
            switch (args.length) {
                case 2:
                    // MOVE <speed>
                    int speed = CommandParser.parseSpeed(args[1]);
                    MotorController.moveAllForward(speed);
                    context.say("Motors moving at " + speed, false);
                    context.sendLog("Move all motors at speed " + speed);
                    break;
                    
                case 3:
                    // MOVE <port> <speed>
                    char port = CommandParser.parsePort(args[1]);
                    speed = CommandParser.parseSpeed(args[2]);
                    MotorController.moveForward(port, speed);
                    context.say("Motor " + port + " moving at " + speed, false);
                    context.sendLog("Move motor " + port + " at speed " + speed);
                    break;
                    
                default:
                    context.say("Usage: MOVE <speed> or MOVE <port> <speed>", false);
                    break;
            }
        } catch (IllegalArgumentException e) {
            context.say("Error: " + e.getMessage(), false);
        }
    }
}
