package client.commands;

import client.network.CommandHandler;
import client.util.ClientLogger;

import java.io.IOException;

// Base class for commands with common utility methods
public abstract class BaseCommand implements ICommand {
    
    // Send feedback to display and log
    protected void feedback(CommandHandler context, String message, boolean beep) {
        context.say(message, beep);
        ClientLogger.info(message);
    }
    
    // Send feedback without beep
    protected void feedback(CommandHandler context, String message) {
        feedback(context, message, false);
    }
    
    // Send error message
    protected void error(CommandHandler context, String errorMessage) {
        context.say("Error: " + errorMessage, false);
        ClientLogger.error(errorMessage);
    }
    
    // Show usage message
    protected void usage(CommandHandler context, String usageMessage) {
        context.say("Usage: " + usageMessage, false);
        ClientLogger.debug("Usage requested: " + usageMessage);
    }
    
    // Send data to server
    protected void sendToServer(CommandHandler context, String data) {
        try {
            context.send(context.getOut(), data);
            ClientLogger.debug("Sent to server: " + data);
        } catch (IOException e) {
            error(context, "Send failed: " + e.getMessage());
            ClientLogger.error("Send failed", e);
        }
    }
    
    // Validate argument count
    protected boolean validateArgCount(CommandHandler context, String[] args, 
                                      int expected, String usageMsg) {
        if (args.length != expected) {
            usage(context, usageMsg);
            ClientLogger.warn(String.format("Invalid arg count: expected %d, got %d", 
                expected, args.length));
            return false;
        }
        return true;
    }
    
    // Validate argument count range
    protected boolean validateArgCount(CommandHandler context, String[] args, 
                                      int min, int max, String usageMsg) {
        if (args.length < min || args.length > max) {
            usage(context, usageMsg);
            ClientLogger.warn(String.format("Invalid arg count: expected %d-%d, got %d", 
                min, max, args.length));
            return false;
        }
        return true;
    }
    
    // Log command execution start
    protected void logExecutionStart(String commandName, String[] args) {
        ClientLogger.debug(String.format("Executing %s with %d args", 
            commandName, args.length));
    }
    
    // Log command execution completion with timing
    protected void logExecutionComplete(String commandName, long startTimeMs) {
        ClientLogger.logPerformance(commandName, startTimeMs);
    }
}
