package client.motor;

import client.config.RobotConfig;
import lejos.hardware.motor.BaseRegulatedMotor;

// Enhanced motor controller with synchronized multi-motor movements
public class MovementExecutor {
    
    /**
     * Execute a movement command based on the provided parameters.
     */
    public static void execute(MovementParameters params) {
        try {
            if (params.getDirection() == MovementParameters.Direction.STOP) {
                executeStop(params);
                return;
            }
            
            if (params.isAllMotors()) {
                executeAllMotors(params);
            } else {
                executeSingleMotor(params);
            }
        } catch (Exception e) {
            // Handle motor errors gracefully (e.g., unplugged motor)
            lejos.hardware.lcd.LCD.clear(3);
            lejos.hardware.lcd.LCD.drawString("Motor error!", 0, 3);
            lejos.hardware.Sound.buzz();
            // Don't crash - just log and continue
        }
    }
    
    /**
     * Execute movement on all motors synchronously.
     */
    private static void executeAllMotors(MovementParameters params) {
        BaseRegulatedMotor[] motors = MotorFactory.getAllMotors();
        
        // Configure all motors first (with error handling for each)
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                try {
                    configureMotor(motor, params);
                } catch (Exception e) {
                    // Skip this motor if it's not responding (unplugged)
                    continue;
                }
            }
        }
        
        // Start all motors simultaneously (non-blocking, with error handling)
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                try {
                    startMotorDirection(motor, params, true); // true = non-blocking
                } catch (Exception e) {
                    // Skip this motor if it's not responding
                    continue;
                }
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
        
        try {
            configureMotor(motor, params);
            startMotorDirection(motor, params, params.isImmediateReturn());
        } catch (Exception e) {
            // Handle motor error (unplugged, etc.)
            lejos.hardware.lcd.LCD.clear(3);
            lejos.hardware.lcd.LCD.drawString("Motor " + params.getPort() + " err", 0, 3);
            lejos.hardware.Sound.buzz();
        }
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
                // For left turn, only move motors in RIGHT_MOTORS forward
                // and LEFT_MOTORS backward (differential drive)
                boolean isInLeft = false;
                boolean isInRight = false;
                
                for (char port : RobotConfig.LEFT_MOTORS) {
                    if (motor == MotorFactory.getMotor(port)) {
                        isInLeft = true;
                        break;
                    }
                }
                
                for (char port : RobotConfig.RIGHT_MOTORS) {
                    if (motor == MotorFactory.getMotor(port)) {
                        isInRight = true;
                        break;
                    }
                }
                
                if (isInRight) {
                    motor.forward(); // Right motors forward to turn left
                } else if (isInLeft) {
                    motor.backward(); // Left motors backward to turn left
                }
                break;
                
            case RIGHT:
                // For right turn, only move motors in LEFT_MOTORS forward
                // and RIGHT_MOTORS backward (differential drive)
                isInLeft = false;
                isInRight = false;
                
                for (char port : RobotConfig.LEFT_MOTORS) {
                    if (motor == MotorFactory.getMotor(port)) {
                        isInLeft = true;
                        break;
                    }
                }
                
                for (char port : RobotConfig.RIGHT_MOTORS) {
                    if (motor == MotorFactory.getMotor(port)) {
                        isInRight = true;
                        break;
                    }
                }
                
                if (isInLeft) {
                    motor.forward(); // Left motors forward to turn right
                } else if (isInRight) {
                    motor.backward(); // Right motors backward to turn right
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
                try {
                    motor.stop(false); // Non-blocking stop
                } catch (Exception e) {
                    // Motor might be unplugged - ignore error
                }
            }
        }
    }
    
    /**
     * Stop all motors simultaneously.
     */
    public static void stopAllSynchronized() {
        BaseRegulatedMotor[] motors = MotorFactory.getAllMotors();
        
        // Issue stop command to all motors at once (completely non-blocking)
        for (BaseRegulatedMotor motor : motors) {
            if (motor != null) {
                try {
                    motor.stop(false); // Non-blocking stop - returns immediately
                } catch (Exception e) {
                    // Motor might be unplugged - continue stopping others
                    continue;
                }
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
