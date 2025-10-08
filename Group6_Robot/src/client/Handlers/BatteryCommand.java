package client.Handlers;

import lejos.hardware.Battery;
import java.io.IOException;

import client.Interfaces.ICommand;

public class BatteryCommand implements ICommand {
    private float batteryLevel;
    private float voltageLevel;
    private float batteryCurrent;
    private float motorCurrent;

    public void execute(String[] args, CommandHandler context) {
        this.batteryLevel = Battery.getVoltageMilliVolt();
        this.voltageLevel = Battery.getVoltage();
        this.batteryCurrent = Battery.getBatteryCurrent();
        this.motorCurrent = Battery.getMotorCurrent();
        try {
            this.batteryLevel = Battery.getVoltageMilliVolt();
            this.voltageLevel = Battery.getVoltage();
            this.batteryCurrent = Battery.getBatteryCurrent();
            this.motorCurrent = Battery.getMotorCurrent();

            // Send battery log
            context.send(context.getOut(), "BATTERY: " + this.batteryLevel + "mV"
                    + ", Voltage: " + this.voltageLevel + "V"
                    + ", Battery Current: " + this.batteryCurrent + "mA"
                    + ", Motor Current: " + motorCurrent + "mA");

            context.sendLog("Battery level sent: " + batteryLevel + "mV"
                    + ", Voltage: " + voltageLevel + "V"
                    + ", Battery Current: " + batteryCurrent + "mA"
                    + ", Motor Current: " + motorCurrent + "mA");

        } catch (IOException e) {
            context.say("Batt send err", false);
        }
    }
}