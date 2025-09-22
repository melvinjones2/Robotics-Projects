package client;

import client.DisplayUtils;
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
    private static final String SERVER_HOST = "10.0.1.8"; // change this to your ip. Use ip config and look at the bluetooth one
    private static final int SERVER_PORT = 9999;

    // What the robot "says" back
    private static final String[] REPLIES = new String[] {
        "Hello, human!",
        "I am EV3.",
        "Beep boop...",
        "Ready to roll.",
        "Awaiting orders."
    };

    public static void main(String[] args) {
        LCD.clear();
        LCD.drawString("EV3 Client", 0, 0);
        LCD.drawString("Connecting...", 0, 1);

        Socket sock = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        final AtomicBoolean running = new AtomicBoolean(true);
        int replyIndex = 0;

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
            send(out, "READY");

            LCD.clear();
            LCD.drawString("Connected", 0, 0);
            LCD.drawString(SERVER_HOST + ":" + SERVER_PORT, 0, 1);
            Sound.beep();

            // ---- command handling thread ----
            IHandler handler = new CommandHandler(in, out, running, REPLIES);
            Thread commandThread = new Thread(handler);
            commandThread.start();

            // Main thread: monitor for ESCAPE button to exit
            while (running.get()) {
                if (Button.ESCAPE.isDown()) {
                    send(out, "BYE");
                    running.set(false);
                    break;
                }
                Thread.sleep(100); // avoid busy wait
            }

            // Wait for command thread to finish
            commandThread.join();

        } catch (IOException | InterruptedException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
        } finally {
            running.set(false);
            try { 
            	if (in != null) in.close(); 
        	} 
            
            catch (IOException ignored) {}
            try {
            	if (out != null) out.close(); 
        	} 
            catch (IOException ignored) {}
            
            try {
            	if (sock != null) sock.close(); 
        	} 
            catch (IOException ignored) {}
            
            LCD.clear();
            LCD.drawString("Disconnected", 0, 2);
            Button.ESCAPE.waitForPress();
        }
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }
}
