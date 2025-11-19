package client.motor;

import client.config.RobotConfig;
import lejos.hardware.motor.BaseRegulatedMotor;

/**
 * Differential drive implementation for two-wheeled robots.
 * Centralizes conversions between robot angles/distances and motor tacho counts.
 * Implements IDriveController for standardized drive control.
 */
public class DifferentialDrive implements IDriveController {

    private final BaseRegulatedMotor leftMotor;
    private final BaseRegulatedMotor rightMotor;

    public DifferentialDrive() {
        this(RobotConfig.LEFT_MOTOR_PORT, RobotConfig.RIGHT_MOTOR_PORT);
    }

    public DifferentialDrive(char leftPort, char rightPort) {
        this(MotorFactory.getMotor(leftPort), MotorFactory.getMotor(rightPort));
    }

    public DifferentialDrive(BaseRegulatedMotor leftMotor, BaseRegulatedMotor rightMotor) {
        this.leftMotor = leftMotor;
        this.rightMotor = rightMotor;
    }

    public BaseRegulatedMotor getLeftMotor() {
        return leftMotor;
    }

    public BaseRegulatedMotor getRightMotor() {
        return rightMotor;
    }

    public boolean isReady() {
        return leftMotor != null && rightMotor != null;
    }

    public void move(boolean forward, int speed) {
        if (!isReady()) return;
        speed = clampSpeed(speed);
        setSpeed(speed);
        if (forward) {
            leftMotor.forward();
            rightMotor.forward();
        } else {
            leftMotor.backward();
            rightMotor.backward();
        }
    }

    public void turnInPlace(boolean leftTurn, int speed) {
        if (!isReady()) return;
        speed = clampSpeed(speed);
        setSpeed(speed);
        if (leftTurn) {
            leftMotor.backward();
            rightMotor.forward();
        } else {
            leftMotor.forward();
            rightMotor.backward();
        }
    }

    public void rotateDegrees(int degrees) {
        rotateDegrees(degrees, RobotConfig.ROTATION_SPEED);
    }

    public void rotateDegrees(int degrees, int speed) {
        if (!isReady()) return;
        if (Math.abs(degrees) < 1) return;
        speed = clampSpeed(speed);
        setSpeed(speed);
        int motorDegrees = toRotationDegrees(degrees);
        if (degrees > 0) {
            leftMotor.rotate(motorDegrees, true);
            rightMotor.rotate(-motorDegrees, false);
        } else {
            leftMotor.rotate(-motorDegrees, true);
            rightMotor.rotate(motorDegrees, false);
        }
    }

    public void rotateAtSpeed(int degrees, int degreesPerSecond, boolean nonBlocking) {
        if (!isReady()) return;
        degreesPerSecond = Math.abs(degreesPerSecond);
        int speed = clampSpeed(degreesPerSecond * 2);
        setSpeed(speed);
        int motorDegrees = toRotationDegrees(degrees);

        if (degrees > 0) {
            leftMotor.rotate(motorDegrees, true);
            rightMotor.rotate(-motorDegrees, nonBlocking);
        } else {
            leftMotor.rotate(-motorDegrees, true);
            rightMotor.rotate(motorDegrees, nonBlocking);
        }
    }

    public void moveForwardCm(int cm) {
        moveForwardCm(cm, RobotConfig.COMMAND_DEFAULT_SPEED);
    }

    public void moveForwardCm(int cm, int speed) {
        moveDistanceCm(cm, speed, true);
    }

    public void moveBackwardCm(int cm, int speed) {
        moveDistanceCm(cm, speed, false);
    }

    private void moveDistanceCm(int cm, int speed, boolean forward) {
        if (!isReady()) return;
        if (cm <= 0) return;
        speed = clampSpeed(speed);
        setSpeed(speed);
        int motorDegrees = toWheelDegrees(cm);
        int direction = forward ? 1 : -1;
        leftMotor.rotate(direction * motorDegrees, true);
        rightMotor.rotate(direction * motorDegrees, false);
    }

    public void stop() {
        stopMotor(leftMotor);
        stopMotor(rightMotor);
    }

    public boolean isMoving() {
        try {
            return (leftMotor != null && leftMotor.isMoving()) ||
                   (rightMotor != null && rightMotor.isMoving());
        } catch (Exception e) {
            // Motor query failed - assume stopped
            return false;
        }
    }

    private void setSpeed(int speed) {
        try {
            if (leftMotor != null) {
                leftMotor.setSpeed(speed);
            }
            if (rightMotor != null) {
                rightMotor.setSpeed(speed);
            }
        } catch (Exception e) {
            // Speed setting failed - motor will use previous speed
        }
    }

    private static void stopMotor(BaseRegulatedMotor motor) {
        if (motor != null) {
            try {
                motor.stop(true);
            } catch (Exception e) {
                // Motor already stopped or hardware error - safe to ignore
            }
        }
    }

    @Override
    public int clampSpeed(int speed) {
        return Math.max(RobotConfig.MIN_MOTOR_SPEED, Math.min(RobotConfig.MAX_MOTOR_SPEED, speed));
    }
    
    @Override
    public String getName() {
        return "Drive";
    }

    private static int toRotationDegrees(int robotDegrees) {
        float trackCircumference = (float) (Math.PI * RobotConfig.TRACK_WIDTH_MM);
        float arcLength = trackCircumference * Math.abs(robotDegrees) / 360.0f;
        float wheelCircumference = (float) (Math.PI * RobotConfig.WHEEL_DIAMETER_MM);
        return (int) (arcLength / wheelCircumference * 360);
    }

    private static int toWheelDegrees(int cm) {
        float wheelCircumference = (float) (Math.PI * RobotConfig.WHEEL_DIAMETER_MM);
        return (int) ((cm * 10f) / wheelCircumference * 360);
    }
}
