package client.motor;

import client.config.RobotConfig;
import lejos.hardware.motor.BaseRegulatedMotor;

/**
 * Arm controller implementation for single-motor arm mechanisms.
 * Provides position control with predefined UP/DOWN positions.
 */
public class ArmController implements IArmController {
    
    private final BaseRegulatedMotor armMotor;
    private final int upPosition;
    private final int downPosition;
    private final int defaultSpeed;
    
    /**
     * Create arm controller with default positions from RobotConfig.
     */
    public ArmController(char motorPort) {
        this(motorPort, RobotConfig.ARM_UP_POSITION, RobotConfig.ARM_DOWN_POSITION, RobotConfig.ARM_SPEED);
    }
    
    /**
     * Create arm controller with custom positions.
     */
    public ArmController(char motorPort, int upPosition, int downPosition, int defaultSpeed) {
        this.armMotor = MotorFactory.getMotor(motorPort);
        this.upPosition = upPosition;
        this.downPosition = downPosition;
        this.defaultSpeed = defaultSpeed;
        
        if (this.armMotor != null) {
            this.armMotor.setSpeed(defaultSpeed);
        }
    }
    
    /**
     * Create arm controller with existing motor instance.
     */
    public ArmController(BaseRegulatedMotor armMotor, int upPosition, int downPosition, int defaultSpeed) {
        this.armMotor = armMotor;
        this.upPosition = upPosition;
        this.downPosition = downPosition;
        this.defaultSpeed = defaultSpeed;
        
        if (this.armMotor != null) {
            this.armMotor.setSpeed(defaultSpeed);
        }
    }
    
    @Override
    public boolean isReady() {
        return armMotor != null;
    }
    
    @Override
    public void stop() {
        if (armMotor != null) {
            try {
                armMotor.stop(true);
            } catch (Exception e) {
                // Motor stop failed - safe to ignore
            }
        }
    }
    
    @Override
    public boolean isMoving() {
        try {
            return armMotor != null && armMotor.isMoving();
        } catch (Exception e) {
            // Motor query failed - assume stopped
            return false;
        }
    }
    
    @Override
    public String getName() {
        return "Arm";
    }
    
    @Override
    public void moveUp() {
        moveTo(upPosition);
    }
    
    @Override
    public void moveDown() {
        moveTo(downPosition);
    }
    
    @Override
    public void moveTo(int degrees) {
        if (!isReady()) return;
        
        try {
            armMotor.rotateTo(degrees, false);
        } catch (Exception e) {
            // Movement failed - motor may be blocked
        }
    }
    
    @Override
    public int getPosition() {
        if (!isReady()) return -1;
        
        try {
            return armMotor.getTachoCount();
        } catch (Exception e) {
            return -1;
        }
    }
    
    @Override
    public void setSpeed(int speed) {
        if (!isReady()) return;
        
        try {
            armMotor.setSpeed(speed);
        } catch (Exception e) {
            // Speed setting failed - motor will use previous speed
        }
    }
    
    /**
     * Get the motor instance (for legacy code compatibility).
     */
    public BaseRegulatedMotor getMotor() {
        return armMotor;
    }
    
    /**
     * Get configured up position.
     */
    public int getUpPosition() {
        return upPosition;
    }
    
    /**
     * Get configured down position.
     */
    public int getDownPosition() {
        return downPosition;
    }
}
