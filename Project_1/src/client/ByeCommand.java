package client;

public class ByeCommand implements Command {

    public void execute(String[] args, CommandHandler context) {
        context.say("Bye!", true);
        context.sendLog("Received BYE command");
        context.getRunning().set(false);
    }
}
