package client.util;

import lejos.hardware.Sound;

/**
 * Display and sound utilities for user feedback.
 */
public class DisplayUtils {
    /**
     * Provide audio feedback to user.
     * @param msg Message (currently not displayed to avoid overwriting command display)
     * @param beep Whether to play beep sound
     */
    public static void say(String msg, boolean beep) {
        try {
            if (beep) Sound.beep();
            // Command is already shown by CommandHandler on line 3
            // Don't overwrite it with feedback message
        } catch (Exception e) {
            // Sound system might fail - continue anyway
        }
    }
}
