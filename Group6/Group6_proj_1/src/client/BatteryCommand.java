package client;

import lejos.hardware.Battery;

public class BatteryCommand extends BaseCommand {
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        float batteryLevel = Battery.getVoltageMilliVolt();
        float voltageLevel = Battery.getVoltage();
        float batteryCurrent = Battery.getBatteryCurrent();
        float motorCurrent = Battery.getMotorCurrent();

        String batteryData = String.format("BATTERY: %.0fmV, Voltage: %.2fV, "
                + "Battery Current: %.0fmA, Motor Current: %.0fmA",
                batteryLevel, voltageLevel, batteryCurrent, motorCurrent);

        sendToServer(context, batteryData);
        feedback(context, "Battery data sent");
    }
}