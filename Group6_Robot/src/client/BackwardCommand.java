package client;

public class BackwardCommand implements Command {
    public void execute(String[] args, CommandHandler context) {
        if (args.length == 3) {
            char port = args[1].charAt(0);
            int speed = 200;
            try { speed = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
            MotorController.moveBackward(port, speed);
            context.say("Motor " + port + " backward at " + speed, false);
            context.sendLog("Backward motor " + port + " at speed " + speed);
        }
    }
}