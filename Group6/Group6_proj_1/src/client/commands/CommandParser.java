package client.commands;

import client.config.RobotConfig;

public class CommandParser {
    
    // Parse and normalize command string
    public static ParsedCommand parse(String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return new ParsedCommand("", new String[0], -1);
        }
        
        String msg = rawCommand.trim();
        
        // Extract tick/frame suffix if present (e.g., ":123")
        int frameNumber = -1;
        int colonIdx = msg.lastIndexOf(':');
        if (colonIdx > 0 && colonIdx < msg.length() - 1) {
            String possibleTick = msg.substring(colonIdx + 1);
            try {
                frameNumber = Integer.parseInt(possibleTick);
                msg = msg.substring(0, colonIdx).trim();
            } catch (NumberFormatException ignored) {
                // Not a frame number, keep original
            }
        }
        
        // Normalize whitespace
        msg = msg.replaceAll("\\s+", " ");
        
        // Split into parts
        String[] parts = msg.split(" ");
        String command = parts.length > 0 ? parts[0].toUpperCase() : "";
        
        // Handle special commands like SET_DEBUG:1
        if (command.contains(":")) {
            String[] cmdParts = command.split(":", 2);
            command = cmdParts[0];
            // Reconstruct parts with the value
            String[] newParts = new String[parts.length + 1];
            newParts[0] = command;
            newParts[1] = cmdParts[1];
            System.arraycopy(parts, 1, newParts, 2, parts.length - 1);
            parts = newParts;
        }
        
        return new ParsedCommand(command, parts, frameNumber);
    }
    
    // Parse integer parameter with validation
    public static int parseInt(String value, String paramName) throws IllegalArgumentException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + paramName + ": " + value);
        }
    }
    
    // Parse motor port with validation
    public static char parsePort(String value) throws IllegalArgumentException {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Port cannot be empty");
        }
        
        char port = Character.toUpperCase(value.charAt(0));
        if (port != 'A' && port != 'B' && port != 'C' && port != 'D' && port != '*') {
            throw new IllegalArgumentException("Invalid port: " + value);
        }
        
        return port;
    }
    
    // Validate speed parameter
    public static int parseSpeed(String value) throws IllegalArgumentException {
        int speed = parseInt(value, "speed");
        if (speed < RobotConfig.MIN_MOTOR_SPEED || speed > RobotConfig.MAX_MOTOR_SPEED) {
            throw new IllegalArgumentException("Speed must be between " + 
                RobotConfig.MIN_MOTOR_SPEED + " and " + RobotConfig.MAX_MOTOR_SPEED);
        }
        return speed;
    }
    
    // Holds parsed command information
    public static class ParsedCommand {
        private final String command;
        private final String[] args;
        private final int frameNumber;
        
        public ParsedCommand(String command, String[] args, int frameNumber) {
            this.command = command;
            this.args = args;
            this.frameNumber = frameNumber;
        }
        
        public String getCommand() { return command; }
        public String[] getArgs() { return args; }
        public int getFrameNumber() { return frameNumber; }
        public boolean hasFrame() { return frameNumber >= 0; }
        public int getArgCount() { return args.length; }
        
        public String getArg(int index) {
            return index < args.length ? args[index] : null;
        }
    }
}
