package client.commands.system;

import client.commands.BaseCommand;
import client.network.CommandHandler;

public class SetDebugCommand extends BaseCommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (!validateArgCount(context, args, 2, 2, "SET_DEBUG <0|1|on|off>")) {
            return;
        }
        
        String value = args[1].trim().toLowerCase();
        boolean debugMode = "1".equals(value) || "true".equals(value) || "on".equals(value);
        
        context.setDebug(debugMode);
        feedback(context, "Debug mode: " + (debugMode ? "ON" : "OFF"));
    }
}