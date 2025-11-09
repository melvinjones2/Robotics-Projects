package client;

import lejos.hardware.motor.BaseRegulatedMotor;

/**
 * Enhanced motor controller with support for advanced movement operations.
 * Provides smooth acceleration, precise distance control, and synchronized multi-motor movements.
 */
public class MovementExecutor {
    
    private static final int DEFAULT_ACCELERATION = 200;
    
    /**
     * Execute a movement command based on the provided parameters.
     */
    public static void execute(MovementParameters params) {
        if (params.getDirection() == MovementParameters.Direction.STOP) {
            executeStop(params);
            return;
        }
        
        if (params.isAllMotors()) {
            executeAllMotors(params);
        } else {
            executeSingleMotor(params);
        }
    }
    
    /**
     * Execute movement on all motors synchronously.
     */
    private static void executeAllMotors(MovementParameters params) {
        BaseRegulatedMotor[] motors = MotorFactory.getAllMotors();
        
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                configureMotor(motor, params);
            }
        }
        
        // Execute synchronized movement
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                executeMotorDirection(motor, params);
            }
        }
    }
    
    /**
     * Execute movement on a single motor.
     */
    private static void executeSingleMotor(MovementParameters params) {
        BaseRegulatedMotor motor = MotorFactory.getMotor(params.getPort());
        if (motor == null) {
            return;
        }
        
        configureMotor(motor, params);
        executeMotorDirection(motor, params);
    }
    
    /**
     * Configure motor settings (speed, acceleration).
     */
    private static void configureMotor(BaseRegulatedMotor motor, MovementParameters params) {
        motor.setSpeed(params.getSpeed());
        motor.setAcceleration(DEFAULT_ACCELERATION);
    }
    
    /**
     * Execute the directional movement on a motor.
     */
    private static void executeMotorDirection(BaseRegulatedMotor motor, MovementParameters params) {
        int distance = params.getDistance();
        boolean blocking = !params.isImmediateReturn();
        
        switch (params.getDirection()) {
            case FORWARD:
                if (distance > 0) {
                    motor.rotate(distance, !blocking);
                } else {
                    motor.forward();
                }
                break;
                
            case BACKWARD:
                if (distance > 0) {
                    motor.rotate(-distance, !blocking);
                } else {
                    motor.backward();
                }
                break;
                
            case LEFT:
                // For differential drive: left = right motor forward, left motor backward
                if (motor == MotorFactory.getMotor('B') || motor == MotorFactory.getMotor('C')) {
                    motor.forward();
                }
                break;
                
            case RIGHT:
                // For differential drive: right = left motor forward, right motor backward
                if (motor == MotorFactory.getMotor('A') || motor == MotorFactory.getMotor('D')) {
                    motor.forward();
                }
                break;
                
            case STOP:
                motor.stop();
                break;
        }
    }
    
    /**
     * Stop motors based on parameters.
     */
    private static void executeStop(MovementParameters params) {
        if (params.isAllMotors()) {
            MotorFactory.stopAll();
        } else {
            MotorController.stop(params.getPort());
        }
    }
    
    /**
     * Emergency stop - immediate stop all motors.
     */
    public static void emergencyStop() {
        BaseRegulatedMotor[] motors = MotorFactory.getAllMotors();
        
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                motor.stop(true); // Immediate stop
            }
        }
    }
}
