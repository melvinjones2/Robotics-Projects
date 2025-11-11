package client.commands.movement;

import client.commands.BaseCommand;
import client.commands.CommandParser;
import client.motor.MovementExecutor;
import client.motor.MovementParameters;
import client.motor.MovementParameters.Direction;
import client.network.CommandHandler;

/**
 * Unified movement command that handles all directional movements and rotation.
 * Supports: MOVE, BWD (backward), LEFT, RIGHT, ROTATE with flexible parameters.
 * 
 * Usage examples:
 *   MOVE 300                    - Move all motors forward at speed 300
 *   MOVE A 400                  - Move motor A forward at speed 400
 *   MOVE A 400 360              - Move motor A forward at speed 400 for 360 degrees
 *   BWD 200                     - Move all motors backward at speed 200
 *   BWD B 250                   - Move motor B backward at speed 250
 *   LEFT 300                    - Turn left at speed 300
 *   RIGHT 300                   - Turn right at speed 300
 *   ROTATE 90                   - Rotate default motor (A) by 90 degrees
 *   ROTATE A 180                - Rotate motor A by 180 degrees
 */
public class UnifiedMoveCommand extends BaseCommand {
    
    /**
     * Register this command with the registry.
     */
    public static void register(client.commands.CommandRegistry registry) {
        UnifiedMoveCommand cmd = new UnifiedMoveCommand();
        
        // Register MOVE with aliases
        registry.register(new client.commands.CommandMetadata(
            "MOVE", cmd, "Movement", 
            "Move motors forward",
            "FWD", "FORWARD"
        ));
        
        // Register BWD with aliases
        registry.register(new client.commands.CommandMetadata(
            "BWD", cmd, "Movement",
            "Move motors backward",
            "BACKWARD", "BACK"
        ));
        
        // Register LEFT/RIGHT
        registry.register(new client.commands.CommandMetadata(
            "LEFT", cmd, "Movement",
            "Turn left",
            "TURNLEFT"
        ));
        
        registry.register(new client.commands.CommandMetadata(
            "RIGHT", cmd, "Movement",
            "Turn right",
            "TURNRIGHT"
        ));
        
        // Register ROTATE
        registry.register(new client.commands.CommandMetadata(
            "ROTATE", cmd, "Movement",
            "Rotate motor by angle",
            "ROT"
        ));
    }
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (args.length < 2) {
            showUsage(context);
            return;
        }
        
        try {
            // Check if this is a ROTATE command
            if (isRotateCommand(args[0])) {
                executeRotate(args, context);
                return;
            }
            
            Direction direction = parseDirection(args[0]);
            MovementParameters.Builder builder = new MovementParameters.Builder()
                .direction(direction);
            
            switch (args.length) {
                case 2:
                    parseSpeedOnly(args, builder, context);
                    break;
                case 3:
                    parsePortAndSpeed(args, builder, context);
                    break;
                case 4:
                    parsePortSpeedDistance(args, builder, context);
                    break;
                default:
                    showUsage(context);
                    return;
            }
            
            MovementParameters params = builder.build();
            
            // Set command queue reference for interrupt checking
            MovementExecutor.setCommandQueue(context.getCommandQueue());
            MovementExecutor.execute(params);
            
            String feedbackMsg = generateFeedback(params, direction);
            feedback(context, feedbackMsg);
            
        } catch (IllegalArgumentException e) {
            error(context, e.getMessage());
        } catch (Exception e) {
            // Catch any other exceptions to debug
            error(context, "Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }
    
    private boolean isRotateCommand(String command) {
        String cmd = command.toUpperCase();
        return cmd.equals("ROTATE") || cmd.equals("ROT");
    }
    
    private void executeRotate(String[] args, CommandHandler context) {
        char port;
        int angle;
        
        if (args.length == 2) {
            // ROTATE <angle> - default to port A (arm motor)
            port = 'A';
            angle = CommandParser.parseInt(args[1], "angle");
        } else if (args.length == 3) {
            // ROTATE <port> <angle>
            port = CommandParser.parsePort(args[1]);
            angle = CommandParser.parseInt(args[2], "angle");
        } else {
            error(context, "Usage: ROTATE <angle> or ROTATE <port> <angle>");
            return;
        }
        
        // Set command queue reference for interrupt checking
        MovementExecutor.setCommandQueue(context.getCommandQueue());
        MovementExecutor.rotate(port, angle);
        
        feedback(context, "Motor " + port + " rotated " + angle + "°");
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
        return Direction.FORWARD;
    }
    
    private void parseSpeedOnly(String[] args, MovementParameters.Builder builder, 
                                CommandHandler context) {
        int speed = CommandParser.parseInt(args[1], "speed");
        builder.speed(speed).port('*');
    }
    
    private void parsePortAndSpeed(String[] args, MovementParameters.Builder builder,
                                   CommandHandler context) {
        char port = args[1].charAt(0);
        int speed = CommandParser.parseInt(args[2], "speed");
        builder.port(port).speed(speed);
    }
    
    private void parsePortSpeedDistance(String[] args, MovementParameters.Builder builder,
                                       CommandHandler context) {
        char port = args[1].charAt(0);
        int speed = CommandParser.parseInt(args[2], "speed");
        int distance = CommandParser.parseInt(args[3], "distance");
        builder.port(port).speed(speed).distance(distance).immediateReturn(false);
    }
    
    private String generateFeedback(MovementParameters params, Direction direction) {
        StringBuilder fb = new StringBuilder();
        
        switch (direction) {
            case FORWARD: fb.append("Moving forward"); break;
            case BACKWARD: fb.append("Moving backward"); break;
            case LEFT: fb.append("Turning left"); break;
            case RIGHT: fb.append("Turning right"); break;
        }
        
        if (params.isAllMotors()) {
            fb.append(" (all motors)");
        } else {
            fb.append(" (motor ").append(params.getPort()).append(")");
        }
        
        fb.append(" at speed ").append(params.getSpeed());
        
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
        context.say("  ROTATE <angle>            - Rotate motor A", false);
        context.say("  ROTATE <port> <angle>     - Rotate specific motor", false);
    }
}
