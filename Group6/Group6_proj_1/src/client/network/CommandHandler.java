package client.network;

import client.commands.CommandParser;
import client.commands.ICommand;
import client.commands.autonomous.AutoCommand;
import client.commands.autonomous.BatteryLoggingCommand;
import client.commands.movement.RotateMotorCommand;
import client.commands.movement.StopCommand;
import client.commands.movement.UnifiedMoveCommand;
import client.commands.sensor.AnalyzeSensorsCommand;
import client.commands.sensor.NavigationSuggestCommand;
import client.commands.sensor.SensorStatsCommand;
import client.commands.system.BatteryCommand;
import client.commands.system.BeepCommand;
import client.commands.system.ByeCommand;
import client.commands.system.LogCommand;
import client.commands.system.SetDebugCommand;
import client.motor.MotorDetector;
import client.network.CommandHandler;
import client.safety.AsimovSafetyChecker;
import client.safety.AutonomousMode;
import client.safety.CommandPriority;
import client.sensor.data.SensorDataStore;
import client.util.DisplayUtils;

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
    private SensorDataStore dataStore; // Store sensor data for analysis commands
    private AsimovSafetyChecker safetyChecker; // Asimov's Three Laws enforcement
    private volatile CommandPriority currentCommandPriority = CommandPriority.USER; // Current executing command priority

    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, String[] replies) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.replies = replies;
        registerCommands();
        init();
    }
    
    public void setDataStore(SensorDataStore dataStore) {
        this.dataStore = dataStore;
        this.safetyChecker = new AsimovSafetyChecker(dataStore); // Initialize Asimov's Laws checker
        registerSensorCommands(); // Register sensor analysis commands once dataStore is set
    }
    
    /**
     * Execute a command with the specified priority level.
     * Returns true if command was executed, false if blocked.
     */
    public boolean executeWithPriority(String cmdKey, String[] args, CommandPriority priority) {
        // Check if this priority can interrupt current command
        if (priority.mustYieldTo(currentCommandPriority)) {
            sendLog(String.format("Command '%s' blocked: priority %s cannot interrupt %s", 
                                cmdKey, priority, currentCommandPriority));
            return false;
        }
        
        // Check Asimov's Three Laws (SAFETY priority - always enforced)
        if (safetyChecker != null) {
            AsimovSafetyChecker.SafetyViolation violation = safetyChecker.checkCommand(cmdKey, args);
            if (violation != null) {
                // First Law violations are hard blocks
                if (violation.isHardBlock()) {
                    sendLog("BLOCKED: " + violation.toString());
                    say("Safety block", true); // Beep to alert
                    return false;
                }
                // Third Law violations are warnings (can be overridden by SERVER priority)
                else if (priority != CommandPriority.SERVER) {
                    sendLog("WARNING: " + violation.toString());
                    // Still execute but log the warning
                }
            }
        }
        
        // Update current priority if higher priority command
        if (priority.canInterrupt(currentCommandPriority)) {
            sendLog(String.format("Priority escalation: %s -> %s for command '%s'",
                                currentCommandPriority, priority, cmdKey));
            // Interrupt any running lower-priority command
            if (currentCommandThread != null && currentCommandThread.isAlive()) {
                currentCommandThread.interrupt();
            }
        }
        
        currentCommandPriority = priority;
        
        // Execute the command
        ICommand cmd = commandMap.get(cmdKey);
        if (cmd != null) {
            cmd.execute(args, this);
            return true;
        }
        
        return false;
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
    
    // Register sensor analysis commands after dataStore is set
    private void registerSensorCommands() {
        if (dataStore != null) {
            commandMap.put("ANALYZE", new AnalyzeSensorsCommand(dataStore));
            commandMap.put("NAV_SUGGEST", new NavigationSuggestCommand(dataStore));
            commandMap.put("SENSOR_STATS", new SensorStatsCommand(dataStore));
        }
    }
    
    /**
     * Register autonomous mode command.
     */
    public void setAutonomousMode(AutonomousMode autoMode) {
        if (autoMode != null) {
            commandMap.put("AUTO", new AutoCommand(autoMode));
        }
    }

    private void init() {
        String motorSummary = MotorDetector.getMotorSummary();
        LCD.drawString("Motors: OK", 0, 2);
        try {
            send(out, "MOTOR: " + motorSummary);
        } catch (IOException e) {
            // Motor detection error - don't clutter display
        }
    }

    public void run() {
        int replyIndex = 0;
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                // Detect command priority based on message prefix
                CommandPriority priority = CommandPriority.USER;
                String actualLine = line;
                
                if (line.startsWith("SERVER:")) {
                    priority = CommandPriority.SERVER;
                    actualLine = line.substring(7); // Remove "SERVER:" prefix
                } else if (line.startsWith("AUTO:")) {
                    priority = CommandPriority.AUTONOMOUS;
                    actualLine = line.substring(5); // Remove "AUTO:" prefix
                }
                
                // Use unified parser
                CommandParser.ParsedCommand parsed = CommandParser.parse(actualLine);
                final String cmdKey = parsed.getCommand();
                String[] parts = parsed.getArgs();

                // Handle SET_DEBUG specially
                if (cmdKey.equals("SET_DEBUG") && parts.length > 1) {
                    setDebug("1".equals(parts[1]));
                    sendLog("Debug mode set to " + debug);
                    continue;
                }

                final ICommand cmd = commandMap.get(cmdKey);
                if (cmd != null) {
                    // Execute with priority checking
                    final CommandPriority cmdPriority = priority;
                    final String[] cmdParts = parts;
                    
                    // Movement commands should run in separate thread to avoid blocking
                    boolean isMovementCommand = "MOVE".equalsIgnoreCase(cmdKey) || 
                                              "FWD".equalsIgnoreCase(cmdKey) || 
                                              "FORWARD".equalsIgnoreCase(cmdKey) ||
                                              "BWD".equalsIgnoreCase(cmdKey) || 
                                              "BACKWARD".equalsIgnoreCase(cmdKey) ||
                                              "BACK".equalsIgnoreCase(cmdKey) ||
                                              "LEFT".equalsIgnoreCase(cmdKey) || 
                                              "TURNLEFT".equalsIgnoreCase(cmdKey) ||
                                              "RIGHT".equalsIgnoreCase(cmdKey) || 
                                              "TURNRIGHT".equalsIgnoreCase(cmdKey) ||
                                              "ROTATE".equalsIgnoreCase(cmdKey) ||
                                              "MOVE_AND_LOG".equalsIgnoreCase(cmdKey) ||
                                              "BEEP".equalsIgnoreCase(cmdKey); // BEEP can block with multiple beeps
                    
                    if (isMovementCommand) {
                        // Run movement commands in separate thread
                        currentCommandThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                executeWithPriority(cmdKey, cmdParts, cmdPriority);
                            }
                        }, "movement-thread");
                        currentCommandThread.start();
                    } else if ("STOP".equalsIgnoreCase(cmdKey)) {
                        // STOP executes immediately with SAFETY priority (non-blocking)
                        executeWithPriority(cmdKey, parts, CommandPriority.SAFETY);
                    } else {
                        // Other commands execute directly
                        boolean executed = executeWithPriority(cmdKey, parts, priority);
                        if (executed && "BYE".equalsIgnoreCase(cmdKey)) {
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
