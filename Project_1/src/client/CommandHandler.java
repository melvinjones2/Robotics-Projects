
package client;

import lejos.hardware.Battery;
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
    private volatile int batteryMonitorIntervalMs = 1000; // Default 1 second
    private java.util.Timer batteryMonitorTimer;

    // Initialization step
    private void init() {
        String motorSummary = MotorDetector.getMotorSummary();
        LCD.drawString(motorSummary, 0, 2);
        try {
            send(out, "MOTOR: " + motorSummary);
        } catch (IOException e) {
            LCD.drawString("Motor send err", 0, 6);
        }
    }

    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, String[] replies) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.replies = replies;
        init();
        startBatteryMonitoring();
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
                } else if (msg.toUpperCase().startsWith("MOVE")) {
                    // MOVE <speed>
                    String[] parts = msg.split(" ");
                    int speed = 200;
                    if (parts.length > 1) {
                        try { speed = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    }
                    MotorController.moveAllForward(speed);
                    say("Motors moving at " + speed, false);
                } else if (msg.toUpperCase().startsWith("STOP")) {
                    MotorController.stopAll();
                    say("Motors stopped", false);
                } else if (msg.length() > 0) {
                    // Show the server's message
                    say(msg, false);
                } else {
                    // auto-reply with a rotating phrase
                    String reply = replies[replyIndex];
                    replyIndex = (replyIndex + 1) % replies.length;
                    send(out, "REPLY: " + reply);
                }


            }
        } catch (IOException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
        }
        stopBatteryMonitoring();
    }

    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }

    // Battery monitoring logic
    private void startBatteryMonitoring() {
        batteryMonitorTimer = new java.util.Timer(true);
        batteryMonitorTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                monitorBattery();
            }
        }, 0, batteryMonitorIntervalMs);
    }

    private void stopBatteryMonitoring() {
        if (batteryMonitorTimer != null) {
            batteryMonitorTimer.cancel();
        }
    }

    public void setBatteryMonitorInterval(int intervalMs) {
        batteryMonitorIntervalMs = intervalMs;
        stopBatteryMonitoring();
        startBatteryMonitoring();
    }

    private void monitorBattery() {
        int batteryLevel = Battery.getVoltageMilliVolt();
        LCD.drawString("Battery: " + batteryLevel + "mV", 0, 5);
        try {
            send(out, "BATTERY: " + batteryLevel + "mV");
        } catch (IOException e) {
            LCD.drawString("Batt send err", 0, 6);
        }
    }

    private void say(String msg, boolean beep) {
        DisplayUtils.say(msg, beep);
    }
}
