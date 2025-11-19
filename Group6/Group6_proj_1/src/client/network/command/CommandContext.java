package client.network.command;

import client.autonomous.BallDetector;
import client.autonomous.BallSearchController;
import client.data.SensorDataWarehouse;
import client.motor.IArmController;
import client.motor.IDriveController;
import client.sensor.ISensor;

import java.io.BufferedWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Context with robot hardware, sensors, and communication
public class CommandContext {
    
    // Hardware
    private final IDriveController drive;
    private final IArmController armController;
    private final List<ISensor> sensors;
    
    // Communication
    private final BufferedWriter out;
    private final AtomicBoolean running;
    
    // Data
    private final SensorDataWarehouse warehouse;
    
    // Autonomous tasks
    private BallDetector ballDetector;
    private BallSearchController ballSearchController;
    private Thread ballScanThread;
    
    public CommandContext(IDriveController drive, IArmController armController, 
                         List<ISensor> sensors, BufferedWriter out, AtomicBoolean running,
                         SensorDataWarehouse warehouse) {
        this.drive = drive;
        this.armController = armController;
        this.sensors = sensors;
        this.out = out;
        this.running = running;
        this.warehouse = warehouse;
    }
    
    // Getters
    public IDriveController getDrive() {
        return drive;
    }
    
    public IArmController getArmController() {
        return armController;
    }
    
    public List<ISensor> getSensors() {
        return sensors;
    }
    
    public BufferedWriter getOut() {
        return out;
    }
    
    public AtomicBoolean getRunning() {
        return running;
    }
    
    public SensorDataWarehouse getWarehouse() {
        return warehouse;
    }
    
    public BallDetector getBallDetector() {
        return ballDetector;
    }
    
    public void setBallDetector(BallDetector ballDetector) {
        this.ballDetector = ballDetector;
    }
    
    public BallSearchController getBallSearchController() {
        return ballSearchController;
    }
    
    public void setBallSearchController(BallSearchController ballSearchController) {
        this.ballSearchController = ballSearchController;
    }
    
    public Thread getBallScanThread() {
        return ballScanThread;
    }
    
    public void setBallScanThread(Thread ballScanThread) {
        this.ballScanThread = ballScanThread;
    }
    
    /**
     * Find a sensor by name (case-insensitive).
     */
    public ISensor findSensor(String name) {
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
    
    public void shutdownAutonomousTasks() {
        if (ballSearchController != null && ballSearchController.isEnabled()) {
            ballSearchController.setEnabled(false);
        }
        if (ballDetector != null) {
            ballDetector.stop();
        }
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
