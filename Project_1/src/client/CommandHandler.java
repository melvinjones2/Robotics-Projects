
package client;

import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import client.DisplayUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandHandler implements IHandler {
    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final String[] replies;

    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, String[] replies) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.replies = replies;
    }

    public void run() {
        int replyIndex = 0;
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                String msg = line.trim();

                // handle control messages
                if ("BYE".equalsIgnoreCase(msg)) {
                    say("Bye!", true);
                    running.set(false);
                    break;
                }
                if ("BEEP".equalsIgnoreCase(msg)) {
                    Sound.beep();
                    say("Beep!", true);
                    // fall through to reply
                } else if (msg.length() > 0) {
                    // Show the server's message
                    say(msg, false);
                }

                // auto-reply with a rotating phrase
                String reply = replies[replyIndex];
                replyIndex = (replyIndex + 1) % replies.length;
                send(out, reply);
            }
        } catch (IOException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
        }
    }

    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }

    private void say(String msg, boolean beep) {
        DisplayUtils.say(msg, beep);
    }
}
