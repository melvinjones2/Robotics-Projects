package server;

import java.io.BufferedWriter;

/**
 * Handles SENSOR messages from client containing batch sensor data.
 * Format: SENSOR:ultrasonic=45.2,touch=0.0,light=35.0
 */
public class SensorMessageHandler implements IMessageHandler {
    
    private final ServerAutonomousController autonomousController;
    
    public SensorMessageHandler(ServerAutonomousController autonomousController) {
        this.autonomousController = autonomousController;
    }
    
    @Override
    public void handle(String message, BufferedWriter out) {
        if (autonomousController != null) {
            autonomousController.parseSensorMessage(message);
            
            // Log sensor data if debug enabled
            LogManager.debug("Received sensor data: " + message);
        }
    }
}
