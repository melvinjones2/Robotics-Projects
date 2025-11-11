package client.commands.system;

import client.commands.BaseCommand;
import client.network.CommandHandler;

public class LogCommand extends BaseCommand {
    
    /**
     * Register this command with the registry.
     */
    public static void register(client.commands.CommandRegistry registry) {
        registry.register(new client.commands.CommandMetadata(
            "LOG", new LogCommand(), "System",
            "Send log message to server"
        ));
    }

    @Override
    public void execute(String[] args, CommandHandler context) {
        if (args.length < 2) {
            usage(context, "LOG <message>");
            return;
        }
        
        // Reconstruct message from all args
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) message.append(" ");
            message.append(args[i]);
        }
        
        String msg = message.toString();
        feedback(context, "Logged: " + msg);
    }
}
