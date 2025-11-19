package client.motor;

/**
 * Interface for differential drive motor control.
 * Provides high-level movement operations for two-wheeled drive systems.
 */
public interface IDriveController extends IMotorController {
    
    /**
     * Move forward or backward at specified speed.
     */
    void move(boolean forward, int speed);
    
    /**
     * Turn in place (pivot turn).
     */
    void turnInPlace(boolean leftTurn, int speed);
    
    /**
     * Rotate robot by specified angle.
     */
    void rotateDegrees(int degrees);
    
    /**
     * Rotate robot by specified angle at specified speed.
     */
    void rotateDegrees(int degrees, int speed);
    
    /**
     * Move forward by specified distance in centimeters.
     */
    void moveForwardCm(int cm);
    
    /**
     * Move forward by specified distance at specified speed.
     */
    void moveForwardCm(int cm, int speed);
    
    /**
     * Move backward by specified distance.
     */
    void moveBackwardCm(int cm, int speed);
    
    /**
     * Clamp speed to valid motor range.
     */
    int clampSpeed(int speed);
}
