package client.motor;

/**
 * Interface for arm motor control.
 * Provides position control for robotic arm mechanisms.
 */
public interface IArmController extends IMotorController {
    
    /**
     * Move arm to predefined UP position.
     */
    void moveUp();
    
    /**
     * Move arm to predefined DOWN position.
     */
    void moveDown();
    
    /**
     * Move arm to specific angle position.
     * @param degrees Target position in degrees
     */
    void moveTo(int degrees);
    
    /**
     * Get current arm position.
     * @return Current position in degrees, or -1 if unavailable
     */
    int getPosition();
    
    /**
     * Set arm movement speed.
     * @param speed Speed in degrees per second
     */
    void setSpeed(int speed);
}
