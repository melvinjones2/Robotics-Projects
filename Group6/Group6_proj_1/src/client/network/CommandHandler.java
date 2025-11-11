package client.network;

import client.motor.MotorFactory;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.Sound;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles incoming commands from the server and executes them on the robot.
 * 
 * <p>This handler operates in a "thin client" model where the server sends
 * commands and the client executes them immediately. The handler supports:
 * <ul>
 *   <li>Movement commands (MOVE, FWD, BWD, LEFT, RIGHT)</li>
 *   <li>Individual motor control (MOVE A 200, STOP B)</li>
 *   <li>Stop commands (STOP, STOP [port])</li>
 *   <li>Audio feedback (BEEP)</li>
 *   <li>Disconnect command (BYE)</li>
 * </ul>
 * 
 * <p>Commands are displayed on LCD line 1, except for TICK_ACK messages which
 * are filtered out to avoid clutter.
 * 
 * @author Group 6
 * @version 2.0
 */
public class CommandHandler implements Runnable {
    
    private static final int DEFAULT_SPEED = 300;
    private static final int DEFAULT_TURN_SPEED = 300;
    private static final int MAX_SPEED = 900;
    private static final int MIN_SPEED = 0;
    private static final int MAX_BEEP_COUNT = 5;
    private static final int BEEP_INTERVAL_MS = 200;
    
    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicBoolean running;
    
