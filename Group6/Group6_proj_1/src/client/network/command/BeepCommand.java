package client.network.command;

import client.config.RobotConfig;
import common.ParsedCommand;
import lejos.hardware.Sound;

import java.io.IOException;

/**
 * Command to make the robot beep.
 * 
 * Syntax: BEEP [count]
 * Count is optional, defaults to 1. Maximum is configured limit.
 */
public class BeepCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public BeepCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        int count = parsedCmd.getArgAsInt(0, 1);
        for (int i = 0; i < Math.min(count, RobotConfig.COMMAND_MAX_BEEP_COUNT); i++) {
            Sound.beep();
            try {
                Thread.sleep(RobotConfig.COMMAND_BEEP_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    @Override
    public String getName() {
        return "BEEP";
    }
}
