package client;

import lejos.hardware.motor.BaseRegulatedMotor;

// Enhanced motor controller with synchronized multi-motor movements
public class MovementExecutor {
    
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
        
        // Configure all motors first
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                configureMotor(motor, params);
            }
        }
        
        // Start all motors simultaneously (non-blocking)
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                startMotorDirection(motor, params, true); // true = non-blocking
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
        startMotorDirection(motor, params, params.isImmediateReturn());
    }
    
    /**
     * Configure motor settings (speed, acceleration).
     */
    private static void configureMotor(BaseRegulatedMotor motor, MovementParameters params) {
        motor.setSpeed(params.getSpeed());
        motor.setAcceleration(RobotConfig.DEFAULT_MOTOR_ACCELERATION);
    }
    
    /**
     * Start the directional movement on a motor.
     * @param immediateReturn true for non-blocking movement
     */
    private static void startMotorDirection(BaseRegulatedMotor motor, MovementParameters params, boolean immediateReturn) {
        int distance = params.getDistance();
        
        switch (params.getDirection()) {
            case FORWARD:
                if (distance > 0) {
                    motor.rotate(distance, immediateReturn);
                } else {
                    motor.forward();
                }
                break;
                
            case BACKWARD:
                if (distance > 0) {
                    motor.rotate(-distance, immediateReturn);
                } else {
                    motor.backward();
                }
                break;
                
            case LEFT:
                // Left motors from config
                for (char port : RobotConfig.LEFT_MOTORS) {
                    if (motor == MotorFactory.getMotor(port)) {
                        motor.forward();
                        break;
                    }
                }
                break;
                
            case RIGHT:
                // Right motors from config
                for (char port : RobotConfig.RIGHT_MOTORS) {
                    if (motor == MotorFactory.getMotor(port)) {
                        motor.forward();
                        break;
                    }
                }
                break;
                
            case STOP:
                motor.stop(true); // Immediate stop
                break;
        }
    }
    
    /**
     * Stop motors based on parameters - synchronized stop.
     */
    private static void executeStop(MovementParameters params) {
        if (params.isAllMotors()) {
            stopAllSynchronized();
        } else {
            BaseRegulatedMotor motor = MotorFactory.getMotor(params.getPort());
            if (motor != null) {
                motor.stop(true);
            }
        }
    }
    
    /**
     * Stop all motors simultaneously.
     */
    public static void stopAllSynchronized() {
        BaseRegulatedMotor[] motors = MotorFactory.getAllMotors();
        
        // Issue stop command to all motors at once (non-blocking)
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                motor.stop(true); // Immediate stop
            }
        }
    }
    
    /**
     * Emergency stop - immediate stop all motors.
     */
    public static void emergencyStop() {
        stopAllSynchronized();
    }
}
