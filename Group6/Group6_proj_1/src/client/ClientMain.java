package client;

import client.config.RobotConfig;
import client.motor.MotorFactory;
import client.network.HeartbeatThread;
import client.network.CommandHandler;
import client.network.SensorThread;
import client.sensor.ISensor;
import client.sensor.SensorFactory;
import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// EV3 robot client - connects to server, handles commands and sensor reporting
public class ClientMain {
    
    public static void main(String[] args) {
        displayStatus("Init", "Connecting...");
        
        AtomicBoolean running = new AtomicBoolean(true);
        Socket socket = null;
        
        try {
            // Connect to server
            socket = new Socket(RobotConfig.SERVER_HOST, RobotConfig.SERVER_PORT);
            socket.setSoTimeout(RobotConfig.SOCKET_TIMEOUT_MS);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            // Perform handshake with server
            if (!performHandshake(in, out)) {
                return;
            }
            
            displayStatus("OK", "Ready");
            
            // Initialize sensors
            List<ISensor> sensors = SensorFactory.createSensors(
                SensorFactory.getDefaultSensorConfig()
            );
            
            // Start threads
            Thread heartbeat = new Thread(
                new HeartbeatThread(out, running, RobotConfig.TICK_RATE_MS),
                "heartbeat"
            );
            
            // Store command handler reference for button control
            final CommandHandler cmdHandler = new CommandHandler(in, out, running, sensors);
            Thread commands = new Thread(cmdHandler, "commands");
            
            Thread sensorThread = new Thread(
                new SensorThread(out, running, sensors, RobotConfig.SENSOR_POLL_INTERVAL_MS),
                "sensors"
            );
            
            heartbeat.start();
            commands.start();
            sensorThread.start();
            
            while (running.get() && Button.ESCAPE.isUp()) {
                if (Button.DOWN.isDown()) {
                    if (cmdHandler.getBallSearchController() != null) {
                        cmdHandler.getBallSearchController().toggle();
                        while (Button.DOWN.isDown() && running.get()) {
                            Thread.sleep(50);
                        }
                    }
                }
                
                Thread.sleep(RobotConfig.BUTTON_POLL_INTERVAL_MS);
            }
            
            shutdown(running, heartbeat, commands, sensorThread);
            
        } catch (Exception e) {
            handleError(e);
        } finally {
            closeSocket(socket);
        }
        
        // Wait for button press before exit
        Button.waitForAnyPress();
    }
    
    private static boolean performHandshake(BufferedReader in, BufferedWriter out) throws IOException {
        LCD.clear(1);
        LCD.drawString("Handshake...", 0, 1);
        
        String response = in.readLine();
        if (response == null || !response.trim().equals("HELLO")) {
            displayError("Bad handshake", response);
            return false;
        }
        
        out.write("READY:0\n");
        out.flush();
        return true;
    }
    
    private static void displayStatus(String status, String message) {
        LCD.clear();
        LCD.drawString("Status: " + status, 0, 0);
        LCD.drawString(message, 0, 1);
    }
    
    private static void displayError(String message, String details) {
        LCD.clear();
        LCD.drawString("Status: ERROR", 0, 0);
        LCD.drawString(message, 0, 1);
        if (details != null && details.length() > 0) {
            LCD.drawString(details.substring(0, Math.min(RobotConfig.LCD_MAX_WIDTH, details.length())), 0, 2);
        }
    }
    
    private static void handleError(Exception e) {
        LCD.clear();
        LCD.drawString("Status: ERROR", 0, 0);
        String msg = e.getMessage();
        if (msg != null && msg.length() > 0) {
            LCD.drawString(msg.substring(0, Math.min(RobotConfig.LCD_MAX_WIDTH, msg.length())), 0, 1);
        } else {
            LCD.drawString(e.getClass().getSimpleName(), 0, 1);
        }
    }
    
    private static void shutdown(AtomicBoolean running, Thread heartbeat, 
                                 Thread commands, Thread sensorThread) {
        running.set(false);
        
        LCD.clear(0);
        LCD.drawString("Status: Exit", 0, 0);
        
        MotorFactory.stopAll();
        
        try {
            heartbeat.join(RobotConfig.THREAD_JOIN_TIMEOUT_MS);
            commands.join(RobotConfig.THREAD_JOIN_TIMEOUT_MS);
            sensorThread.join(RobotConfig.THREAD_JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }
}

