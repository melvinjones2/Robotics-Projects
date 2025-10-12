package client;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class ClientMain {

    private static final String SERVER_HOST = "10.0.1.8";
    private static final int SERVER_PORT = 9999;
    private static final int TICK_RATE_MS = 50; // 20 ticks per second
    private static volatile int frameCount = 0;
    private static final boolean DEBUG = false; // Set true to enable client debug logging

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

            List<ISensor> foundSensors = new ArrayList<>();
            
            /////// NEED to FIND A BETTER WAy TO DO THIS
            try
            {
	            foundSensors.add(new UltrasonicSensor(SensorPort.S1, "listen"));
	            foundSensors.add(new TouchSensor());
	            foundSensors.add(new LightSensor(SensorPort.S4, "rgb"));
	            foundSensors.add(new GyroSensor(SensorPort.S3, "rate"));
            } catch (Exception e)
            {
            	out.write(e.toString());
            }
            ////////////////////////////////////////////////
            
            
            SensorThread sensorThread = new SensorThread(out, running, foundSensors, frameCount);
            Thread sensorThreadObj = new Thread(sensorThread);
            sensorThreadObj.start();

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
                if (DEBUG) {
                    LCD.clear(4);
                    LCD.drawString("Frame: " + frameCount, 0, 4);
                }

                frameCount++; // Increment after sending

                // Wait for next tick
                long elapsed = System.currentTimeMillis() - tickStart;
                long sleepTime = TICK_RATE_MS - elapsed;
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }

            // Wait for command and sensor threads to finish
            try {
                commandThread.join();
            } catch (InterruptedException e) {
                // Ignore
            }
            sensorThreadObj.interrupt();
            try {
                sensorThreadObj.join();
            } catch (InterruptedException e) {
                // Ignore
            }

        } catch (IOException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
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
            LCD.drawString("Disconnected", 0, 2);
            Button.ESCAPE.waitForPress();
        }
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
    }
}
