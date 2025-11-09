package client;

/**
 * Enhanced stop command with emergency stop capability.
 * 
 * Usage:
 *   STOP              - Stop all motors
 *   STOP <port>       - Stop specific motor
 *   STOP EMERGENCY    - Emergency stop (immediate)
 */
public class StopCommand implements ICommand {
    public void execute(String[] args, CommandHandler context) {
        if (args.length == 1) {
            // Regular stop all
            MotorController.stopAll();
            context.say("Motors stopped", false);
            context.sendLog("Stopped all motors");
        } else if (args.length == 2) {
            String param = args[1].toUpperCase();
            
            // Check for emergency stop
            if (param.equals("EMERGENCY") || param.equals("E") || param.equals("!")) {
                MovementExecutor.emergencyStop();
                context.say("EMERGENCY STOP!", true);
                context.sendLog("Emergency stop executed");
            } else {
                // Single motor stop
                char port = args[1].charAt(0);
                MotorController.stop(port);
                context.say("Motor " + port + " stopped", false);
                context.sendLog("Stopped motor " + port);
            }
        } else {
            context.say("Usage: STOP [port|EMERGENCY]", false);
        }
    }
}