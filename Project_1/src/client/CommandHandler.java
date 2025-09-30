package client;

import lejos.hardware.Battery;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import client.DisplayUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;
import client.Message;

public class CommandHandler implements IHandler {

    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final String[] replies;
    private final Map<String, Command> commandMap = new HashMap<String, Command>();

    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, String[] replies) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.replies = replies;
        registerCommands();
        init();
    }

    private void registerCommands() {
        commandMap.put("BEEP", new BeepCommand());
        commandMap.put("MOVE", new MoveCommand());
        commandMap.put("BWD", new BackwardCommand());
        commandMap.put("STOP", new StopCommand());
        commandMap.put("GET_BATTERY", new BatteryCommand());
        commandMap.put("SET_DEBUG", new SetDebugCommand());
        commandMap.put("BYE", new ByeCommand());
        // Add more commands as needed
    }

    private void init() {
        String motorSummary = MotorDetector.getMotorSummary();
        LCD.drawString(motorSummary, 0, 2);
        try {
            send(out, "MOTOR: " + motorSummary);
        } catch (IOException e) {
            LCD.drawString("Motor send err", 0, 6);
        }
    }

    public void run() {
        int replyIndex = 0;
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                String msg = line.trim();
                Message message = Message.parse(msg);

                if ("SET_DEBUG".equalsIgnoreCase(message.getType())) {
                    ClientMain.DEBUG = "1".equals(message.getPayload().trim());
                    System.out.println("DEBUG mode set to: " + ClientMain.DEBUG);
                    continue;
                }

                // Ignore TICK_ACK messages for display
                if ("TICK_ACK".equals(message.getType())) {
                    continue;
                }

                // Respond to GET_BATTERY
                if ("GET_BATTERY".equalsIgnoreCase(message.getType())) {
                    int voltage = lejos.hardware.Battery.getVoltageMilliVolt();
                    int current = lejos.hardware.Battery.getCurrentMilliAmp();
                    int percent = lejos.hardware.Battery.getBatteryLevel();
                    // Format as: BATTERY: VOLTAGE=7.2 V, CURRENT=0.5 A, LEVEL=80%
                    String batteryMsg = String.format("BATTERY: VOLTAGE=%.2f V, CURRENT=%.2f A, LEVEL=%d%%",
                            voltage / 1000.0, current / 1000.0, percent);
                    send(out, batteryMsg);
                    continue;
                }

                Command cmd = commandMap.get(message.getType());
                if (cmd != null) {
                    cmd.execute(msg.split(" "), this);
                    if ("BYE".equalsIgnoreCase(message.getType())) {
                        break;
                    }
                } else if (msg.length() > 0) {
                    say(msg, false);
                    sendLog("Displayed message: " + msg);
                } else {
                    if (replies.length > 0) {
                        String reply = replies[replyIndex % replies.length];
                        replyIndex++;
                        send(out, Message.construct("REPLY", reply));
                        sendLog("Sent reply: " + reply);
                    }
                }
            }
        } catch (IOException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
            sendLog("Network error: " + e.getMessage());
        }
    }

    // Utility methods for commands to use
    public void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
    }

    public void sendLog(String logMsg) {
        if (ClientMain.DEBUG) {
            try {
                send(this.out, "LOG: " + logMsg);
            } catch (IOException e) {
                // Optionally handle send error
            }
        }
    }

    public void say(String msg, boolean beep) {
        DisplayUtils.say(msg, beep);
    }

    // Getters for command classes
    public BufferedWriter getOut() {
        return out;
    }

    public AtomicBoolean getRunning() {
        return running;
    }
}
