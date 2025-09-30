package client;

import java.util.ArrayList;
import java.util.List;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.hardware.port.Port;

public class MotorDetector {

    public static List<BaseRegulatedMotor> detectMotors() {
        List<BaseRegulatedMotor> motors = new ArrayList<BaseRegulatedMotor>();
        for (String portName : new String[]{"A", "B", "C", "D"}) {
            BaseRegulatedMotor motor = getMotorForPort(portName);
            if (motor != null) {
                motors.add(motor);
            }
        }
        return motors;
    }

    public static String getMotorSummary() {
        StringBuilder sb = new StringBuilder();
        for (String portName : new String[]{"A", "B", "C", "D"}) {
            String type = detectMotorType(portName);
            sb.append("MOTOR:").append(portName).append("[").append(type).append("] ");
        }
        return sb.toString().trim();
    }

    public static String detectMotorType(String portName) {
        try {
            Port port = LocalEV3.get().getPort(portName);
            try {
                EV3LargeRegulatedMotor largeMotor = new EV3LargeRegulatedMotor(port);
                largeMotor.setSpeed(100);
                largeMotor.forward();
                Thread.sleep(100L);
                largeMotor.stop();
                largeMotor.close();
                return "EV3_LARGE";
            } catch (Exception e1) {
                try {
                    EV3MediumRegulatedMotor mediumMotor = new EV3MediumRegulatedMotor(port);
                    mediumMotor.setSpeed(100);
                    mediumMotor.forward();
                    Thread.sleep(100L);
                    mediumMotor.stop();
                    mediumMotor.close();
                    return "EV3_MEDIUM";
                } catch (Exception e2) {
                    try {
                        NXTRegulatedMotor nxtRegMotor = new NXTRegulatedMotor(port);
                        nxtRegMotor.setSpeed(100);
                        nxtRegMotor.forward();
                        Thread.sleep(100L);
                        nxtRegMotor.stop();
                        nxtRegMotor.close();
                        return "NXT";
                    } catch (Exception e3) {
                        return "NOT DETECTED";
                    }
                }
            }
        } catch (Exception e) {
            return "ERROR";
        }
    }

    public static BaseRegulatedMotor getMotorForPort(String portName) {
        String type = detectMotorType(portName);
        try {
            Port port = LocalEV3.get().getPort(portName);
            if ("EV3_LARGE".equals(type)) {
                return new EV3LargeRegulatedMotor(port);
            }
            if ("EV3_MEDIUM".equals(type)) {
                return new EV3MediumRegulatedMotor(port);
            }
            if ("NXT".equals(type)) {
                return new NXTRegulatedMotor(port);
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }
}
