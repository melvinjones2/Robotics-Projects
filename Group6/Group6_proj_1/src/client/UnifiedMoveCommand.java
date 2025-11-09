package client;

import client.MovementParameters.Direction;

/**
 * Unified movement command that handles all directional movements.
 * Supports: MOVE, BWD (backward), LEFT, RIGHT with flexible parameters.
 * 
 * Usage examples:
 *   MOVE 300                    - Move all motors forward at speed 300
 *   MOVE A 400                  - Move motor A forward at speed 400
 *   MOVE A 400 360              - Move motor A forward at speed 400 for 360 degrees
 *   BWD 200                     - Move all motors backward at speed 200
 *   BWD B 250                   - Move motor B backward at speed 250
 *   LEFT 300                    - Turn left at speed 300
 *   RIGHT 300                   - Turn right at speed 300
 */
public class UnifiedMoveCommand implements ICommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (args.length < 2) {
            showUsage(context);
            return;
        }
        
        try {
            // Determine direction from command
            Direction direction = parseDirection(args[0]);
            MovementParameters.Builder builder = new MovementParameters.Builder()
                .direction(direction);
            
            // Parse arguments based on count
            switch (args.length) {
                case 2:
                    // MOVE <speed> - all motors at speed
                    parseSpeedOnly(args, builder, context);
                    break;
                    
                case 3:
                    // MOVE <port> <speed> - single motor at speed
                    parsePortAndSpeed(args, builder, context);
                    break;
                    
                case 4:
                    // MOVE <port> <speed> <distance> - single motor at speed for distance
                    parsePortSpeedDistance(args, builder, context);
                    break;
                    
                default:
                    showUsage(context);
                    return;
            }
            
            // Execute the movement
            MovementParameters params = builder.build();
            MovementExecutor.execute(params);
            
            // Provide feedback
            String feedback = generateFeedback(params, direction);
            context.say(feedback, false);
            context.sendLog(feedback);
            
        } catch (IllegalArgumentException e) {
            context.say("Error: " + e.getMessage(), false);
        }
    }
    
    private Direction parseDirection(String command) {
        String cmd = command.toUpperCase();
        if (cmd.equals("MOVE") || cmd.equals("FWD") || cmd.equals("FORWARD")) {
            return Direction.FORWARD;
        } else if (cmd.equals("BWD") || cmd.equals("BACKWARD") || cmd.equals("BACK")) {
            return Direction.BACKWARD;
        } else if (cmd.equals("LEFT") || cmd.equals("TURNLEFT")) {
            return Direction.LEFT;
        } else if (cmd.equals("RIGHT") || cmd.equals("TURNRIGHT")) {
            return Direction.RIGHT;
        }
        return Direction.FORWARD; // Default
    }
    
    private void parseSpeedOnly(String[] args, MovementParameters.Builder builder, 
                                CommandHandler context) {
        int speed = parseInteger(args[1], "speed", context);
        builder.speed(speed).port('*'); // All motors
    }
    
    private void parsePortAndSpeed(String[] args, MovementParameters.Builder builder,
                                   CommandHandler context) {
        char port = args[1].charAt(0);
        int speed = parseInteger(args[2], "speed", context);
        builder.port(port).speed(speed);
    }
    
    private void parsePortSpeedDistance(String[] args, MovementParameters.Builder builder,
                                       CommandHandler context) {
        char port = args[1].charAt(0);
        int speed = parseInteger(args[2], "speed", context);
        int distance = parseInteger(args[3], "distance", context);
        builder.port(port).speed(speed).distance(distance).immediateReturn(false);
    }
    
    private int parseInteger(String value, String paramName, CommandHandler context) {
        return CommandParser.parseInt(value, paramName);
    }
    
    private String generateFeedback(MovementParameters params, Direction direction) {
        StringBuilder fb = new StringBuilder();
        
        // Direction
        switch (direction) {
            case FORWARD: fb.append("Moving forward"); break;
            case BACKWARD: fb.append("Moving backward"); break;
            case LEFT: fb.append("Turning left"); break;
            case RIGHT: fb.append("Turning right"); break;
        }
        
        // Target
        if (params.isAllMotors()) {
            fb.append(" (all motors)");
        } else {
            fb.append(" (motor ").append(params.getPort()).append(")");
        }
        
        // Speed
        fb.append(" at speed ").append(params.getSpeed());
        
        // Distance
        if (params.getDistance() > 0) {
            fb.append(" for ").append(params.getDistance()).append("°");
        }
        
        return fb.toString();
    }
    
    private void showUsage(CommandHandler context) {
        context.say("Usage:", false);
        context.say("  MOVE <speed>              - All motors forward", false);
        context.say("  MOVE <port> <speed>       - Single motor forward", false);
        context.say("  MOVE <port> <speed> <deg> - Motor for degrees", false);
        context.say("  BWD <speed>               - All motors backward", false);
        context.say("  LEFT/RIGHT <speed>        - Turn", false);
    }
}
