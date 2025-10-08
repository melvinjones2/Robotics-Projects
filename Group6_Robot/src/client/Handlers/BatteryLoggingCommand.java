package client.Handlers;

import lejos.hardware.Battery;
import lejos.hardware.motor.Motor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import java.io.IOException;

import client.Interfaces.ICommand;

public class BatteryLoggingCommand implements ICommand {
    private float batteryLevel;
    private float voltageLevel;
    private float batteryCurrent;
    private float motorCurrent;

    public BatteryLoggingCommand() {
        this.batteryLevel = Battery.getVoltageMilliVolt();
        this.voltageLevel = Battery.getVoltage();
        this.batteryCurrent = Battery.getBatteryCurrent();
        this.motorCurrent = Battery.getMotorCurrent();
    }

    public void moveAndLog(int speed, int logCount, int intervalMs, CommandHandler context) {
        EV3LargeRegulatedMotor motorA = new EV3LargeRegulatedMotor(MotorPort.A);
        try {
            motorA.setSpeed(speed);
            motorA.forward();
            context.sendLog("Motor started at speed: " + speed);

            for (int i = 0; i < logCount; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    context.sendLog("MoveAndLog interrupted!");
                    break;
                }
                // Update battery readings
                batteryLevel = Battery.getVoltageMilliVolt();
                voltageLevel = Battery.getVoltage();
                batteryCurrent = Battery.getBatteryCurrent();
                motorCurrent = Battery.getMotorCurrent();

                // Send battery log
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
            motorA.close();
        }
    }

    @Override
    public void execute(String[] args, CommandHandler context) {
        try {
            context.send(context.getOut(), "BATTERY: " + batteryLevel + "mV"
                    + ", Voltage: " + voltageLevel + "V"
                    + ", Battery Current: " + batteryCurrent + "mA"
                    + ", Motor Current: " + motorCurrent + "mA");

            context.sendLog("Battery level sent: " + batteryLevel + "mV"
                    + ", Voltage: " + voltageLevel + "V"
                    + ", Battery Current: " + batteryCurrent + "mA"
                    + ", Motor Current: " + motorCurrent + "mA");
        } catch (IOException e) {
            context.say("Batt send err", false);
        }
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(float batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public float getVoltageLevel() {
        return voltageLevel;
    }

    public void setVoltageLevel(float voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    public float getBatteryCurrent() {
        return batteryCurrent;
    }

    public void setBatteryCurrent(float batteryCurrent) {
        this.batteryCurrent = batteryCurrent;
    }

    public float getMotorCurrent() {
        return motorCurrent;
    }

    public void setMotorCurrent(float motorCurrent) {
        this.motorCurrent = motorCurrent;
    }

}
