package client.commands.autonomous;

import client.commands.BaseCommand;
import client.commands.CommandParser;
import client.config.RobotConfig;
import client.motor.MotorFactory;
import client.network.CommandHandler;

import lejos.hardware.Battery;
import lejos.hardware.motor.BaseRegulatedMotor;

public class BatteryLoggingCommand extends BaseCommand {

    public void moveAndLog(int speed, int logCount, int intervalMs, CommandHandler context) {
        BaseRegulatedMotor motorA = MotorFactory.getMotor('A');
        if (motorA == null) {
            error(context, "Motor A not available");
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

                String data = "BATTERY: " + batteryLevel + "mV"
                        + ", Voltage: " + voltageLevel + "V"
                        + ", Battery Current: " + batteryCurrent + "mA"
                        + ", Motor Current: " + motorCurrent + "mA";
                
                sendToServer(context, data);
                context.sendLog("Battery level sent: " + batteryLevel + "mV"
                        + ", Voltage: " + voltageLevel + "V"
                        + ", Battery Current: " + batteryCurrent + "mA"
                        + ", Motor Current: " + motorCurrent + "mA");

                if (i < logCount - 1) {
                    Thread.sleep(intervalMs);
                }
            }
        } catch (Exception e) {
            error(context, "Move/log error: " + e.getMessage());
        } finally {
            motorA.stop();
            context.sendLog("Motor stopped.");
        }
    }

    @Override
    public void execute(String[] args, CommandHandler context) {
        try {
            if (!validateArgCount(context, args, 4, 4, "MOVE_AND_LOG <speed> <count> <interval_ms>")) {
                return;
            }
            
            int speed = CommandParser.parseSpeed(args[1]);
            int count = CommandParser.parseInt(args[2], "count");
            int interval = CommandParser.parseInt(args[3], "interval");
            
            if (count < RobotConfig.MIN_LOG_COUNT || count > RobotConfig.MAX_LOG_COUNT) {
                error(context, "Count must be " + RobotConfig.MIN_LOG_COUNT + 
                    "-" + RobotConfig.MAX_LOG_COUNT);
                return;
            }
            
            if (interval < RobotConfig.MIN_LOG_INTERVAL_MS || interval > RobotConfig.MAX_LOG_INTERVAL_MS) {
                error(context, "Interval must be " + RobotConfig.MIN_LOG_INTERVAL_MS + 
                    "-" + RobotConfig.MAX_LOG_INTERVAL_MS + " ms");
                return;
            }
            
            moveAndLog(speed, count, interval, context);
            
        } catch (IllegalArgumentException e) {
            error(context, e.getMessage());
        }
    }
}
