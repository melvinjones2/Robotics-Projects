package client;

import lejos.hardware.Battery;
import java.io.IOException;

public class BatteryCommand implements ICommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        float batteryLevel;
        float voltageLevel;
        float batteryCurrent;
        float motorCurrent;
        try {
            batteryLevel = Battery.getVoltageMilliVolt();
            voltageLevel = Battery.getVoltage();
            batteryCurrent = Battery.getBatteryCurrent();
            motorCurrent = Battery.getMotorCurrent();

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
}