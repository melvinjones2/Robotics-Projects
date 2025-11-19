package client.network;

import client.autonomous.BallSearchController;
import client.config.RobotConfig;
import client.data.SensorDataWarehouse;
import client.motor.ArmController;
import client.motor.DifferentialDrive;
import client.motor.IArmController;
import client.motor.IDriveController;
import client.motor.MotorFactory;
import client.network.command.CommandContext;
import client.network.command.CommandFactory;
import client.network.command.ICommand;
import client.sensor.ISensor;
import common.ParsedCommand;
import common.ProtocolConstants;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Handles commands from server and executes them on robot
public class CommandHandler implements Runnable {
    
    private final BufferedReader in;
    private final CommandContext context;
    
    public CommandHandler(BufferedReader in, BufferedWriter out, AtomicBoolean running, 
                          List<ISensor> sensors, SensorDataWarehouse warehouse) {
        this.in = in;
        
        // Create command execution context with warehouse
        IDriveController drive = new DifferentialDrive();
        IArmController armController = new ArmController(RobotConfig.ARM_MOTOR_PORT);
        this.context = new CommandContext(drive, armController, sensors, out, running, warehouse);
    }
    
    @Override
    public void run() {
        LCD.clear(1);
        LCD.drawString("Ready", 0, 1);
        
        try {
            String line;
            while (context.getRunning().get() && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Parse command using type-safe parser
                ParsedCommand cmd = ProtocolConstants.parseCommand(line);
                
                if (!cmd.is("TICK_ACK")) {
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    String display = line.substring(0, Math.min(RobotConfig.LCD_MAX_WIDTH, line.length()));
                    LCD.drawString(display, 0, RobotConfig.LCD_COMMAND_LINE);
                }
                
                try {
                    executeCommand(cmd);
                } catch (Exception e) {
                    // Log exception details for debugging
                    System.err.println("Command execution error: " + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    String err = "ERR:" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    LCD.drawString(err.substring(0, Math.min(RobotConfig.LCD_MAX_WIDTH, err.length())), 0, RobotConfig.LCD_COMMAND_LINE);
                    Sound.buzz();
                }
            }
        } catch (IOException e) {
            LCD.clear(1);
            LCD.drawString("Conn Lost", 0, 1);
        } finally {
            context.getRunning().set(false);
            context.shutdownAutonomousTasks();
            MotorFactory.stopAll();
        }
    }
    
    private void executeCommand(ParsedCommand cmd) throws IOException {
        ICommand command = CommandFactory.createCommand(cmd);
        
        if (command != null) {
            command.execute(context);
        } else {
            LCD.clear(RobotConfig.LCD_COMMAND_LINE);
            LCD.drawString("Unknown cmd", 0, RobotConfig.LCD_COMMAND_LINE);
        }
    }
    
    public BallSearchController getBallSearchController() {
        return context.getBallSearchController();
    }
}
