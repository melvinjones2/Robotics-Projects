package client;

public class ByeCommand implements ICommand {
    public void execute(String[] args, CommandHandler context) {
        context.say("Bye!", true);
        context.sendLog("Received BYE command");
        context.getRunning().set(false);
    }
}