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

    public static volatile boolean DEBUG = false;

    private static final String SERVER_HOST = "10.0.1.8";
    private static final int SERVER_PORT = 9999;
    private static final int TICK_RATE_MS = 50; // 20 ticks per second

    private static volatile int frameCount = 0;
    private static volatile boolean running = true;

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
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

            // ---- handshake ----
            String first = in.readLine();
            LCD.clear();
            LCD.drawString("Received: " + (first != null ? first.trim() : "null"), 0, 0);
            System.out.println("Received from server: " + first);

            // Expect "HELLO:<frameCount>"
            int serverFrame = 0;
            boolean helloOk = false;
            if (first != null && first.trim().startsWith("HELLO")) {
                String[] parts = first.trim().split(":");
                if (parts.length == 2) {
                    try {
                        serverFrame = Integer.parseInt(parts[1]);
                        helloOk = true;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (!helloOk) {
                LCD.clear();
                LCD.drawString("Bad hello", 0, 0);
                Sound.buzz();
                System.out.println("Expected HELLO:<frame>, got: " + first);
                Button.ESCAPE.waitForPress();
                return;
            }
            send(out, "READY:" + frameCount);
            System.out.println("Sent to server: READY:" + frameCount);

            LCD.clear();
            LCD.drawString("Connected", 0, 0);
            LCD.drawString(SERVER_HOST + ":" + SERVER_PORT, 0, 1);
            Sound.beep();

            // ---- command handling thread ----
            final String[] replies = new String[]{
                "Hello, human!",
                "I am EV3.",
                "Beep boop...",
                "Ready to roll.",
                "Awaiting orders."
            };

            final CommandHandler command_handler = new CommandHandler(in, out, running, replies);
            Thread commandThread = new Thread(command_handler);
            commandThread.start();

            // Main thread: monitor for ESCAPE button to exit, and tick loop
            while (running.get()) {
                long tickStart = System.currentTimeMillis();

                if (Button.ESCAPE.isDown()) {
                    LCD.clear();
                    LCD.drawString("Disconnecting...", 0, 0);
                    send(out, "BYE:" + frameCount);
                    System.out.println("Sent to server: BYE:" + frameCount);
                    running.set(false);
                    break;
                }

                // if (DEBUG) {
                //     LCD.clear();
                //     LCD.drawString("Tick: " + frameCount, 0, 0);
                // }
                send(out, "TICK:" + frameCount);
                // System.out.println("Sent to server: TICK:" + frameCount);

                frameCount++;

                long elapsed = System.currentTimeMillis() - tickStart;
                long sleepTime = TICK_RATE_MS - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            }

            commandThread.join();

        } catch (IOException | InterruptedException e) {
            LCD.clear();
            LCD.drawString("Net error", 0, 0);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 1);
            Sound.buzz();
            System.out.println("Network error: " + e.getMessage());
        } finally {
            running.set(false);
            try {
                if (in != null) {
                    in.close();

                }
            } catch (IOException ignored) {
            }
            try {
                if (out != null) {
                    out.close();

                }
            } catch (IOException ignored) {
            }
            try {
                if (sock != null) {
                    sock.close();

                }
            } catch (IOException ignored) {
            }

            LCD.clear();
            LCD.drawString("Disconnected", 0, 0);
            System.out.println("Disconnected from server.");
            Button.ESCAPE.waitForPress();
        }
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
    }
}
