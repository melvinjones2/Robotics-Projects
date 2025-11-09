package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

public class CommandHandler implements IHandler {

    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final String[] replies;
    private volatile boolean debug;
    private final Map<String, ICommand> commandMap = new HashMap<String, ICommand>();
    private volatile Thread currentCommandThread; // Track running command thread

    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, String[] replies) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.replies = replies;
        registerCommands();
        init();
    }

    private void registerCommands() {
        // Use unified movement command for better control
        UnifiedMoveCommand moveCmd = new UnifiedMoveCommand();
        
        commandMap.put("BEEP", new BeepCommand());
        commandMap.put("MOVE", moveCmd);
        commandMap.put("FWD", moveCmd);
        commandMap.put("FORWARD", moveCmd);
        commandMap.put("BWD", moveCmd);
        commandMap.put("BACKWARD", moveCmd);
        commandMap.put("BACK", moveCmd);
        commandMap.put("LEFT", moveCmd);
        commandMap.put("TURNLEFT", moveCmd);
        commandMap.put("RIGHT", moveCmd);
        commandMap.put("TURNRIGHT", moveCmd);
        commandMap.put("STOP", new StopCommand());
        commandMap.put("GET_BATTERY", new BatteryCommand());
        commandMap.put("SET_DEBUG", new SetDebugCommand());
        commandMap.put("BYE", new ByeCommand());
        commandMap.put("LOG", new LogCommand());
        commandMap.put("MOVE_AND_LOG", new BatteryLoggingCommand());
        commandMap.put("ROTATE", new RotateMotorCommand());
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
                // Use unified parser
                CommandParser.ParsedCommand parsed = CommandParser.parse(line);
                String cmdKey = parsed.getCommand();
                String[] parts = parsed.getArgs();

                // Handle SET_DEBUG specially
                if (cmdKey.equals("SET_DEBUG") && parts.length > 1) {
                    setDebug("1".equals(parts[1]));
                    sendLog("Debug mode set to " + debug);
                    continue;
                }

                final ICommand cmd = commandMap.get(cmdKey);
                if (cmd != null) {
                    // If it's a long-running command, run in a separate thread
                    if ("MOVE_AND_LOG".equalsIgnoreCase(cmdKey)) {
                        // Interrupt any previous command
                        if (currentCommandThread != null && currentCommandThread.isAlive()) {
                            currentCommandThread.interrupt();
                        }
                        final String[] cmdParts = parts;
                        final CommandHandler handler = this;
                        currentCommandThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                cmd.execute(cmdParts, handler);
                            }
                        }, "move-and-log-thread");
                        currentCommandThread.start();
                    } else if ("STOP".equalsIgnoreCase(cmdKey)) {
                        // Interrupt the running command thread
                        if (currentCommandThread != null && currentCommandThread.isAlive()) {
                            currentCommandThread.interrupt();
                        }
                        cmd.execute(parts, this);
                    } else {
                        cmd.execute(parts, this);
                        if ("BYE".equalsIgnoreCase(cmdKey)) {
                            break;
                        }
                    }
                } else if (cmdKey.length() > 0) {
                    say(line.trim(), false);
                    sendLog("Displayed message: " + line.trim());
                } else {
                    if (replies.length > 0) {
                        String reply = replies[replyIndex % replies.length];
                        replyIndex++;
                        send(out, "REPLY: " + reply);
                        sendLog("Sent reply: " + reply);
                    }
                }
            }
        } catch (IOException e) {
            say("Net error", false);
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
        if (debug) {
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

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }
}
