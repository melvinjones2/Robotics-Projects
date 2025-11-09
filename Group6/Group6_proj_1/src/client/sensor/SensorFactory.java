package client.sensor;

import java.util.ArrayList;
import java.util.List;

import client.sensor.impl.GyroSensor;
import client.sensor.impl.LightSensor;
import client.sensor.impl.TouchSensor;
import client.sensor.impl.UltrasonicSensor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;

// Factory for creating sensors from configuration
public class SensorFactory {
    
    // Create a sensor from config
    public static ISensor createSensor(SensorConfig config) {
        switch (config.getType()) {
            case TOUCH:
                return new TouchSensor(config.getPort());
                
            case ULTRASONIC:
                if (config.getMode() != null) {
                    return new UltrasonicSensor(config.getPort(), config.getMode());
                }
                return new UltrasonicSensor(config.getPort());
                
            case LIGHT:
                if (config.getMode() != null) {
                    return new LightSensor(config.getPort(), config.getMode());
                }
                return new LightSensor(config.getPort());
                
            case GYRO:
                if (config.getMode() != null) {
                    return new GyroSensor(config.getPort(), config.getMode());
                }
                return new GyroSensor(config.getPort());
                
            default:
                return null;
        }
    }
    
    // Create sensors from a list of configs
    public static List<ISensor> createSensors(List<SensorConfig> configs) {
        List<ISensor> sensors = new ArrayList<>();
        for (SensorConfig config : configs) {
            try {
                ISensor sensor = createSensor(config);
                if (sensor != null) {
                    sensors.add(sensor);
                }
            } catch (Exception e) {
                // Skip sensors that fail to initialize
                System.err.println("Failed to create sensor: " + config.getType());
            }
        }
        return sensors;
    }
    
    // Parse port from string (S1, S2, S3, S4)
    public static Port parsePort(String portStr) {
        if (portStr == null || portStr.length() < 2) {
            return null;
        }
        
        char portNum = portStr.toUpperCase().charAt(1);
        switch (portNum) {
            case '1': return SensorPort.S1;
            case '2': return SensorPort.S2;
            case '3': return SensorPort.S3;
            case '4': return SensorPort.S4;
            default: return null;
        }
    }
    
    // Parse sensor type from string
    public static SensorConfig.SensorType parseSensorType(String typeStr) {
        if (typeStr == null) return null;
        
        String type = typeStr.toUpperCase();
        if (type.contains("TOUCH")) return SensorConfig.SensorType.TOUCH;
        if (type.contains("ULTRA") || type.equals("US")) return SensorConfig.SensorType.ULTRASONIC;
        if (type.contains("LIGHT") || type.contains("COLOR")) return SensorConfig.SensorType.LIGHT;
        if (type.contains("GYRO")) return SensorConfig.SensorType.GYRO;
        
        return null;
    }
    
    // Create default sensor configuration
    public static List<SensorConfig> getDefaultSensorConfig() {
        List<SensorConfig> configs = new ArrayList<>();
        configs.add(new SensorConfig(SensorConfig.SensorType.ULTRASONIC, SensorPort.S1, "listen"));
        configs.add(new SensorConfig(SensorConfig.SensorType.LIGHT, SensorPort.S2, "rgb")); // right
        configs.add(new SensorConfig(SensorConfig.SensorType.GYRO, SensorPort.S3, "rate"));
        configs.add(new SensorConfig(SensorConfig.SensorType.LIGHT, SensorPort.S4, "rgb")); // left
        return configs;
    }
}
