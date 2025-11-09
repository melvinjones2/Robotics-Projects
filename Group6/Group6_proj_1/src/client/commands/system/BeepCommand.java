package client.commands.system;

import client.commands.BaseCommand;
import client.commands.CommandParser;
import client.config.RobotConfig;
import client.network.CommandHandler;

import lejos.hardware.Sound;

public class BeepCommand extends BaseCommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        int count = 1;
        
        if (args.length > 1) {
            try {
                count = CommandParser.parseInt(args[1], "count");
                if (count < RobotConfig.MIN_BEEP_COUNT || count > RobotConfig.MAX_BEEP_COUNT) {
                    error(context, "Count must be " + RobotConfig.MIN_BEEP_COUNT + 
                        "-" + RobotConfig.MAX_BEEP_COUNT);
                    return;
                }
            } catch (IllegalArgumentException e) {
                error(context, e.getMessage());
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
        
        feedback(context, "Beep! x" + count, true);
    }
}