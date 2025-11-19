package client.autonomous.fusion;

import client.sensor.ISensor;
import client.sensor.SensorReader;

/**
 * Ultrasonic-only sensor fusion strategy.
 * 
 * Uses only ultrasonic sensor for distance measurement.
 * Falls back to infrared only if ultrasonic unavailable.
 * 
 * Useful when infrared sensor is unreliable or unavailable.
 */
public class UltrasonicOnlyFusionStrategy implements ISensorFusionStrategy {
    
    @Override
    public float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor) {
        float us = readDistance(ultrasonicSensor);
        
        if (us > 0) {
            return us;
        }
        
        // Fallback to IR if ultrasonic unavailable
        return readDistance(infraredSensor);
    }
    
    @Override
    public String getName() {
        return "UltrasonicOnly";
    }
    
    private float readDistance(ISensor sensor) {
        return SensorReader.readDistance(sensor);
    }
}
