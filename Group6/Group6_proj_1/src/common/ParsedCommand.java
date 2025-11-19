package common;

/**
 * Represents a parsed command with its arguments.
 * Provides type-safe access to command components.
 */
public class ParsedCommand {
    
    private final String command;
    private final String[] args;
    
    public ParsedCommand(String command, String[] args) {
        this.command = command != null ? command.toUpperCase() : "";
        this.args = args != null ? args : new String[0];
    }
    
    /**
     * Get the command name (uppercase).
     */
    public String getCommand() {
        return command;
    }
    
    /**
     * Get all command arguments.
     */
    public String[] getArgs() {
        return args;
    }
    
    /**
     * Get number of arguments.
     */
    public int getArgCount() {
        return args.length;
    }
    
    /**
     * Get specific argument by index.
     */
    public String getArg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }
    
    /**
     * Get argument as integer.
     */
    public int getArgAsInt(int index, int defaultValue) {
        String arg = getArg(index);
        if (arg == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get argument as float.
     */
    public float getArgAsFloat(int index, float defaultValue) {
        String arg = getArg(index);
        if (arg == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(arg);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Check if argument is a single character (for motor ports).
     */
    public boolean isArgChar(int index) {
        String arg = getArg(index);
        return arg != null && arg.length() == 1 && Character.isLetter(arg.charAt(0));
    }
    
    /**
     * Get argument as character (for motor ports).
     */
    public char getArgAsChar(int index) {
        String arg = getArg(index);
        return (arg != null && arg.length() == 1) ? arg.charAt(0) : '\0';
    }
    
    /**
     * Check if command matches given name.
     */
    public boolean is(String commandName) {
        return command.equalsIgnoreCase(commandName);
    }
    
    /**
     * Check if command matches any of the given names.
     */
    public boolean isAnyOf(String... commandNames) {
        for (String name : commandNames) {
            if (command.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(command);
        for (String arg : args) {
            sb.append(' ').append(arg);
        }
        return sb.toString();
    }
}
