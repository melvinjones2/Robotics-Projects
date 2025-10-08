package client.Handlers;

import client.Interfaces.ICommand;
import lejos.hardware.Sound;

public class BeepCommand implements ICommand {
    public void execute(String[] args, CommandHandler context) {
        Sound.beep();
        context.say("Beep!", true);
        context.sendLog("Beep command executed");
    }
}