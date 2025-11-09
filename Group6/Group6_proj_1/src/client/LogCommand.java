package client;

public class LogCommand implements ICommand {

    @Override
    public void execute(String[] args, CommandHandler context) {
        // LOG <message> - send a log message
        if (args.length < 2) {
            context.say("Usage: LOG <message>", false);
            return;
        }
        
        // Reconstruct message from all args after LOG
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) message.append(" ");
            message.append(args[i]);
        }
        
        String msg = message.toString();
        context.sendLog("Client log: " + msg);
        context.say("Logged: " + msg, false);
    }
}
