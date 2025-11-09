package client;

import lejos.hardware.Sound;

public class BeepCommand implements ICommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        // BEEP or BEEP <count>
        int count = 1;
        
        if (args.length > 1) {
            try {
                count = CommandParser.parseInt(args[1], "count");
                if (count < RobotConfig.MIN_BEEP_COUNT || count > RobotConfig.MAX_BEEP_COUNT) {
                    context.say("Count must be " + RobotConfig.MIN_BEEP_COUNT + 
                        "-" + RobotConfig.MAX_BEEP_COUNT, false);
                    return;
                }
            } catch (IllegalArgumentException e) {
                context.say("Error: " + e.getMessage(), false);
                return;
            }
        }
        
        for (int i = 0; i < count; i++) {
            Sound.beep();
            if (i < count - 1) {
                try {
                    Thread.sleep(RobotConfig.BEEP_DELAY_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        context.say("Beep! x" + count, true);
        context.sendLog("Beep command executed " + count + " times");
    }
}