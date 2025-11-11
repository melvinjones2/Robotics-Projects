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

/**
 * Main entry point for the EV3 Robot Client.
 * 
 * <p>This client connects to a remote server and runs three concurrent threads:
 * <ul>
 *   <li>Heartbeat thread - Maintains connection with periodic TICK messages</li>
 *   <li>Command handler - Receives and executes commands from server</li>
 *   <li>Sensor thread - Reads sensors and reports data to server</li>
 * </ul>
 * 
 * <p>The client operates in a "thin client" architecture where the server handles all
 * decision-making logic, and the client simply executes commands and reports sensor data.
 * 
 * <p>LCD Layout:
 * <ul>
 *   <li>Line 0: Connection status (Init, OK, ERROR, Exit)</li>
 *   <li>Line 1: Last command received</li>
 *   <li>Lines 2-7: Sensor readings (6 lines available)</li>
 * </ul>
 * 
 * @author Group 6
 * @version 2.0
 */
public class ClientMain {
    
    private static final int THREAD_JOIN_TIMEOUT_MS = 1000;
    private static final int BUTTON_POLL_INTERVAL_MS = 100;
    private static final int LCD_MAX_WIDTH = 18;
    
    /**
     * Main entry point for the EV3 robot client application.
     * 
     * <p>Connects to the server, performs handshake, and starts worker threads
     * for heartbeat, command handling, and sensor monitoring. Runs until the
     * ESCAPE button is pressed or the connection is lost.
     * 
     * @param args Command line arguments (not used)
     */
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
            
            Thread commands = new Thread(
                new CommandHandler(in, out, running),
                "commands"
            );
            
            Thread sensorThread = new Thread(
                new SensorThread(out, running, sensors, RobotConfig.SENSOR_POLL_INTERVAL_MS),
                "sensors"
            );
            
            heartbeat.start();
            commands.start();
            sensorThread.start();
            
            // Wait for ESCAPE button or connection loss
            while (running.get() && Button.ESCAPE.isUp()) {
                Thread.sleep(BUTTON_POLL_INTERVAL_MS);
            }
            
            // Shutdown gracefully
            shutdown(running, heartbeat, commands, sensorThread);
            
        } catch (Exception e) {
            handleError(e);
        } finally {
            closeSocket(socket);
        }
        
        // Wait for button press before exit
        Button.waitForAnyPress();
    }
    
    /**
     * Performs the handshake protocol with the server.
     * Server sends HELLO, client responds with READY:0.
     * 
     * @param in  Input stream from server
     * @param out Output stream to server
     * @return true if handshake successful, false otherwise
     * @throws IOException if communication error occurs
     */
    private static boolean performHandshake(BufferedReader in, BufferedWriter out) throws IOException {
        LCD.clear(1);
        LCD.drawString("Handshake...", 0, 1);
        
        String response = in.readLine();
        if (response == null || !response.trim().equals("HELLO")) {
            displayError("Bad handshake", response);
            return false;
        }
        
        // Send ready response
        out.write("READY:0\n");
        out.flush();
        return true;
    }
    
    /**
     * Displays status message on LCD.
     * 
     * @param status  Status text for line 0
     * @param message Message for line 1
     */
    private static void displayStatus(String status, String message) {
        LCD.clear();
        LCD.drawString("Status: " + status, 0, 0);
        LCD.drawString(message, 0, 1);
    }
    
    /**
     * Displays error message on LCD.
     * 
     * @param message Primary error message
     * @param details Additional details (can be null)
     */
    private static void displayError(String message, String details) {
        LCD.clear();
        LCD.drawString("Status: ERROR", 0, 0);
        LCD.drawString(message, 0, 1);
        if (details != null && details.length() > 0) {
            LCD.drawString(details.substring(0, Math.min(LCD_MAX_WIDTH, details.length())), 0, 2);
        }
    }
    
    /**
     * Handles exceptions by displaying error on LCD.
     * 
     * @param e Exception to handle
     */
    private static void handleError(Exception e) {
        LCD.clear();
        LCD.drawString("Status: ERROR", 0, 0);
        String msg = e.getMessage();
        if (msg != null && msg.length() > 0) {
            LCD.drawString(msg.substring(0, Math.min(LCD_MAX_WIDTH, msg.length())), 0, 1);
        } else {
            LCD.drawString(e.getClass().getSimpleName(), 0, 1);
        }
    }
    
    /**
     * Performs graceful shutdown of all threads and motors.
     * 
     * @param running       Shared running flag
     * @param heartbeat     Heartbeat thread
     * @param commands      Command handler thread
     * @param sensorThread  Sensor thread
     */
    private static void shutdown(AtomicBoolean running, Thread heartbeat, 
                                 Thread commands, Thread sensorThread) {
        running.set(false);
        
        LCD.clear(0);
        LCD.drawString("Status: Exit", 0, 0);
        
        // Stop all motors for safety
        MotorFactory.stopAll();
        
        // Wait for threads to terminate
        try {
            heartbeat.join(THREAD_JOIN_TIMEOUT_MS);
            commands.join(THREAD_JOIN_TIMEOUT_MS);
            sensorThread.join(THREAD_JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Safely closes the socket connection.
     * 
     * @param socket Socket to close (can be null)
     */
    private static void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                // Ignore - we're shutting down anyway
            }
        }
    }
}
