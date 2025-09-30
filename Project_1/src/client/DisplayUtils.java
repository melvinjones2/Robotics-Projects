package client;

import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

public class DisplayUtils {

    public static void say(String msg, boolean beep) {
        if (beep) {
            Sound.beep();
        }
        LCD.clear();
        LCD.drawString(center(trimToWidth(msg, 16), 16), 0, 3);
    }

    public static String trimToWidth(String s, int w) {
        if (s == null) {
            return "";
        }
        return s.length() <= w ? s : s.substring(0, w);
    }

    public static String center(String s, int width) {
        int pad = Math.max(0, (width - s.length()) / 2);
        StringBuilder sb = new StringBuilder(width);
        for (int k = 0; k < pad; k++) {
            sb.append(' ');
        }
        sb.append(s);

        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String trim(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 16 ? s : s.substring(0, 16);
    }
}
