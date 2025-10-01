package client;

import client.DisplayUtils;
import lejos.hardware.Battery;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientMain {
    private static final String SERVER_HOST = "10.0.1.8";
    private static final int SERVER_PORT = 9999;
    private static final int TICK_RATE_MS = 50; // 20 ticks per second

    private static volatile int frameCount = 0;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    private static volatile boolean running = true;
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
=======
    private static final boolean DEBUG = false; // Set true to enable client debug logging
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)

    public static void main(String[] args) {
        LCD.clear();
        LCD.drawString("EV3 Client", 0, 0);
        LCD.drawString("Connecting...", 0, 1);

        Socket sock = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        final AtomicBoolean running = new AtomicBoolean(true);

        try {
            sock = new Socket(SERVER_HOST, SERVER_PORT);
            sock.setTcpNoDelay(true);
            in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

            // ---- handshake ----
            String first = in.readLine(); // expect HELLO
            if (!"HELLO".equalsIgnoreCase(first != null ? first.trim() : "")) {
                LCD.drawString("Bad hello", 0, 3);
                Sound.buzz();
                Button.ESCAPE.waitForPress();
                return;
            }
            send(out, "READY:0"); // Send initial frame count

            LCD.clear();
            LCD.drawString("Connected", 0, 0);
            LCD.drawString(SERVER_HOST + ":" + SERVER_PORT, 0, 1);
            Sound.beep();

            // ---- command handling thread ----
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
            final String[] replies = new String[]{
=======
            final String[] replies = new String[] {
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)
                "Hello, human!",
                "I am EV3.",
                "Beep boop...",
                "Ready to roll.",
                "Awaiting orders."
            };

            final CommandHandler command_handler = new CommandHandler(in, out, running, replies);
            Thread commandThread = new Thread(command_handler);
=======
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
            final BufferedReader inFinal = in;
            Thread commandThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        String line;
                        while (running.get() && (line = inFinal.readLine()) != null) {
                            String msg = line.trim();
                            // Optionally handle TICK_ACK or other messages here
                        }
                    } catch (IOException e) {
                        running.set(false);
                    }
                }
            });
<<<<<<< HEAD
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
            commandThread.start();

            // Main thread: monitor for ESCAPE button to exit, and tick loop
            while (running.get()) {
                long tickStart = System.currentTimeMillis();

                if (Button.ESCAPE.isDown()) {
                    send(out, "BYE:" + frameCount);
                    running.set(false);
                    break;
                }

                // Send a TICK message with the current frame count
                send(out, "TICK:" + frameCount);

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
                frameCount++;
=======
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
                // Send battery status every 20 ticks (~1 second)
                if (frameCount % 20 == 0) {
                    double voltage = Battery.getVoltage();
                    send(out, "BATTERY:" + voltage);
                }

                frameCount++; // Increment after sending
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
=======
                frameCount++; // Increment after sending
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)

                // Wait for next tick
                long elapsed = System.currentTimeMillis() - tickStart;
                long sleepTime = TICK_RATE_MS - elapsed;
                if (sleepTime > 0) Thread.sleep(sleepTime);
            }

            // Wait for command thread to finish
            commandThread.join();

        } catch (IOException | InterruptedException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
        } finally {
            running.set(false);
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (sock != null) sock.close(); } catch (IOException ignored) {}

            LCD.clear();
            LCD.drawString("Disconnected", 0, 2);
            Button.ESCAPE.waitForPress();
        }
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }
}
