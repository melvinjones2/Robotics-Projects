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
     */
    float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor);
    
    /**
     * Gets the name/description of this fusion strategy.
     */
    String getName();
}
