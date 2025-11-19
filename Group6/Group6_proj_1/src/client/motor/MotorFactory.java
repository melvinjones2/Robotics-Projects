package client.motor;

import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import java.util.HashMap;
import java.util.Map;

public class MotorFactory {
    
    public enum MotorType {
        LARGE, MEDIUM
    }
    
    private static final Map<Character, BaseRegulatedMotor> motorCache = new HashMap<Character, BaseRegulatedMotor>();
    
    public static BaseRegulatedMotor getMotor(char port) {
        port = Character.toUpperCase(port);
        
        if (motorCache.containsKey(port)) {
            return motorCache.get(port);
        }
        
        Port motorPort = parseMotorPort(port);
        if (motorPort == null) {
            return null;
        }
        
        BaseRegulatedMotor motor = new EV3LargeRegulatedMotor(motorPort);
        motorCache.put(port, motor);
        
        return motor;
    }
    
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
    
    public static Port parseMotorPort(char port) {
        switch (Character.toUpperCase(port)) {
            case 'A': return MotorPort.A;
            case 'B': return MotorPort.B;
            case 'C': return MotorPort.C;
            case 'D': return MotorPort.D;
            default: return null;
        }
    }
    
    public static BaseRegulatedMotor[] getAllMotors() {
        return new BaseRegulatedMotor[] {
            getMotor('A'), getMotor('B'), getMotor('C'), getMotor('D')
        };
    }
    
    public static BaseRegulatedMotor[] getDriveMotors() {
        char[] drivePorts = client.config.RobotConfig.DRIVE_MOTORS;
        BaseRegulatedMotor[] motors = new BaseRegulatedMotor[drivePorts.length];
        for (int i = 0; i < drivePorts.length; i++) {
            motors[i] = getMotor(drivePorts[i]);
        }
        return motors;
    }
    
    public static void stopAll() {
        BaseRegulatedMotor[] motors = getAllMotors();
        
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                try {
                    motor.stop(false);
                } catch (Exception e) {
                    // Motor already stopped or hardware error - safe to continue with other motors
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
