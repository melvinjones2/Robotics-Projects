package client.network.command;

import client.autonomous.BallDetector;
import client.config.RobotConfig;
import client.sensor.ISensor;
import common.ParsedCommand;
import lejos.hardware.lcd.LCD;

import java.io.IOException;

// 360-degree ball scan and approach. Syntax: SCAN
public class ScanCommand implements ICommand {
    
    private final ParsedCommand parsedCmd;
    
    public ScanCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }
    
    @Override
    public void execute(final CommandContext context) throws IOException {
        // Initialize ball detector if needed
        if (context.getBallDetector() == null && context.getSensors() != null) {
            ISensor ultrasonicSensor = context.findSensor("ultrasonic");
            ISensor infraredSensor = context.findSensor("infrared");
            ISensor gyroSensor = context.findSensor("gyro");
            ISensor colorSensor = context.findSensor("light");
            
            // Pass warehouse for thread-safe sensor data access
            BallDetector detector = new BallDetector(ultrasonicSensor, infraredSensor, gyroSensor, colorSensor, 
                                                     context.getOut(), context.getWarehouse());
            context.setBallDetector(detector);
        }
        
        if (context.getBallDetector() != null) {
            stopActiveScan(context);
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("SCANNING...", 0, RobotConfig.LCD_COMMAND_LINE);
            
            Thread scanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean found = context.getBallDetector().searchAndApproachBall();
                        send(context, found ? "SCAN:FOUND" : "SCAN:NOT_FOUND");
                    } catch (Exception e) {
                        send(context, "SCAN:ERROR");
                    } finally {
                        context.setBallScanThread(null);
                    }
                }
            }, "ball-scan");
            
            context.setBallScanThread(scanThread);
            scanThread.start();
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("SCAN: No sensors", 0, RobotConfig.LCD_COMMAND_LINE);
            send(context, "SCAN:NO_SENSORS");
        }
    }
    
    @Override
    public String getName() {
        return "SCAN";
    }
    
    private void stopActiveScan(CommandContext context) {
        Thread ballScanThread = context.getBallScanThread();
        if (ballScanThread != null && ballScanThread.isAlive()) {
            if (context.getBallDetector() != null) {
                context.getBallDetector().stop();
            }
            try {
                ballScanThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            context.setBallScanThread(null);
        }
    }
    
    private void send(CommandContext context, String msg) {
        try {
            context.getOut().write(msg + "\n");
            context.getOut().flush();
        } catch (IOException e) {
            // Connection failed, ignore
        }
    }
}
