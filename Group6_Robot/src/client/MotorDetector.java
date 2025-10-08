package client;

import lejos.hardware.motor.Motor;
import lejos.hardware.motor.BaseRegulatedMotor;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for detecting connected motors on the EV3 brick.
 */
public class MotorDetector {
    /**
     * Returns a list of motors (A, B, C, D) that are available.
     */
    public static List<BaseRegulatedMotor> detectMotors() {
        List<BaseRegulatedMotor> motors = new ArrayList<BaseRegulatedMotor>();
        motors.add(Motor.A);
        motors.add(Motor.B);
        motors.add(Motor.C);
        motors.add(Motor.D);
        return motors;
    }

    /**
     * Returns a string summary of detected motors.
     */
    public static String getMotorSummary() {
        StringBuilder sb = new StringBuilder();
        String[] ports = {"A", "B", "C", "D"};
        BaseRegulatedMotor[] motors = {Motor.A, Motor.B, Motor.C, Motor.D};
        for (int i = 0; i < motors.length; i++) {
            String status = "NOT DETECTED";
            try {
                int initial = motors[i].getTachoCount();
                motors[i].setSpeed(100);
                motors[i].forward();
                Thread.sleep(200); // Run for 200ms
                motors[i].stop();
                int after = motors[i].getTachoCount();
                if (Math.abs(after - initial) > 0) {
                    status = "OK";
                }
            } catch (InterruptedException e) {
                status = "ERROR";
            }
            sb.append("MOTOR:").append(ports[i]).append("[").append(status).append("] ");
        }
        return sb.toString().trim();
    }
}