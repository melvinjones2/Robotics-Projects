package client.autonomous.fusion;

import client.sensor.ISensor;
import client.sensor.SensorReader;

/**
 * Range-adaptive sensor fusion strategy.
 * 
 * Switches between sensors based on range:
 * - ≥50cm: Ultrasonic (long-range detection)
 * - <50cm: Infrared (close-range precision, center-mounted)
 * 
 * IR is center-mounted so aligning to IR = ball is centered.
 * 
 * Used by: BallDetector.quickSweepScan() - alignment verification
 */
public class RangeAdaptiveFusionStrategy implements ISensorFusionStrategy {
    
    private static final float FAR_THRESHOLD_CM = 50.0f;
    private static final float CLOSE_THRESHOLD_CM = 50.0f;
    private static final float MAX_VALID_DISTANCE_CM = 100.0f;
    
    @Override
    public float fuseDistance(ISensor ultrasonicSensor, ISensor infraredSensor) {
        float us = readDistance(ultrasonicSensor);
        float ir = readDistance(infraredSensor);
        
        // Use ultrasonic for far (50+cm), IR for close (<50cm)
        // IR is centered and more accurate at close range
        if (us > 0 && us >= FAR_THRESHOLD_CM) {
            // Far away: use ultrasonic
            return us;
        } else if (ir > 0 && ir < CLOSE_THRESHOLD_CM) {
            // Close range: use IR (centered sensor)
            return ir;
        } else if (ir > 0 && ir < MAX_VALID_DISTANCE_CM) {
            // IR available but might be close to 50cm threshold
            return ir;
        } else if (us > 0 && us < MAX_VALID_DISTANCE_CM) {
            // Fallback to ultrasonic
            return us;
        }
        
        return -1;
    }
    
    @Override
    public String getName() {
        return "RangeAdaptive (US≥50cm, IR<50cm)";
    }
    
    private float readDistance(ISensor sensor) {
        return SensorReader.readDistance(sensor);
    }
}
