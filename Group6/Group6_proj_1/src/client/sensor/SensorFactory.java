package client.sensor;

import java.util.ArrayList;
import java.util.List;

import client.sensor.impl.GyroSensor;
import client.sensor.impl.InfraredSensor;
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
                
            case INFRARED:
                if (config.getMode() != null) {
                    return new InfraredSensor(config.getPort(), config.getMode());
                }
                return new InfraredSensor(config.getPort());
                
            default:
                return null;
        }
    }
    
    // Create sensors from a list of configs
    public static List<ISensor> createSensors(List<SensorConfig> configs) {
        List<ISensor> sensors = new ArrayList<ISensor>();
        for (SensorConfig config : configs) {
            try {
                ISensor sensor = createSensor(config);
                if (sensor != null && sensor.isAvailable()) {
                    sensors.add(sensor);
                    // Don't print to console - it shows on LCD
                }
            } catch (Exception e) {
                // Skip sensors that fail to initialize - no output
            }
        }
        return sensors;
    }
    
    public static ISensor createSensorWithRetry(SensorConfig config, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                ISensor sensor = createSensor(config);
                if (sensor != null && sensor.isAvailable()) {
                    return sensor;
                }
                // Wait briefly before retry
                Thread.sleep(100);
            } catch (Exception e) {
                // Sensor creation failed - will retry or return null
                // No output on EV3 to avoid LCD clutter
            }
        }
        return null;
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
        if (type.contains("INFRARED") || type.contains("IR")) return SensorConfig.SensorType.INFRARED;
        
        return null;
    }
    
    // Create default sensor configuration
    public static List<SensorConfig> getDefaultSensorConfig() {
        List<SensorConfig> configs = new ArrayList<SensorConfig>();
        
        configs.add(new SensorConfig(SensorConfig.SensorType.ULTRASONIC, SensorPort.S2, "distance"));
        configs.add(new SensorConfig(SensorConfig.SensorType.LIGHT, SensorPort.S4, "rgb"));
        configs.add(new SensorConfig(SensorConfig.SensorType.GYRO, SensorPort.S3, "rate"));
        configs.add(new SensorConfig(SensorConfig.SensorType.INFRARED, SensorPort.S1, "distance"));
        
        return configs;
    }
}
