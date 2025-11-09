package client;

/**
 * Command to enable/disable autonomous navigation mode.
 * Usage: AUTO <on|off>
 */
public class AutoCommand extends BaseCommand {
    
    private final AutonomousMode autoMode;
    
    public AutoCommand(AutonomousMode autoMode) {
        this.autoMode = autoMode;
    }
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (autoMode == null) {
            error(context, "Autonomous mode not available");
            return;
        }
        
        if (args.length < 2) {
            usage(context, "AUTO <on|off>");
            context.say("Current: " + (autoMode.isEnabled() ? "ON" : "OFF"), false);
            return;
        }
        
        String action = args[1].toLowerCase();
        
        if ("on".equals(action) || "1".equals(action) || "true".equals(action)) {
            autoMode.setEnabled(true);
            feedback(context, "Autonomous mode ENABLED");
        } else if ("off".equals(action) || "0".equals(action) || "false".equals(action)) {
            autoMode.setEnabled(false);
            feedback(context, "Autonomous mode DISABLED");
        } else {
            error(context, "Invalid action. Use 'on' or 'off'");
        }
    }
}
