package client.autonomous;

import lejos.hardware.motor.Motor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;

// Test program for custom line map navigation
public class TestCustomLineMap {
    
    public static void main(String[] args) {
        LCD.clear();
        LCD.drawString("Custom LineMap", 0, 0);
        LCD.drawString("Press ENTER", 0, 1);
        Button.ENTER.waitForPressAndRelease();
        
        // Setup robot configuration
        double wheelDiameter = 5.6;  // Adjust to your robot's wheel size in cm
        double trackWidth = 11.6;    // Adjust to your robot's track width in cm
        
        // Create pilot (adjust Motor.A and Motor.B to your motor ports)
        DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, trackWidth, Motor.B, Motor.A);
        pilot.setLinearSpeed(6);        // 6 cm/s for linear movement
        pilot.setAngularSpeed(45);      // 45 degrees/s for rotation
        pilot.setLinearAcceleration(500);
        
        // Create custom navigator
        CustomLineMapNavigator customNav = new CustomLineMapNavigator(pilot);
        
        LCD.clear();
        LCD.drawString("Navigate to Goal 2", 0, 0);
        LCD.drawString("Press ENTER", 0, 1);
        LCD.drawString("ESCAPE to exit", 0, 2);
        
        Button.ENTER.waitForPressAndRelease();
        if (Button.ESCAPE.isUp()) {
            // Navigate from Goal 1 (top-left) to Goal 2 (bottom-right)
            // Assuming robot starts at Goal 1 facing north (0 degrees)
            boolean success = customNav.navigateGoal1ToGoal2(0.0f);
            
            LCD.clear();
            if (success) {
                LCD.drawString("Goal 2 reached!", 0, 0);
            } else {
                LCD.drawString("Navigation failed", 0, 0);
            }
            
            LCD.drawString("Press ENTER", 0, 2);
            Button.ENTER.waitForPressAndRelease();
            
            if (Button.ESCAPE.isUp()) {
                // Navigate back from Goal 2 to Goal 1
                LCD.clear();
                LCD.drawString("Return to Goal 1", 0, 0);
                LCD.drawString("Press ENTER", 0, 1);
                
                Button.ENTER.waitForPressAndRelease();
                if (Button.ESCAPE.isUp()) {
                    success = customNav.navigateGoal2ToGoal1(0.0f);
                    
                    LCD.clear();
                    if (success) {
                        LCD.drawString("Goal 1 reached!", 0, 0);
                    } else {
                        LCD.drawString("Navigation failed", 0, 0);
                    }
                }
            }
        }
        
        LCD.clear();
        LCD.drawString("Done!", 0, 0);
        LCD.drawString("Press any button", 0, 1);
        Button.waitForAnyPress();
    }
}
