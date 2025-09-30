package client;

import java.util.HashMap;
import java.util.Map;
import lejos.hardware.motor.BaseRegulatedMotor;

public class MotorController {

    private static final Map<Character, IMotorControl> motorMap = new HashMap<Character, IMotorControl>();
    private static final int MIN_SPEED = 0;
    private static final int MAX_SPEED = 900; // Adjust as needed for your motors

    static {
        for (String portName : new String[]{"A", "B", "C", "D"}) {
            char port = portName.charAt(0);
            String type = MotorDetector.detectMotorType(portName);
            BaseRegulatedMotor motor = MotorDetector.getMotorForPort(portName);
            if (motor != null) {
                motorMap.put(port, new EV3MotorControl(motor));
            } else {
                System.out.println("No supported motor detected on port " + portName);
            }
        }
    }

    public static IMotorControl getMotor(char port) {
        return motorMap.get(Character.toUpperCase(port));
    }

    private static int clampSpeed(int speed) {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }

    public static void moveForward(char port, int speed) {
        IMotorControl motor = getMotor(port);
        if (motor != null) {
            motor.forward(clampSpeed(speed));
        } else {
            System.out.println("moveForward: No motor on port " + port);
        }
    }

    public static void moveBackward(char port, int speed) {
        IMotorControl motor = getMotor(port);
        if (motor != null) {
            motor.backward(clampSpeed(speed));
        } else {
            System.out.println("moveBackward: No motor on port " + port);
        }
    }

    public static void stop(char port) {
        IMotorControl motor = getMotor(port);
        if (motor != null) {
            motor.stop();
        } else {
            System.out.println("stop: No motor on port " + port);
        }
    }

    public static void moveAllForward(int speed) {
        for (char port : new char[]{'A', 'B', 'C', 'D'}) {
            moveForward(port, speed);
        }
    }

    public static void moveAllBackward(int speed) {
        for (char port : new char[]{'A', 'B', 'C', 'D'}) {
            moveBackward(port, speed);
        }
    }

    public static void stopAll() {
        for (char port : new char[]{'A', 'B', 'C', 'D'}) {
            stop(port);
        }
    }

    // Shutdown method to stop and close motors
    public static void shutdown() {
        for (IMotorControl motor : motorMap.values()) {
            motor.stop();
            // Close the underlying BaseRegulatedMotor if possible
            if (motor instanceof EV3MotorControl) {
                BaseRegulatedMotor regulatedMotor = ((EV3MotorControl) motor).getMotor();
                if (regulatedMotor != null) {
                    regulatedMotor.close();
                }
            }
        }
        motorMap.clear();
        System.out.println("All motors stopped and resources released.");
    }
}
