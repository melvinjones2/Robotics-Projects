package client;

import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.Motor;
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
    
    // Cache motors to avoid recreating them
    private static final Map<Character, BaseRegulatedMotor> motorCache = new HashMap<>();
    
    // Get motor by port letter (A, B, C, D)
    public static BaseRegulatedMotor getMotor(char port) {
        port = Character.toUpperCase(port);
        
        // Return cached motor if exists
        if (motorCache.containsKey(port)) {
            return motorCache.get(port);
        }
        
        // Use the static Motor instances (recommended for EV3)
        BaseRegulatedMotor motor = null;
        switch (port) {
            case 'A': motor = Motor.A; break;
            case 'B': motor = Motor.B; break;
            case 'C': motor = Motor.C; break;
            case 'D': motor = Motor.D; break;
        }
        
        if (motor != null) {
            motorCache.put(port, motor);
        }
        
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
            Motor.A, Motor.B, Motor.C, Motor.D
        };
    }
    
    // Stop all motors synchronously (non-blocking, then wait for all)
    public static void stopAll() {
        BaseRegulatedMotor[] motors = getAllMotors();
        
        // Issue stop to all motors without waiting
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                motor.stop(true); // Immediate stop, non-blocking
            }
        }
    }
    
    // Stop all motors with immediate brake
    public static void stopAllImmediate() {
        BaseRegulatedMotor[] motors = getAllMotors();
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                motor.stop(true);
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
