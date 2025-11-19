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
     * @param motorPort Motor port character ('A', 'B', 'C', 'D')
     */
    public ArmController(char motorPort) {
        this(motorPort, RobotConfig.ARM_UP_POSITION, RobotConfig.ARM_DOWN_POSITION, RobotConfig.ARM_SPEED);
    }
    
    /**
     * Create arm controller with custom positions.
     * @param motorPort Motor port character
     * @param upPosition Up position in degrees
     * @param downPosition Down position in degrees
     * @param defaultSpeed Default movement speed
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
     * @param armMotor The motor instance
     * @param upPosition Up position in degrees
     * @param downPosition Down position in degrees
     * @param defaultSpeed Default movement speed
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
     * @return The underlying motor or null
     */
    public BaseRegulatedMotor getMotor() {
        return armMotor;
    }
    
    /**
     * Get configured up position.
     * @return Up position in degrees
     */
    public int getUpPosition() {
        return upPosition;
    }
    
    /**
     * Get configured down position.
     * @return Down position in degrees
     */
    public int getDownPosition() {
        return downPosition;
    }
}
