package client;

/**
 * Enhanced stop command with emergency stop capability.
 * 
 * Usage:
 *   STOP              - Stop all motors
 *   STOP <port>       - Stop specific motor
 *   STOP EMERGENCY    - Emergency stop (immediate)
 */
public class StopCommand extends BaseCommand {
    public void execute(String[] args, CommandHandler context) {
        if (args.length == 1) {
            MotorController.stopAll();
            feedback(context, "Motors stopped");
        } else if (args.length == 2) {
            String param = args[1].toUpperCase();
            
            if (param.equals("EMERGENCY") || param.equals("E") || param.equals("!")) {
                MovementExecutor.emergencyStop();
                feedback(context, "EMERGENCY STOP!", true);
            } else {
                char port = args[1].charAt(0);
                MotorController.stop(port);
                feedback(context, "Motor " + port + " stopped");
            }
        } else {
            usage(context, "STOP [port|EMERGENCY]");
        }
    }
}