package client;

import lejos.hardware.motor.BaseRegulatedMotor;

public class EV3MotorControl implements IMotorControl {
    private final BaseRegulatedMotor motor;

    public EV3MotorControl(BaseRegulatedMotor motor) {
        this.motor = motor;
    }

    public void forward(int speed) {
        motor.setSpeed(speed);
        motor.forward();
    }

    public void backward(int speed) {
        motor.setSpeed(speed);
        motor.backward();
    }

    public void stop() {
        motor.stop();
    }

    public BaseRegulatedMotor getMotor() {
        return motor;
    }
}