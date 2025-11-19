package client.motor;

/**
 * Interface for motor control abstraction.
 * Allows different motor control strategies to be used interchangeably.
 * Supports dependency injection and testing with mock implementations.
 */
public interface IMotorController {
    
    /**
     * Check if motor controller is ready and operational.
     */
    boolean isReady();
    
    /**
     * Stop all motors controlled by this controller.
     */
    void stop();
    
    /**
     * Check if any motors are currently moving.
     */
    boolean isMoving();
    
    /**
     * Get human-readable name for this controller.
     */
    String getName();
}
