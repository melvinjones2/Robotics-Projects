package client.util;

import lejos.hardware.Sound;

public class DisplayUtils {
    
    public static void say(String msg, boolean beep) {
        try {
            if (beep) Sound.beep();
        } catch (Exception e) {
        }
    }
}
