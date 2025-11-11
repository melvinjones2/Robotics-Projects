package client.safety;

import client.commands.CommandParser;
import client.network.CommandHandler;
import client.sensor.data.SensorAnalyzer;
import client.sensor.data.SensorAnalyzer.NavigationSuggestion;
import client.sensor.data.SensorDataStore;

/**
 * Autonomous navigation mode that uses sensor analysis to make decisions.
 * Can be toggled on/off and runs periodically to check for obstacles.
 */
public class AutonomousMode {
    
    private final SensorAnalyzer analyzer;
    private final CommandHandler handler;
    private volatile boolean enabled = false;
    private volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 500; // Check every 500ms
    
    public AutonomousMode(SensorDataStore dataStore, CommandHandler handler) {
        this.analyzer = new SensorAnalyzer(dataStore);
        this.handler = handler;
    }
    
    /**
     * Enable or disable autonomous mode.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            handler.sendLog("Autonomous mode ENABLED");
        } else {
            handler.sendLog("Autonomous mode DISABLED");
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check sensors and take action if needed.
     * Should be called periodically from main loop.
     */
    public void update() {
        if (!enabled) {
            return;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime = now;
        
        // Check for emergency stop conditions
        if (analyzer.shouldEmergencyStop()) {
            handler.sendLog("EMERGENCY STOP triggered by sensor");
            executeCommand("STOP");
            setEnabled(false); // Disable autonomous mode on emergency
            return;
        }
        
        // Get navigation suggestion
        NavigationSuggestion suggestion = analyzer.suggestNavigation();
        
        // Execute suggested action
        switch (suggestion.action) {
            case FORWARD:
                // Already moving or should move forward
                break;
            case BACKWARD:
                handler.sendLog("Auto: Backing up - " + suggestion.reason);
                executeCommand("BWD 300 1000");
                break;
            case TURN_LEFT:
                handler.sendLog("Auto: Turning left - " + suggestion.reason);
                executeCommand("LEFT 200 500");
                break;
            case TURN_RIGHT:
                handler.sendLog("Auto: Turning right - " + suggestion.reason);
                executeCommand("RIGHT 200 500");
                break;
            case STOP:
                handler.sendLog("Auto: Stopping - " + suggestion.reason);
                executeCommand("STOP");
                break;
        }
    }
    
    private void executeCommand(String cmdString) {
        try {
            CommandParser.ParsedCommand parsed = CommandParser.parse(cmdString);
            String[] args = parsed.getArgs();
            String cmd = parsed.getCommand();
            
            // Execute with AUTONOMOUS priority
            boolean executed = handler.executeWithPriority(cmd, args, CommandPriority.AUTONOMOUS);
            
            if (executed) {
                handler.sendLog("Auto executed: " + cmdString);
            } else {
                handler.sendLog("Auto blocked: " + cmdString);
            }
        } catch (Exception e) {
            handler.sendLog("Auto command error: " + e.getMessage());
        }
    }
}
