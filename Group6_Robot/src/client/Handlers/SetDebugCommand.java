package client.Handlers;

import client.Interfaces.ICommand;

public class SetDebugCommand implements ICommand {
    public void execute(String[] args, CommandHandler context) {
        if (args.length == 2) {
            context.setDebug("1".equals(args[1].trim()));
            context.sendLog("Debug mode set to " + context.isDebug());
        }
    }
}