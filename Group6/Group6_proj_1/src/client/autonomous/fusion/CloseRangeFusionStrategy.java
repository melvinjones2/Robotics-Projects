package client.autonomous.fusion;

import client.sensor.ISensor;
import client.sensor.SensorReader;

/**
 * Close-range sensor fusion strategy.
 * 
 * Prioritizes infrared sensor for close-range (<20cm) due to its precision.
 * Falls back to ultrasonic if IR unavailable.
 * 
 * Used by: BallDetector.readBestDistance() - final approach phase
 */
public class CloseRangeFusionStrategy implements ISensorFusionStrategy {
    
    private static final float IR_THRESHOLD_CM = 20.0f;
    
    @Override
    public float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor) {
        float us = readDistance(ultrasonicSensor);
        float ir = readDistance(infraredSensor);
        
        // Prefer infrared at close range (<20cm) for precision
        if (ir > 0 && ir < IR_THRESHOLD_CM) {
            return ir;
        } else if (us > 0) {
            return us;
        } else if (ir > 0) {
            return ir;
        }
        
        return -1;
    }
    
    @Override
    public String getName() {
        return "CloseRange (IR<20cm preferred)";
    }
    
    private float readDistance(ISensor sensor) {
        return SensorReader.readDistance(sensor);
    }
}
