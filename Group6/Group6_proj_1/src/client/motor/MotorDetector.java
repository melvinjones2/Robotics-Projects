package client.motor;

import lejos.hardware.motor.BaseRegulatedMotor;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for detecting connected motors on the EV3 brick.
 * CRITICAL: Uses MotorFactory to avoid creating duplicate motor instances.
 */
public class MotorDetector {
    /**
     * Returns a list of motors (A, B, C, D) that are available.
     * Uses MotorFactory to ensure consistent motor instances.
     */
    public static List<BaseRegulatedMotor> detectMotors() {
        List<BaseRegulatedMotor> motors = new ArrayList<BaseRegulatedMotor>();
        // Use MotorFactory to get the same instances used everywhere else
        motors.add(MotorFactory.getMotor('A'));
        motors.add(MotorFactory.getMotor('B'));
        motors.add(MotorFactory.getMotor('C'));
        motors.add(MotorFactory.getMotor('D'));
        return motors;
    }

    /**
     * Returns a string summary of detected motors.
     * IMPORTANT: Uses MotorFactory instances to avoid port conflicts.
     */
    public static String getMotorSummary() {
        StringBuilder sb = new StringBuilder();
        char[] ports = {'A', 'B', 'C', 'D'};
        
        for (char port : ports) {
            String status = "NOT DETECTED";
            BaseRegulatedMotor motor = MotorFactory.getMotor(port);
            
            if (motor == null) {
                sb.append("MOTOR:").append(port).append("[NULL] ");
                continue;
            }
            
            try {
                // Quick test: check if we can read tacho count
                int tacho = motor.getTachoCount();
                status = "OK"; // If we can read it, motor exists
            } catch (Exception e) {
                status = "ERROR";
            }
            
            sb.append("MOTOR:").append(port).append("[").append(status).append("] ");
        }
        return sb.toString().trim();
    }
}