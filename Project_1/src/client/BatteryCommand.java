package client;

import lejos.hardware.Battery;
import java.io.IOException;

public class BatteryCommand implements Command {

    public void execute(String[] args, CommandHandler context) {
        int batteryLevel = Battery.getVoltageMilliVolt();
        try {
            context.send(context.getOut(), "BATTERY: " + batteryLevel + "mV");
            context.sendLog("Battery level sent: " + batteryLevel + "mV");
        } catch (IOException e) {
            context.say("Batt send err", false);
        }
    }
}
