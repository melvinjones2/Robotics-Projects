package client;

public class StopCommand implements Command {
    public void execute(String[] args, CommandHandler context) {
        if (args.length == 1) {
            MotorController.stopAll();
            context.say("Motors stopped", false);
            context.sendLog("Stopped all motors");
        } else if (args.length == 2) {
            char port = args[1].charAt(0);
            MotorController.stop(port);
            context.say("Motor " + port + " stopped", false);
            context.sendLog("Stopped motor " + port);
        }
    }
}