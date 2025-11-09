package client;

public class SetDebugCommand implements ICommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (args.length < 2) {
            context.say("Usage: SET_DEBUG <0|1>", false);
            return;
        }
        
        String value = args[1].trim();
        boolean debugMode = "1".equals(value) || "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value);
        
        context.setDebug(debugMode);
        context.say("Debug: " + (debugMode ? "ON" : "OFF"), false);
        context.sendLog("Debug mode set to " + context.isDebug());
    }
}