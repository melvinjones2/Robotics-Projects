package client;

import lejos.hardware.Sound;

public class BeepCommand implements Command {
    public void execute(String[] args, CommandHandler context) {
        Sound.beep();
        context.say("Beep!", true);
        context.sendLog("Beep command executed");
    }
}