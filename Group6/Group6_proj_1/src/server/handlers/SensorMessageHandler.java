package server.handlers;

import java.io.BufferedWriter;
import server.autonomous.ServerAutonomousController;
import server.logging.LogManager;

public class SensorMessageHandler implements IMessageHandler {
    
    private final ServerAutonomousController autonomousController;
    
    public SensorMessageHandler(ServerAutonomousController autonomousController) {
        this.autonomousController = autonomousController;
    }
    
    @Override
    public void handle(String message, BufferedWriter out) {
        if (autonomousController != null) {
            autonomousController.parseSensorMessage(message);
            LogManager.debug("Received sensor data: " + message);
        }
    }
}