    /**
     * Constructs a new command handler.
     * 
     * @param in      Input stream for receiving commands from server
     * @param out     Output stream for sending responses to server
     * @param running Shared flag indicating if the client should continue running
     */
    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running) {
        this.in = in;
        this.out = out;
        this.running = running;
    }
    
    /**
     * Main command processing loop. Reads commands from server and executes them.
     * Continues until connection is lost or running flag is set to false.
     */
    @Override
    public void run() {
        LCD.clear(1);
        LCD.drawString("Ready", 0, 1);
        
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Parse command first to check if we should display it
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toUpperCase();
                
                // Don't display TICK_ACK messages on LCD
                if (!cmd.equals("TICK_ACK") && !line.startsWith("TICK_ACK")) {
                    // Show command on LCD line 1 (line 0=status, lines 2-7=sensors)
                    LCD.clear(1);
                    String display = line.substring(0, Math.min(18, line.length()));
                    LCD.drawString(display, 0, 1);
                }
                
                try {
                    executeCommand(cmd, parts);
                } catch (Exception e) {
                    LCD.clear(1);
                    String err = "ERR:" + (e.getMessage() != null ? e.getMessage() : "Unknown");
                    LCD.drawString(err.substring(0, Math.min(18, err.length())), 0, 1);
                    Sound.buzz();
                }
            }
        } catch (IOException e) {
            LCD.clear(1);
            LCD.drawString("Conn Lost", 0, 1);
        } finally {
            running.set(false);
            stopAllMotors();
        }
    }
    
    /**
     * Parses and executes a command from the server.
     * 
     * @param cmd   Command keyword (uppercase)
     * @param parts All parts of the command split by whitespace
     * @throws IOException if communication error occurs
     */
    private void executeCommand(String cmd, String[] parts) throws IOException {
        switch (cmd) {
            case "MOVE":
            case "FWD":
            case "FORWARD":
                // Check if it's a single motor command: MOVE A 200
                if (parts.length > 2 && parts[1].length() == 1 && Character.isLetter(parts[1].charAt(0))) {
                    handleSingleMotor(parts[1].charAt(0), Integer.parseInt(parts[2]), true);
                } else {
                    handleMove(parts, true);
                }
                break;
                
            case "BWD":
            case "BACK":
            case "BACKWARD":
                handleMove(parts, false);
                break;
                
            case "LEFT":
                handleTurn(true);
                break;
                
            case "RIGHT":
                handleTurn(false);
                break;
                
            case "STOP":
                // Check if it's a single motor stop: STOP A
                if (parts.length > 1 && parts[1].length() == 1 && Character.isLetter(parts[1].charAt(0))) {
                    stopSingleMotor(parts[1].charAt(0));
                } else {
                    stopAllMotors();
                }
                break;
                
            case "BEEP":
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                for (int i = 0; i < Math.min(count, MAX_BEEP_COUNT); i++) {
                    Sound.beep();
                    try {
                        Thread.sleep(BEEP_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                break;
                
            case "BYE":
                send("BYE:");
                running.set(false);
                break;
            
            case "TICK_ACK":
                // Ignore TICK_ACK messages - don't display on LCD
                break;
                
            default:
                // Unknown command - ignore silently
                break;
        }
    }
    
    /**
     * Handles dual motor movement (forward/backward).
     * 
     * @param parts   Command parts where parts[1] may contain speed
     * @param forward true for forward movement, false for backward
     */
    private void handleMove(String[] parts, boolean forward) {
        int speed = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_SPEED;
        speed = clampSpeed(speed);
        
        BaseRegulatedMotor leftMotor = MotorFactory.getMotor('B');
        BaseRegulatedMotor rightMotor = MotorFactory.getMotor('C');
        
        if (leftMotor != null && rightMotor != null) {
            leftMotor.setSpeed(speed);
            rightMotor.setSpeed(speed);
            
            if (forward) {
                leftMotor.forward();
                rightMotor.forward();
            } else {
                leftMotor.backward();
                rightMotor.backward();
            }
        }
    }
    
    /**
     * Handles differential steering for turning.
     * 
     * @param left true to turn left, false to turn right
     */
    private void handleTurn(boolean left) {
        BaseRegulatedMotor leftMotor = MotorFactory.getMotor('B');
        BaseRegulatedMotor rightMotor = MotorFactory.getMotor('C');
        
        if (leftMotor != null && rightMotor != null) {
            leftMotor.setSpeed(DEFAULT_TURN_SPEED);
            rightMotor.setSpeed(DEFAULT_TURN_SPEED);
            
            if (left) {
                leftMotor.backward();
                rightMotor.forward();
            } else {
                leftMotor.forward();
                rightMotor.backward();
            }
        }
    }
    
    /**
     * Handles individual motor control.
     * 
     * @param motorPort Motor port identifier (A, B, C, or D)
     * @param speed     Speed in degrees per second
     * @param forward   true for forward, false for backward
     */
    private void handleSingleMotor(char motorPort, int speed, boolean forward) {
        BaseRegulatedMotor motor = MotorFactory.getMotor(motorPort);
        if (motor != null) {
            speed = clampSpeed(speed);
            motor.setSpeed(speed);
            if (forward) {
                motor.forward();
            } else {
                motor.backward();
            }
        }
    }
    
    /**
     * Stops a single motor.
     * 
     * @param motorPort Motor port identifier (A, B, C, or D)
     */
    private void stopSingleMotor(char motorPort) {
        BaseRegulatedMotor motor = MotorFactory.getMotor(motorPort);
        if (motor != null) {
            motor.stop(true);
        }
    }
    
    /**
     * Emergency stop for all motors.
     * Stops motors A, B, C, and D immediately.
     */
    private void stopAllMotors() {
        char[] motorPorts = {'A', 'B', 'C', 'D'};
        for (char port : motorPorts) {
            BaseRegulatedMotor motor = MotorFactory.getMotor(port);
            if (motor != null) {
                motor.stop(true);
            }
        }
    }
    
    /**
     * Clamps speed to valid range.
     * 
     * @param speed Speed to clamp
     * @return Speed clamped between MIN_SPEED and MAX_SPEED
     */
    private int clampSpeed(int speed) {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }
    
    /**
     * Sends a message to the server.
     * 
     * @param msg Message to send (newline will be appended)
     * @throws IOException if communication error occurs
     */
    private void send(String msg) throws IOException {
        out.write(msg + "\n");
        out.flush();
    }
}
