package shared;

public class CommandBuilder {
	
	// Command types
	public static final String MOVE = "MOVE";
	public static final String ROTATE = "ROTATE";
	public static final String STOP = "STOP";
	public static final String SPEED = "SPEED";
	public static final String EXIT = "EXIT";
	
	// Navigation commands
	public static final String NAV_SIMPLE = "NAV_SIMPLE";     // Simple obstacle avoidance
	public static final String NAV_GOTO = "NAV_GOTO";         // Navigate to X,Y coordinate
	public static final String NAV_DYNAMIC = "NAV_DYNAMIC";   // Dynamic obstacle avoidance
	
	// Ball tracking commands
	public static final String FIND_BALL = "FIND_BALL";       // Scan and locate ball
	public static final String APPROACH_BALL = "APPROACH_BALL"; // Move toward ball
	public static final String TRACK_BALL = "TRACK_BALL";     // Continuously track ball
	
	// Safety commands
	public static final String EMERGENCY_STOP = "EMERGENCY_STOP"; // Override all and stop
	public static final String CLEAR_STOP = "CLEAR_STOP";         // Clear emergency stop
	
	// Build command with single parameter
	public static String build(String command, double value) {
		return command + " " + value;
	}
	
	// Build command with two parameters (e.g., X,Y coordinates)
	public static String build(String command, double value1, double value2) {
		return command + " " + value1 + " " + value2;
	}
	
	// Build command with no parameters
	public static String build(String command) {
		return command;
	}
	
	// Parse command - returns array [command, param1, param2, ...]
	public static String[] parse(String commandString) {
		if (commandString == null || commandString.trim().isEmpty()) {
			return new String[0];
		}
		return commandString.trim().split("\\s+");
	}
	
	// Get command type (first word)
	public static String getCommand(String commandString) {
		String[] parts = parse(commandString);
		return parts.length > 0 ? parts[0].toUpperCase() : "";
	}
	
	// Get parameter at index
	public static String getParameter(String commandString, int index) {
		String[] parts = parse(commandString);
		return (parts.length > index + 1) ? parts[index + 1] : null;
	}
	
	// Get parameter as double
	public static Double getParameterAsDouble(String commandString, int index) {
		String param = getParameter(commandString, index);
		if (param == null) {
			return null;
		}
		try {
			return Double.parseDouble(param);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	// Check if command has required number of parameters
	public static boolean hasParameters(String commandString, int count) {
		String[] parts = parse(commandString);
		return parts.length > count;
	}
}
