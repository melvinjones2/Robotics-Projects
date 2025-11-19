package client.motor;

/**
 * Interface for differential drive motor control.
 * Provides high-level movement operations for two-wheeled drive systems.
 */
public interface IDriveController extends IMotorController {
    
    /**
     * Move forward or backward at specified speed.
     * @param forward true for forward, false for backward
     * @param speed Motor speed (will be clamped to valid range)
     */
    void move(boolean forward, int speed);
    
    /**
     * Turn in place (pivot turn).
     * @param leftTurn true for left turn, false for right
     * @param speed Motor speed for turning
     */
    void turnInPlace(boolean leftTurn, int speed);
    
    /**
     * Rotate robot by specified angle.
     * @param degrees Degrees to rotate (positive=right, negative=left)
     */
    void rotateDegrees(int degrees);
    
    /**
     * Rotate robot by specified angle at specified speed.
     * @param degrees Degrees to rotate (positive=right, negative=left)
     * @param speed Motor speed for rotation
     */
    void rotateDegrees(int degrees, int speed);
    
    /**
     * Move forward by specified distance in centimeters.
     * @param cm Distance in centimeters
     */
    void moveForwardCm(int cm);
    
    /**
     * Move forward by specified distance at specified speed.
     * @param cm Distance in centimeters
     * @param speed Motor speed
     */
    void moveForwardCm(int cm, int speed);
    
    /**
     * Move backward by specified distance.
     * @param cm Distance in centimeters
     * @param speed Motor speed
     */
    void moveBackwardCm(int cm, int speed);
    
    /**
     * Clamp speed to valid motor range.
     * @param speed Desired speed
     * @return Clamped speed within motor limits
     */
    int clampSpeed(int speed);
}
