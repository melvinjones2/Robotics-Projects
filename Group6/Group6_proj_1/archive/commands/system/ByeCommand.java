package client.commands.system;

import client.commands.BaseCommand;
import client.network.CommandHandler;

public class ByeCommand extends BaseCommand {
    
    /**
     * Register this command with the registry.
     */
    public static void register(client.commands.CommandRegistry registry) {
        registry.register(new client.commands.CommandMetadata(
            "BYE", new ByeCommand(), "System",
            "Disconnect and shutdown"
        ));
    }
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        feedback(context, "Bye! Disconnecting...", true);
        context.getRunning().set(false);
    }
}