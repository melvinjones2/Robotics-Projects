package client.autonomous.fusion;

import client.sensor.ISensor;

/**
 * Strategy interface for sensor fusion algorithms.
 * 
 * Different strategies can combine multiple sensor readings in different ways
 * to produce more accurate distance measurements.
 * 
 * Strategy Pattern allows swapping fusion algorithms without changing BallDetector.
 */
public interface ISensorFusionStrategy {
    
    /**
     * Fuses sensor readings to produce best distance estimate.
     * 
     * @param ultrasonicSensor ultrasonic distance sensor (may be null)
     * @param infraredSensor infrared distance sensor (may be null)
     * @return fused distance in cm, or -1 if no valid reading
     */
    float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor);
    
    /**
     * Gets the name/description of this fusion strategy.
     * 
     * @return strategy name for logging/debugging
     */
    String getName();
}
