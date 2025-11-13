package client.network;

import client.autonomous.BallDetector;
import client.autonomous.BallSearchController;
import client.config.RobotConfig;
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
    private BallDetector ballDetector;
    private BallSearchController ballSearchController;
    
    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, List<ISensor> sensors) {
        this.in = in;
        this.out = out;
        this.running = running;
        this.sensors = sensors;
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
            stopAllMotors();
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
                    stopAllMotors();
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
        speed = clampSpeed(speed);
        
        BaseRegulatedMotor leftMotor = MotorFactory.getMotor(RobotConfig.LEFT_MOTOR_PORT);
        BaseRegulatedMotor rightMotor = MotorFactory.getMotor(RobotConfig.RIGHT_MOTOR_PORT);
        
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
    
    private void handleTurn(boolean left) {
        BaseRegulatedMotor leftMotor = MotorFactory.getMotor(RobotConfig.LEFT_MOTOR_PORT);
        BaseRegulatedMotor rightMotor = MotorFactory.getMotor(RobotConfig.RIGHT_MOTOR_PORT);
        
        if (leftMotor != null && rightMotor != null) {
            leftMotor.setSpeed(RobotConfig.COMMAND_TURN_SPEED);
            rightMotor.setSpeed(RobotConfig.COMMAND_TURN_SPEED);
            
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
     * Handles robot rotation by a specific number of degrees.
     * Rotates both drive motors (B and C) in opposite directions to turn the robot.
     * 
     * @param motorPort Not used (kept for backward compatibility with command format)
     * @param degrees   Degrees to rotate the robot (positive=right, negative=left)
     */
    private void handleRotate(char motorPort, int degrees) {
        BaseRegulatedMotor leftMotor = MotorFactory.getMotor(RobotConfig.LEFT_MOTOR_PORT);
        BaseRegulatedMotor rightMotor = MotorFactory.getMotor(RobotConfig.RIGHT_MOTOR_PORT);
        
        if (leftMotor != null && rightMotor != null) {
            // Set moderate speed for rotation
            leftMotor.setSpeed(RobotConfig.ROTATION_SPEED);
            rightMotor.setSpeed(RobotConfig.ROTATION_SPEED);
            
            // Calculate motor rotation degrees
            // This is an approximation - adjust ROTATION_MULTIPLIER in RobotConfig if needed
            int motorDegrees = (int) Math.abs(degrees * RobotConfig.ROTATION_MULTIPLIER);
            
            if (degrees > 0) {
                // Turn right: left motor forward, right motor backward
                leftMotor.rotate(motorDegrees, true);
                rightMotor.rotate(-motorDegrees, false);
            } else {
                // Turn left: left motor backward, right motor forward
                leftMotor.rotate(-motorDegrees, true);
                rightMotor.rotate(motorDegrees, false);
            }
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
    
    private void stopAllMotors() {
        char[] motorPorts = {'A', 'B', 'C', 'D'};
        for (char port : motorPorts) {
            BaseRegulatedMotor motor = MotorFactory.getMotor(port);
            if (motor != null) {
                motor.stop(true);
            }
        }
    }
    
    private int clampSpeed(int speed) {
        return Math.max(RobotConfig.MIN_MOTOR_SPEED, Math.min(RobotConfig.MAX_MOTOR_SPEED, speed));
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
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("SCANNING...", 0, RobotConfig.LCD_COMMAND_LINE);
            
            Thread scanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean found = ballDetector.searchAndApproachBall();
                        if (found) {
                            send("SCAN:FOUND");
                        } else {
                            send("SCAN:NOT_FOUND");
                        }
                    } catch (Exception e) {
                        try {
                            send("SCAN:ERROR");
                        } catch (IOException ex) {
                            // Can't send error message
                        }
                    }
                }
            }, "ball-scan");
            
            scanThread.start();
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("SCAN: No sensors", 0, RobotConfig.LCD_COMMAND_LINE);
            try {
                send("SCAN:NO_SENSORS");
            } catch (IOException e) {
                // Can't send error message
            }
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
                    try {
                        send("AUTOSEARCH:ON");
                    } catch (IOException e) {
                    }
                    break;
                    
                case "OFF":
                    ballSearchController.setEnabled(false);
                    try {
                        send("AUTOSEARCH:OFF");
                    } catch (IOException e) {
                    }
                    break;
                    
                case "TOGGLE":
                default:
                    ballSearchController.toggle();
                    try {
                        send("AUTOSEARCH:" + (ballSearchController.isEnabled() ? "ON" : "OFF"));
                    } catch (IOException e) {
                    }
                    break;
            }
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("AUTOSEARCH: No Dist", 0, RobotConfig.LCD_COMMAND_LINE);
            try {
                send("AUTOSEARCH:NO_SENSORS");
            } catch (IOException e) {
            }
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
                    action = parameter + "°";
                } catch (NumberFormatException e) {
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    LCD.drawString("ARM: Invalid", 0, RobotConfig.LCD_COMMAND_LINE);
                    return;
                }
            }
            
            armMotor.rotateTo(targetPosition, false);
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("ARM: " + action, 0, RobotConfig.LCD_COMMAND_LINE);
            
            try {
                send("ARM:" + action);
            } catch (IOException e) {
            }
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
            
            try {
                send("SETCOLOR:" + colorName);
            } catch (IOException e) {
            }
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("Color: Invalid", 0, RobotConfig.LCD_COMMAND_LINE);
        }
    }
    
    private void send(String msg) throws IOException {
        out.write(msg + "\n");
        out.flush();
    }
}
