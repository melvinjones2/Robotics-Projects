package client.commands.system;

import client.commands.BaseCommand;
import client.network.CommandHandler;

public class ByeCommand extends BaseCommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        feedback(context, "Bye! Disconnecting...", true);
        context.getRunning().set(false);
    }
}