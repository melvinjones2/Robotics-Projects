package client;

import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.Motor;

/**
 * Provides generic motor movement for debugging.
 */
/**
 * Usage in CommandHandler:
 *
 * MOVE <speed>            // Move all motors forward at <speed>
 * MOVE <port> <speed>     // Move one motor forward (A/B/C/D) at <speed>
 * BWD <port> <speed>      // Move one motor backward (A/B/C/D) at <speed>
 * STOP                    // Stop all motors
 * STOP <port>             // Stop one motor (A/B/C/D)
 */
public class MotorController {
    public static BaseRegulatedMotor getMotor(char port) {
        switch (Character.toUpperCase(port)) {
            case 'A': return Motor.A;
            case 'B': return Motor.B;
            case 'C': return Motor.C;
            case 'D': return Motor.D;
            default: return null;
        }
    }

    /**
     * Move a specific motor forward at the given speed.
     */
    public static void moveForward(char port, int speed) {
        BaseRegulatedMotor motor = getMotor(port);
        if (motor != null) {
            motor.setSpeed(speed);
            motor.forward();
        }
    }

    /**
     * Move a specific motor backward at the given speed.
     */
    public static void moveBackward(char port, int speed) {
        BaseRegulatedMotor motor = getMotor(port);
        if (motor != null) {
            motor.setSpeed(speed);
            motor.backward();
        }
    }

    /**
     * Stop a specific motor.
     */
    public static void stop(char port) {
        BaseRegulatedMotor motor = getMotor(port);
        if (motor != null) {
            motor.stop();
        }
    }

    /**
     * Moves all motors forward at the given speed.
     */
    public static void moveAllForward(int speed) {
        for (char port : new char[]{'A', 'B', 'C', 'D'}) {
            moveForward(port, speed);
        }
    }

    /**
     * Moves all motors backward at the given speed.
     */
    public static void moveAllBackward(int speed) {
        for (char port : new char[]{'A', 'B', 'C', 'D'}) {
            moveBackward(port, speed);
        }
    }

    /**
     * Stops all motors.
     */
    public static void stopAll() {
        for (char port : new char[]{'A', 'B', 'C', 'D'}) {
            stop(port);
        }
    }

    public static void getMotorLocation(char port) {
        BaseRegulatedMotor motor = getMotor(port);
        if (motor != null) {
            motor.getTachoCount();
        }
    }

    public static void rotateArm(char port, int angle) {
        BaseRegulatedMotor motor = getMotor(port);
        if (motor != null) {
            motor.rotate(angle);
        }
    }

    public static void moveArmUp() {
        moveForward('A', 200);
    }

    public static void moveArmDown() {
        moveBackward('A', 200);
    }


}
