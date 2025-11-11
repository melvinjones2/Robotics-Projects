package client.commands.movement;

import client.commands.BaseCommand;
import client.motor.MovementExecutor;
import client.motor.MotorFactory;
import client.network.CommandHandler;
import lejos.hardware.motor.BaseRegulatedMotor;

/**
 * Enhanced stop command with emergency stop capability.
 * 
 * Usage:
 *   STOP              - Stop all motors
 *   STOP <port>       - Stop specific motor
 *   STOP EMERGENCY    - Emergency stop (immediate)
 */
public class StopCommand extends BaseCommand {
    
    /**
     * Register this command with the registry.
     */
    public static void register(client.commands.CommandRegistry registry) {
        registry.register(new client.commands.CommandMetadata(
            "STOP", new StopCommand(), "Movement",
            "Stop all motors or specific motor"
        ));
    }
    public void execute(String[] args, CommandHandler context) {
        // Set command queue reference for interrupt checking
        MovementExecutor.setCommandQueue(context.getCommandQueue());
        
        if (args.length == 1) {
            MovementExecutor.stopAll();
            feedback(context, "Motors stopped");
        } else if (args.length == 2) {
            String param = args[1].toUpperCase();
            
            if (param.equals("EMERGENCY") || param.equals("E") || param.equals("!")) {
                MovementExecutor.emergencyStop();
                feedback(context, "EMERGENCY STOP!", true);
            } else {
                char port = args[1].charAt(0);
                stopSingleMotor(port);
                feedback(context, "Motor " + port + " stopped");
            }
        } else {
            usage(context, "STOP [port|EMERGENCY]");
        }
    }
    
    private void stopSingleMotor(char port) {
        BaseRegulatedMotor motor = MotorFactory.getMotor(port);
        if (motor != null) {
            try {
                motor.stop(false);
            } catch (Exception e) {
                // Handle motor error gracefully
            }
        }
    }
}