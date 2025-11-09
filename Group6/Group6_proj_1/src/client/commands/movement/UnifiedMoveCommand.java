package client.commands.movement;

import client.network.CommandHandler;

import client.commands.BaseCommand;
import client.commands.CommandParser;
import client.motor.MovementExecutor;
import client.motor.MovementParameters;
import client.motor.MovementParameters.Direction;

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
public class UnifiedMoveCommand extends BaseCommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (args.length < 2) {
            showUsage(context);
            return;
        }
        
        try {
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
            MovementExecutor.execute(params);
            
            String feedbackMsg = generateFeedback(params, direction);
            feedback(context, feedbackMsg);
            
        } catch (IllegalArgumentException e) {
            error(context, e.getMessage());
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
    }
}
