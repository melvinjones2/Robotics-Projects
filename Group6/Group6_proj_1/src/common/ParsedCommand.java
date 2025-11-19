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
     * @param index Argument index (0-based)
     * @return Argument value or null if index out of bounds
     */
    public String getArg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }
    
    /**
     * Get argument as integer.
     * @param index Argument index (0-based)
     * @param defaultValue Value to return if parsing fails
     * @return Parsed integer or defaultValue
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
     * @param index Argument index (0-based)
     * @param defaultValue Value to return if parsing fails
     * @return Parsed float or defaultValue
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
     * @param index Argument index (0-based)
     * @return true if argument is single letter
     */
    public boolean isArgChar(int index) {
        String arg = getArg(index);
        return arg != null && arg.length() == 1 && Character.isLetter(arg.charAt(0));
    }
    
    /**
     * Get argument as character (for motor ports).
     * @param index Argument index (0-based)
     * @return Character or '\0' if invalid
     */
    public char getArgAsChar(int index) {
        String arg = getArg(index);
        return (arg != null && arg.length() == 1) ? arg.charAt(0) : '\0';
    }
    
    /**
     * Check if command matches given name.
     * @param commandName Command to check (case-insensitive)
     * @return true if matches
     */
    public boolean is(String commandName) {
        return command.equalsIgnoreCase(commandName);
    }
    
    /**
     * Check if command matches any of the given names.
     * @param commandNames Commands to check (case-insensitive)
     * @return true if matches any
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
