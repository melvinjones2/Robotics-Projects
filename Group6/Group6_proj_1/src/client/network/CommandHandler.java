package client.network;

import client.autonomous.BallDetector;
import client.autonomous.BallSearchController;
import client.config.RobotConfig;
import client.motor.DifferentialDrive;
import client.motor.MotorFactory;
import client.sensor.ISensor;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.Sound;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Handles commands from server and executes them on robot
public class CommandHandler implements Runnable {
    
    private final BufferedReader in;
    private final BufferedWriter out;
    private final AtomicBoolean running;
    private final List<ISensor> sensors;
    private final DifferentialDrive drive;
    private BallDetector ballDetector;
    private BallSearchController ballSearchController;
    private Thread ballScanThread;
    
    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, List<ISensor> sensors) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.sensors = sensors;
        this.drive = new DifferentialDrive();
        this.ballDetector = null;
        this.ballSearchController = null;
    }
    
    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running) {
        this(in, out, running, null);
    }
    
    @Override
    public void run() {
        LCD.clear(1);
        LCD.drawString("Ready", 0, 1);
        
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toUpperCase();
                
                if (!cmd.equals("TICK_ACK") && !line.startsWith("TICK_ACK")) {
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    String display = line.substring(0, Math.min(RobotConfig.LCD_MAX_WIDTH, line.length()));
                    LCD.drawString(display, 0, RobotConfig.LCD_COMMAND_LINE);
                }
                
                try {
                    executeCommand(cmd, parts);
                } catch (Exception e) {
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    String err = "ERR:" + (e.getMessage() != null ? e.getMessage() : "Unknown");
                    LCD.drawString(err.substring(0, Math.min(RobotConfig.LCD_MAX_WIDTH, err.length())), 0, RobotConfig.LCD_COMMAND_LINE);
                    Sound.buzz();
                }
            }
        } catch (IOException e) {
            LCD.clear(1);
            LCD.drawString("Conn Lost", 0, 1);
        } finally {
            running.set(false);
            shutdownBallTasks();
            MotorFactory.stopAll();
        }
    }
    
    private void executeCommand(String cmd, String[] parts) throws IOException {
        switch (cmd) {
            case "MOVE":
            case "FWD":
            case "FORWARD":
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
                
            case "ROTATE":
                if (parts.length >= 3) {
                    if (parts[1].equalsIgnoreCase("ROBOT")) {
                        handleRotate('R', Integer.parseInt(parts[2]));
                    } else if (parts[1].length() == 1 && Character.isLetter(parts[1].charAt(0))) {
                        handleRotate(parts[1].charAt(0), Integer.parseInt(parts[2]));
                    }
                }
                break;
                
            case "STOP":
                if (parts.length > 1 && parts[1].length() == 1 && Character.isLetter(parts[1].charAt(0))) {
                    stopSingleMotor(parts[1].charAt(0));
                } else {
                    shutdownBallTasks();
                    MotorFactory.stopAll();
                }
                break;
                
            case "BEEP":
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                for (int i = 0; i < Math.min(count, RobotConfig.COMMAND_MAX_BEEP_COUNT); i++) {
                    Sound.beep();
                    try {
                        Thread.sleep(RobotConfig.COMMAND_BEEP_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                break;
                
            case "SCAN":
                handleScan();
                break;
                
            case "AUTOSEARCH":
                String mode = parts.length > 1 ? parts[1].toUpperCase() : "TOGGLE";
                handleAutoSearch(mode);
                break;
                
            case "ARM":
                if (parts.length > 1) {
                    handleArm(parts[1]);
                }
                break;
                
            case "SETCOLOR":
                if (parts.length > 1) {
                    handleSetColor(Integer.parseInt(parts[1]));
                }
                break;
                
            case "BYE":
                send("BYE:");
                running.set(false);
                break;
            
            case "TICK_ACK":
                break;
                
            default:
                break;
        }
    }
    
    private void handleMove(String[] parts, boolean forward) {
        int speed = parts.length > 1 ? Integer.parseInt(parts[1]) : RobotConfig.COMMAND_DEFAULT_SPEED;
        drive.move(forward, speed);
    }
    
    private void handleTurn(boolean left) {
        drive.turnInPlace(left, RobotConfig.COMMAND_TURN_SPEED);
    }
    
    /**
     * Handles robot rotation by a specific number of degrees.
     * Rotates both drive motors (B and C) in opposite directions to turn the robot.
     * 
     * @param motorPort Not used (kept for backward compatibility with command format)
     * @param degrees   Degrees to rotate the robot (positive=right, negative=left)
     */
    private void handleRotate(char motorPort, int degrees) {
        if (motorPort == 'R' || motorPort == 'r') {
            drive.rotateDegrees(degrees);
            return;
        }
        
        BaseRegulatedMotor motor = MotorFactory.getMotor(motorPort);
        if (motor != null) {
            motor.rotate(degrees, false);
        }
    }
    
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
    
    private void stopSingleMotor(char motorPort) {
        BaseRegulatedMotor motor = MotorFactory.getMotor(motorPort);
        if (motor != null) {
            motor.stop(true);
        }
    }
    
    private void handleScan() {
        if (ballDetector == null && sensors != null) {
            ISensor ultrasonicSensor = findSensor("ultrasonic");
            ISensor infraredSensor = findSensor("infrared");
            ISensor gyroSensor = findSensor("gyro");
            ISensor colorSensor = findSensor("light");
            
            ballDetector = new BallDetector(ultrasonicSensor, infraredSensor, gyroSensor, colorSensor, out);
        }
        
        if (ballDetector != null) {
            stopActiveScan();
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("SCANNING...", 0, RobotConfig.LCD_COMMAND_LINE);
            
            ballScanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean found = ballDetector.searchAndApproachBall();
                        sendSilently(found ? "SCAN:FOUND" : "SCAN:NOT_FOUND");
                    } catch (Exception e) {
                        sendSilently("SCAN:ERROR");
                    } finally {
                        ballScanThread = null;
                    }
                }
            }, "ball-scan");
            
            ballScanThread.start();
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("SCAN: No sensors", 0, RobotConfig.LCD_COMMAND_LINE);
            sendSilently("SCAN:NO_SENSORS");
        }
    }
    
    private void handleAutoSearch(String mode) {
        if (ballSearchController == null && sensors != null) {
            ISensor ultrasonicSensor = findSensor("ultrasonic");
            ISensor infraredSensor = findSensor("infrared");
            ISensor gyroSensor = findSensor("gyro");
            ISensor colorSensor = findSensor("light");
            
            ballSearchController = new BallSearchController(ultrasonicSensor, infraredSensor, gyroSensor, colorSensor, out);
        }
        
        if (ballSearchController != null) {
            switch (mode) {
                case "ON":
                    ballSearchController.setEnabled(true);
                    sendSilently("AUTOSEARCH:ON");
                    break;
                    
                case "OFF":
                    ballSearchController.setEnabled(false);
                    sendSilently("AUTOSEARCH:OFF");
                    break;
                    
                case "TOGGLE":
                default:
                    ballSearchController.toggle();
                    sendSilently("AUTOSEARCH:" + (ballSearchController.isEnabled() ? "ON" : "OFF"));
                    break;
            }
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("AUTOSEARCH: No Dist", 0, RobotConfig.LCD_COMMAND_LINE);
            sendSilently("AUTOSEARCH:NO_SENSORS");
        }
    }
    
    public BallSearchController getBallSearchController() {
        return ballSearchController;
    }
    
    private ISensor findSensor(String name) {
        if (sensors == null) return null;
        
        for (ISensor sensor : sensors) {
            if (sensor != null && sensor.getName().equalsIgnoreCase(name)) {
                if (sensor.isAvailable()) {
                    return sensor;
                }
            }
        }
        return null;
    }
    
    private void handleArm(String parameter) {
        BaseRegulatedMotor armMotor = MotorFactory.getMotor(RobotConfig.ARM_MOTOR_PORT);
        
        if (armMotor != null) {
            armMotor.setSpeed(RobotConfig.ARM_SPEED);
            
            int targetPosition;
            String action;
            
            if ("UP".equalsIgnoreCase(parameter)) {
                targetPosition = RobotConfig.ARM_UP_POSITION;
                action = "UP";
            } else if ("DOWN".equalsIgnoreCase(parameter)) {
                targetPosition = RobotConfig.ARM_DOWN_POSITION;
                action = "DOWN";
            } else {
                try {
                    targetPosition = Integer.parseInt(parameter);
                    action = parameter + "deg";
                } catch (NumberFormatException e) {
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    LCD.drawString("ARM: Invalid", 0, RobotConfig.LCD_COMMAND_LINE);
                    return;
                }
            }
            
            armMotor.rotateTo(targetPosition, false);
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("ARM: " + action, 0, RobotConfig.LCD_COMMAND_LINE);
            
            sendSilently("ARM:" + action);
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("ARM: No Motor", 0, RobotConfig.LCD_COMMAND_LINE);
        }
    }
    
    private void handleSetColor(int colorId) {
        if (colorId >= 1 && colorId <= 6) {
            RobotConfig.TARGET_BALL_COLOR_ID = colorId;
            
            String[] colorNames = {"", "Black", "Blue", "Green", "Yellow", "Red", "White"};
            String colorName = colorNames[colorId];
            
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("Color: " + colorName, 0, RobotConfig.LCD_COMMAND_LINE);
            
            sendSilently("SETCOLOR:" + colorName);
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("Color: Invalid", 0, RobotConfig.LCD_COMMAND_LINE);
        }
    }
    
    private void send(String msg) throws IOException {
        out.write(msg + "\n");
        out.flush();
    }

    private void sendSilently(String msg) {
        try {
            send(msg);
        } catch (IOException e) {
            // Connection already failed; ignore
        }
    }

    private int clampSpeed(int speed) {
        return DifferentialDrive.clampSpeed(speed);
    }
    
    private void shutdownBallTasks() {
        if (ballSearchController != null && ballSearchController.isEnabled()) {
            ballSearchController.setEnabled(false);
        }
        if (ballDetector != null) {
            ballDetector.stop();
        }
        stopActiveScan();
    }
    
    private void stopActiveScan() {
        if (ballScanThread != null && ballScanThread.isAlive()) {
            if (ballDetector != null) {
                ballDetector.stop();
            }
            try {
                ballScanThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ballScanThread = null;
        }
    }
}
