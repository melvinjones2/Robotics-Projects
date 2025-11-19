package client.autonomous.fusion;

import client.sensor.ISensor;
import client.sensor.SensorReader;

/**
 * Average sensor fusion strategy.
 * 
 * Averages readings from both sensors when both are available.
 * Falls back to single sensor if only one available.
 * 
 * Provides smoothed readings but may not be optimal for
 * range-specific scenarios.
 */
public class AverageFusionStrategy implements ISensorFusionStrategy {
    
    @Override
    public float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor) {
        float us = readDistance(ultrasonicSensor);
        float ir = readDistance(infraredSensor);
        
        // Average both sensors if both available
        if (us > 0 && ir > 0) {
            return (us + ir) / 2.0f;
        } else if (us > 0) {
            return us;
        } else if (ir > 0) {
            return ir;
        }
        
        return -1;
    }
    
    @Override
    public String getName() {
        return "Average (both sensors)";
    }
    
    private float readDistance(ISensor sensor) {
        return SensorReader.readDistance(sensor);
    }
}
