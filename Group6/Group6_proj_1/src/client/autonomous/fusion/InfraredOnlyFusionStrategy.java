package client.autonomous.fusion;

import client.sensor.ISensor;
import client.sensor.SensorReader;

/**
 * Infrared-only sensor fusion strategy.
 * 
 * Uses only infrared sensor for distance measurement.
 * Falls back to ultrasonic only if infrared unavailable.
 * 
 * Useful when ultrasonic sensor is unreliable or unavailable.
 */
public class InfraredOnlyFusionStrategy implements ISensorFusionStrategy {
    
    @Override
    public float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor) {
        float ir = readDistance(infraredSensor);
        
        if (ir > 0) {
            return ir;
        }
        
        // Fallback to US if infrared unavailable
        return readDistance(ultrasonicSensor);
    }
    
    @Override
    public String getName() {
        return "InfraredOnly";
    }
    
    private float readDistance(ISensor sensor) {
        return SensorReader.readDistance(sensor);
    }
}
