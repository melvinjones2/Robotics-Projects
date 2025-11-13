package client.motor;

import lejos.hardware.motor.BaseRegulatedMotor;
import java.util.ArrayList;
import java.util.List;

// Detects connected motors on EV3 brick
public class MotorDetector {
    
    public static List<BaseRegulatedMotor> detectMotors() {
        List<BaseRegulatedMotor> motors = new ArrayList<BaseRegulatedMotor>();
        motors.add(MotorFactory.getMotor('A'));
        motors.add(MotorFactory.getMotor('B'));
        motors.add(MotorFactory.getMotor('C'));
        motors.add(MotorFactory.getMotor('D'));
        return motors;
    }

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
                int tacho = motor.getTachoCount();
                status = "OK";
            } catch (Exception e) {
                status = "ERROR";
            }
            
            sb.append("MOTOR:").append(port).append("[").append(status).append("] ");
        }
        return sb.toString().trim();
    }
}