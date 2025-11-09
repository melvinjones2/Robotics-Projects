package client;

public class ByeCommand extends BaseCommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        feedback(context, "Bye! Disconnecting...", true);
        context.getRunning().set(false);
    }
}