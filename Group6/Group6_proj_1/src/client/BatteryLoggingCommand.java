package client;

import lejos.hardware.Battery;
import lejos.hardware.motor.BaseRegulatedMotor;
import java.io.IOException;

public class BatteryLoggingCommand implements ICommand {

    public void moveAndLog(int speed, int logCount, int intervalMs, CommandHandler context) {
        BaseRegulatedMotor motorA = MotorFactory.getMotor('A');
        if (motorA == null) {
            context.say("Motor A not available", false);
            return;
        }
        
        try {
            motorA.setSpeed(speed);
            motorA.forward();
            context.sendLog("Motor started at speed: " + speed);

            for (int i = 0; i < logCount; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    context.sendLog("MoveAndLog interrupted!");
                    break;
                }
                
                float batteryLevel = Battery.getVoltageMilliVolt();
                float voltageLevel = Battery.getVoltage();
                float batteryCurrent = Battery.getBatteryCurrent();
                float motorCurrent = Battery.getMotorCurrent();

                context.send(context.getOut(), "BATTERY: " + batteryLevel + "mV"
                        + ", Voltage: " + voltageLevel + "V"
                        + ", Battery Current: " + batteryCurrent + "mA"
                        + ", Motor Current: " + motorCurrent + "mA");

                context.sendLog("Battery level sent: " + batteryLevel + "mV"
                        + ", Voltage: " + voltageLevel + "V"
                        + ", Battery Current: " + batteryCurrent + "mA"
                        + ", Motor Current: " + motorCurrent + "mA");

                if (i < logCount - 1) {
                    Thread.sleep(intervalMs);
                }
            }
        } catch (Exception e) {
            context.say("Move/log error: " + e.getMessage(), false);
        } finally {
            motorA.stop();
            context.sendLog("Motor stopped.");
        }
    }

    @Override
    public void execute(String[] args, CommandHandler context) {
        // MOVE_AND_LOG <speed> <count> <interval_ms>
        try {
            if (args.length < 4) {
                context.say("Usage: MOVE_AND_LOG <speed> <count> <interval_ms>", false);
                return;
            }
            
            int speed = CommandParser.parseSpeed(args[1]);
            int count = CommandParser.parseInt(args[2], "count");
            int interval = CommandParser.parseInt(args[3], "interval");
            
            if (count < RobotConfig.MIN_LOG_COUNT || count > RobotConfig.MAX_LOG_COUNT) {
                context.say("Count must be " + RobotConfig.MIN_LOG_COUNT + 
                    "-" + RobotConfig.MAX_LOG_COUNT, false);
                return;
            }
            
            if (interval < RobotConfig.MIN_LOG_INTERVAL_MS || interval > RobotConfig.MAX_LOG_INTERVAL_MS) {
                context.say("Interval must be " + RobotConfig.MIN_LOG_INTERVAL_MS + 
                    "-" + RobotConfig.MAX_LOG_INTERVAL_MS + " ms", false);
                return;
            }
            
            moveAndLog(speed, count, interval, context);
            
        } catch (IllegalArgumentException e) {
            context.say("Error: " + e.getMessage(), false);
        }
    }
}
