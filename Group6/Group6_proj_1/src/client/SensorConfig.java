package client;

import lejos.hardware.port.Port;

// Configuration for a sensor instance
public class SensorConfig {
    public enum SensorType {
        TOUCH, ULTRASONIC, LIGHT, GYRO
    }
    
    private final SensorType type;
    private final Port port;
    private final String mode;
    
    public SensorConfig(SensorType type, Port port, String mode) {
        this.type = type;
        this.port = port;
        this.mode = mode;
    }
    
    public SensorConfig(SensorType type, Port port) {
        this(type, port, null);
    }
    
    public SensorType getType() { return type; }
    public Port getPort() { return port; }
    public String getMode() { return mode; }
}
