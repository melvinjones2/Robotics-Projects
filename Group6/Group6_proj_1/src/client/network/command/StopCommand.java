package client.network.command;

import client.motor.MotorFactory;
import common.ParsedCommand;

import java.io.IOException;

/**
 * Command to stop all motors or a specific motor.
 * 
 * Syntax: STOP [motorPort]
 * If motorPort provided, stops only that motor.
 * Otherwise stops all motors and autonomous tasks.
 */
public class StopCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public StopCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(CommandContext context) throws IOException {
        if (parsedCmd.getArgCount() > 0 && parsedCmd.isArgChar(0)) {
            // Stop single motor
            char motorPort = parsedCmd.getArgAsChar(0);
            client.motor.MotorFactory.getMotor(motorPort).stop(true);
        } else {
            // Stop everything
            shutdownBallTasks(context);
            MotorFactory.stopAll();
        }
    }
    
    @Override
    public String getName() {
        return "STOP";
    }
    
    private void shutdownBallTasks(CommandContext context) {
        if (context.getBallSearchController() != null && context.getBallSearchController().isEnabled()) {
            context.getBallSearchController().setEnabled(false);
        }
        if (context.getBallDetector() != null) {
            context.getBallDetector().stop();
        }
        stopActiveScan(context);
    }
    
    private void stopActiveScan(CommandContext context) {
        Thread ballScanThread = context.getBallScanThread();
        if (ballScanThread != null && ballScanThread.isAlive()) {
            if (context.getBallDetector() != null) {
                context.getBallDetector().stop();
            }
            try {
                ballScanThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            context.setBallScanThread(null);
        }
    }
}
