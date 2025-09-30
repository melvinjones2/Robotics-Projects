package client;

public class MoveCommand implements Command {

    public void execute(String[] args, CommandHandler context) {
        if (args.length == 2) {
            // MOVE <speed>
            int speed;
            try {
                speed = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                context.say("Invalid speed: " + args[1], false);
                return;
            }
            MotorController.moveAllForward(speed);
            context.say("Motors moving at " + speed, false);
            context.sendLog("Move all motors at speed " + speed);
        } else if (args.length == 3) {
            // MOVE <port> <speed>
            char port = args[1].charAt(0);
            int speed;
            try {
                speed = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                context.say("Invalid speed: " + args[2], false);
                return;
            }
            MotorController.moveForward(port, speed);
            context.say("Motor " + port + " moving at " + speed, false);
            context.sendLog("Move motor " + port + " at speed " + speed);
        } else {
            context.say("Usage: MOVE <speed> or MOVE <port> <speed>", false);
        }
    }
}
