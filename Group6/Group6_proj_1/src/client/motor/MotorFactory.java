package client.motor;

import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import java.util.HashMap;
import java.util.Map;

// Factory for creating and managing motor instances
public class MotorFactory {
    
    public enum MotorType {
        LARGE, MEDIUM
    }
    
    // Cache motors to avoid recreating them (important: only create once per port)
    private static final Map<Character, BaseRegulatedMotor> motorCache = new HashMap<>();
    
    // Get motor by port letter (A, B, C, D) - creates EV3LargeRegulatedMotor by default
    public static BaseRegulatedMotor getMotor(char port) {
        port = Character.toUpperCase(port);
        
        // Return cached motor if exists
        if (motorCache.containsKey(port)) {
            return motorCache.get(port);
        }
        
        // Create new motor instance (recommended approach for EV3)
        Port motorPort = parseMotorPort(port);
        if (motorPort == null) {
            return null;
        }
        
        // Default to large motor (most common for drive motors)
        BaseRegulatedMotor motor = new EV3LargeRegulatedMotor(motorPort);
        motorCache.put(port, motor);
        
        return motor;
    }
    
    // Create a new motor instance (use sparingly, prefer getMotor)
    public static BaseRegulatedMotor createMotor(char port, MotorType type) {
        Port motorPort = parseMotorPort(port);
        if (motorPort == null) {
            return null;
        }
        
        switch (type) {
            case LARGE:
                return new EV3LargeRegulatedMotor(motorPort);
            case MEDIUM:
                return new EV3MediumRegulatedMotor(motorPort);
            default:
                return null;
        }
    }
    
    // Parse port from character
    public static Port parseMotorPort(char port) {
        switch (Character.toUpperCase(port)) {
            case 'A': return MotorPort.A;
            case 'B': return MotorPort.B;
            case 'C': return MotorPort.C;
            case 'D': return MotorPort.D;
            default: return null;
        }
    }
    
    // Get all available motors
    public static BaseRegulatedMotor[] getAllMotors() {
        return new BaseRegulatedMotor[] {
            getMotor('A'), getMotor('B'), getMotor('C'), getMotor('D')
        };
    }
    
    // Get drive motors (left and right) for movement
    public static BaseRegulatedMotor[] getDriveMotors() {
        // Use RobotConfig.DRIVE_MOTORS for configuration
        char[] drivePorts = client.config.RobotConfig.DRIVE_MOTORS;
        BaseRegulatedMotor[] motors = new BaseRegulatedMotor[drivePorts.length];
        for (int i = 0; i < drivePorts.length; i++) {
            motors[i] = getMotor(drivePorts[i]);
        }
        return motors;
    }
    
    // Stop all motors (non-blocking)
    public static void stopAll() {
        BaseRegulatedMotor[] motors = getAllMotors();
        
        // Issue stop to all motors without waiting
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                try {
                    motor.stop(false); // Non-blocking stop
                } catch (Exception e) {
                    // Handle motor error gracefully
                }
            }
        }
    }
    
    // Stop all motors with immediate brake (non-blocking)
    public static void stopAllImmediate() {
        BaseRegulatedMotor[] motors = getAllMotors();
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                try {
                    motor.stop(false); // Non-blocking stop
                } catch (Exception e) {
                    // Handle motor error gracefully
                }
            }
        }
    }
    
    // Check if motor exists and is available
    public static boolean isMotorAvailable(char port) {
        BaseRegulatedMotor motor = getMotor(port);
        return motor != null;
    }
    
    // Clear motor cache (use when closing application)
    public static void clearCache() {
        motorCache.clear();
    }
}
