
package client;

import lejos.hardware.Battery;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import client.DisplayUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.HashMap;
import java.util.Map;
<<<<<<< HEAD
import client.Message;
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
=======
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)

public class CommandHandler implements IHandler {
    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final String[] replies;
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
    private volatile boolean debug;
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)
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
=======
    private volatile int batteryMonitorIntervalMs = 1000; // Default 1 second
    private java.util.Timer batteryMonitorTimer;
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
=======
    private volatile int batteryMonitorIntervalMs = 1000; // Default 1 second
    private java.util.Timer batteryMonitorTimer;
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)

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

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
                if ("SET_DEBUG".equalsIgnoreCase(message.getType())) {
                    ClientMain.DEBUG = "1".equals(message.getPayload().trim());
                    System.out.println("DEBUG mode set to: " + ClientMain.DEBUG);
                    continue;
=======
                // Remove tick/frame suffix if present (e.g., ":123")
                int colonIdx = msg.lastIndexOf(':');
                if (colonIdx > 0 && colonIdx < msg.length() - 1) {
                    String possibleTick = msg.substring(colonIdx + 1);
                    try {
                        Integer.parseInt(possibleTick);
                        msg = msg.substring(0, colonIdx).trim();
                    } catch (NumberFormatException ignored) {}
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)
                }

                // Normalize whitespace
                msg = msg.replaceAll("\\s+", " ");

                // Split command and arguments
                String[] parts = msg.split(" ");
                String cmdKey = parts[0].toUpperCase();

                // Handle SET_DEBUG:1 style
                if (cmdKey.startsWith("SET_DEBUG:")) {
                    String value = cmdKey.substring("SET_DEBUG:".length()).trim();
                    setDebug("1".equals(value));
                    sendLog("Debug mode set to " + debug);
                    continue;
                }

                Command cmd = commandMap.get(cmdKey);
                if (cmd != null) {
                    cmd.execute(parts, this);
                    if ("BYE".equalsIgnoreCase(cmdKey)) break;
                } else if (msg.length() > 0) {
                    say(msg, false);
                    sendLog("Displayed message: " + msg);
                } else {
                    if (replies.length > 0) {
                        String reply = replies[replyIndex % replies.length];
                        replyIndex++;
                        send(out, "REPLY: " + reply);
                        sendLog("Sent reply: " + reply);
=======
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
                    // MOVE <speed> or MOVE <port> <speed>
                    String[] parts = msg.split(" ");
                    if (parts.length == 2) {
                        // MOVE <speed>
                        int speed = 200;
                        try { speed = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        MotorController.moveAllForward(speed);
                        say("Motors moving at " + speed, false);
                    } else if (parts.length == 3) {
                        // MOVE <port> <speed>
                        char port = parts[1].charAt(0);
                        int speed = 200;
                        try { speed = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                        MotorController.moveForward(port, speed);
                        say("Motor " + port + " moving at " + speed, false);
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
                    }
                } else if (msg.toUpperCase().startsWith("BWD")) {
                    // BWD <port> <speed>
                    String[] parts = msg.split(" ");
                    if (parts.length == 3) {
                        char port = parts[1].charAt(0);
                        int speed = 200;
                        try { speed = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                        MotorController.moveBackward(port, speed);
                        say("Motor " + port + " backward at " + speed, false);
                    }
                } else if (msg.toUpperCase().startsWith("STOP")) {
                    // STOP or STOP <port>
                    String[] parts = msg.split(" ");
                    if (parts.length == 1) {
                        MotorController.stopAll();
                        say("Motors stopped", false);
                    } else if (parts.length == 2) {
                        char port = parts[1].charAt(0);
                        MotorController.stop(port);
                        say("Motor " + port + " stopped", false);
                    }
                } else if (msg.length() > 0) {
                    // Show the server's message
                    say(msg, false);
                }
=======
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
                    // MOVE <speed> or MOVE <port> <speed>
                    String[] parts = msg.split(" ");
                    if (parts.length == 2) {
                        // MOVE <speed>
                        int speed = 200;
                        try { speed = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        MotorController.moveAllForward(speed);
                        say("Motors moving at " + speed, false);
                    } else if (parts.length == 3) {
                        // MOVE <port> <speed>
                        char port = parts[1].charAt(0);
                        int speed = 200;
                        try { speed = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                        MotorController.moveForward(port, speed);
                        say("Motor " + port + " moving at " + speed, false);
                    }
                } else if (msg.toUpperCase().startsWith("BWD")) {
                    // BWD <port> <speed>
                    String[] parts = msg.split(" ");
                    if (parts.length == 3) {
                        char port = parts[1].charAt(0);
                        int speed = 200;
                        try { speed = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                        MotorController.moveBackward(port, speed);
                        say("Motor " + port + " backward at " + speed, false);
                    }
                } else if (msg.toUpperCase().startsWith("STOP")) {
                    // STOP or STOP <port>
                    String[] parts = msg.split(" ");
                    if (parts.length == 1) {
                        MotorController.stopAll();
                        say("Motors stopped", false);
                    } else if (parts.length == 2) {
                        char port = parts[1].charAt(0);
                        MotorController.stop(port);
                        say("Motor " + port + " stopped", false);
                    }
                } else if (msg.length() > 0) {
                    // Show the server's message
                    say(msg, false);
                }
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)

                // auto-reply with a rotating phrase
                String reply = replies[replyIndex];
                replyIndex = (replyIndex + 1) % replies.length;
                send(out, "REPLY: " + reply);
            }
        } catch (IOException e) {
            LCD.drawString("Net error", 0, 3);
            LCD.drawString(DisplayUtils.trim(e.getMessage()), 0, 4);
            Sound.buzz();
        }
        stopBatteryMonitoring();
    }

<<<<<<< HEAD
<<<<<<< HEAD
    // Utility methods for commands to use
    public void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }

    public void sendLog(String logMsg) {
        if (debug) {
            try {
                send(this.out, "LOG: " + logMsg);
            } catch (IOException e) {
                // Optionally handle send error
=======
    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }

=======
    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line); out.write("\n"); out.flush();
    }

>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
    // Battery monitoring logic
    private void startBatteryMonitoring() {
        batteryMonitorTimer = new java.util.Timer(true);
        batteryMonitorTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                monitorBattery();
<<<<<<< HEAD
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
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

<<<<<<< HEAD
<<<<<<< HEAD
    // Getters for command classes
<<<<<<< HEAD
    public BufferedWriter getOut() {
        return out;
    }

    public AtomicBoolean getRunning() {
        return running;
=======
=======
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
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
<<<<<<< HEAD
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
    }
=======
    public BufferedWriter getOut() { return out; }
    public AtomicBoolean getRunning() { return running; }
    public void setDebug(boolean debug) { this.debug = debug; }
    public boolean isDebug() { return debug; }
>>>>>>> parent of 47db0d6 (feat: implement message handling and battery logging; enhance debug command functionality)
=======
    }
>>>>>>> parent of 3c29b81 (feat: implement command handling system with battery status, movement, and logging capabilities)
}
