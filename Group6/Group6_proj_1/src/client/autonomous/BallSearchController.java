package client.autonomous;

import client.config.RobotConfig;
import client.motor.DifferentialDrive;
import client.sensor.ISensor;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.utility.Delay;

// Autonomous ball search - explores environment and approaches detected balls
public class BallSearchController {
    
    private boolean enabled = false;
    private boolean running = false;
    private Thread searchThread = null;
    
    private BallDetector ballDetector;
    private final DifferentialDrive drive;
    
    private int ballsFound = 0;
    private int scanAttempts = 0;
    
    public BallSearchController(ISensor ultrasonicSensor, ISensor infraredSensor, ISensor gyroSensor, ISensor colorSensor, java.io.BufferedWriter out) {
        this.ballDetector = new BallDetector(
            ultrasonicSensor != null ? ultrasonicSensor : infraredSensor, 
            infraredSensor,
            gyroSensor, 
            colorSensor,
            out
        );
        
        this.drive = new DifferentialDrive();
    }
    
    /**
     * Enable/disable autonomous search mode.
     * 
     * @param enabled true to start searching, false to stop
     */
    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return; // No change
        }
        
        this.enabled = enabled;
        
        if (enabled) {
            startSearch();
        } else {
            stopSearch();
        }
    }
    
    public synchronized void toggle() {
        setEnabled(!enabled);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getBallsFound() {
        return ballsFound;
    }
    
    public int getScanAttempts() {
        return scanAttempts;
    }
    
    private void startSearch() {
        if (running) {
            return;
        }
        
        ballsFound = 0;
        scanAttempts = 0;
        
        running = true;
        searchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                searchLoop();
            }
        }, "ball-search");
        searchThread.start();
        
        LCD.clear(1);
        LCD.drawString("AUTOSEARCH: ON", 0, 1);
        Sound.beep();
    }
    
    private void stopSearch() {
        running = false;
        
        // Stop ball detector if running
        if (ballDetector != null) {
            ballDetector.stop();
        }
        
        // Wait for thread to finish
        if (searchThread != null) {
            try {
                searchThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            searchThread = null;
        }
        
        drive.stop();
        
        LCD.clear(1);
        LCD.drawString("AUTOSEARCH: OFF", 0, 1);
        LCD.drawString("Found: " + ballsFound, 0, 2);
        Sound.twoBeeps();
    }
    
    private void searchLoop() {
        while (running && !Button.ESCAPE.isDown()) {
            try {
                LCD.clear(1);
                LCD.drawString("AUTOSEARCH RUN", 0, 1);
                LCD.drawString("Found:" + ballsFound, 0, 2);
                
                scanAttempts++;
                boolean found = ballDetector.searchAndApproachBall();
                
                if (!running) break;
                
                if (found) {
                    ballsFound++;
                    announceResult("BALL #" + ballsFound + " FOUND!");
                    retreatAndPivot();
                } else {
                    announceResult("Searching...");
                    searchTurn();
                }
            } catch (Exception e) {
                LCD.clear(1);
                LCD.drawString("SEARCH ERR", 0, 1);
                Delay.msDelay(1000);
            }
        }
        
        drive.stop();
        running = false;
    }
    
    /**
     * Get status summary string.
     */
    public String getStatusSummary() {
        return String.format("AUTOSEARCH: %s | Found: %d | Attempts: %d",
            enabled ? "ON" : "OFF", ballsFound, scanAttempts);
    }
    
    private void announceResult(String message) {
        LCD.clear(1);
        LCD.drawString(message, 0, 1);
        Sound.beep();
    }

    private void retreatAndPivot() {
        drive.move(false, RobotConfig.SEARCH_MOTOR_SPEED);
        Delay.msDelay(800);
        drive.stop();
        drive.rotateDegrees(RobotConfig.SEARCH_TURN_ANGLE_DEGREES * 2, RobotConfig.COMMAND_TURN_SPEED);
        Delay.msDelay(200);
    }

    private void searchTurn() {
        drive.stop();
        drive.rotateDegrees(RobotConfig.SEARCH_TURN_ANGLE_DEGREES, RobotConfig.COMMAND_TURN_SPEED);
        Delay.msDelay(300);
    }
}

