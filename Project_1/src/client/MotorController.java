package client;

import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.Motor;

/**
 * Provides generic motor movement for debugging.
 */
public class MotorController {
    /**
     * Moves all motors forward at the given speed.
     */
    public static void moveAllForward(int speed) {
        BaseRegulatedMotor[] motors = {Motor.A, Motor.B, Motor.C, Motor.D};
        for (BaseRegulatedMotor motor : motors) {
            motor.setSpeed(speed);
            motor.forward();
        }
    }

    /**
     * Stops all motors.
     */
    public static void stopAll() {
        BaseRegulatedMotor[] motors = {Motor.A, Motor.B, Motor.C, Motor.D};
        for (BaseRegulatedMotor motor : motors) {
            motor.stop();
        }
    }
}
