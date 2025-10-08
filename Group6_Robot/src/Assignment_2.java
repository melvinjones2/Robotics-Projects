import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.Motor;


public class Assignment_2 {

	public static void main(String[] args) {
		BaseRegulatedMotor motorA = Motor.A;
		BaseRegulatedMotor motorD = Motor.D;

		// Move forward 10 cm at speed 200
//		moveForward(10.0, 200);
		
//		moveSingleWheel('A', 1080, 600);
//		
//		moveSingleWheel('D', 1080, 600);

		// Turn right 90 degrees at speed 150
//		turnInPlace(1080, 300);

		// Move backward 5 cm at speed 200
//		moveBackward(500.0, 200);

		// Turn left 45 degrees at speed 150
//		turnInPlace(-1080, 300);

		moveArc(15, 90, 100);
		moveArc(15, 90, 100);
		moveArc(15, 90, 100);
		moveArc(15, 90, 100);
		
//		moveArc(15, 90, 100);
//		moveArc(15, 90, 100);
//		moveArc(15, 90, 100);
//		moveArc(15, 90, 100);
//		
//		moveArc(15, 90, 100);
//		moveArc(15, 90, 100);
//		moveArc(15, 90, 100);
//		moveArc(15, 90, 100);
		
		moveArc(15, 1080, 300);
		
		motorA.close();
		motorD.close();
	}
	
	public static void sleepMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

	public static void moveArc(double radius_cm, double angle_deg, double speed_cm_s) {
        double wheelBase_cm = 17.0; 
        double wheelDiameter_cm = 4.5;

        double innerRadius = radius_cm - (wheelBase_cm / 2.0);
        double outerRadius = radius_cm + (wheelBase_cm / 2.0);

        double innerArc = 2 * Math.PI * innerRadius * (angle_deg / 360.0);
        double outerArc = 2 * Math.PI * outerRadius * (angle_deg / 360.0);

        double wheelCircumference = Math.PI * wheelDiameter_cm;
        int innerDegrees = (int)((innerArc / wheelCircumference) * 360.0);
        int outerDegrees = (int)((outerArc / wheelCircumference) * 360.0);

        double outerSpeed = speed_cm_s * (outerArc / ((outerArc + innerArc) / 2.0));
        double innerSpeed = speed_cm_s * (innerArc / ((outerArc + innerArc) / 2.0));

        Motor.A.setSpeed((int)innerSpeed);
        Motor.D.setSpeed((int)outerSpeed);

        Motor.A.rotate(innerDegrees, true);
        Motor.D.rotate(outerDegrees);     
        
		sleepMs(10000);
    }

	public static void moveSingleWheel(char port, double distance, int speed) {
        double robotMoveRatio = 0.5;

        int motorDegrees = (int)(distance / robotMoveRatio);
        BaseRegulatedMotor motor = (port == 'A') ? Motor.A : Motor.D;
        motor.setSpeed(speed);
        motor.rotate(motorDegrees);
    }

	public static void moveForward(double distance, int speed) {
		double robotMoveRatio = 0.5;

		int motorDegrees = (int)(distance / robotMoveRatio);
		Motor.A.setSpeed(speed);
		Motor.D.setSpeed(speed);
		Motor.A.rotate(motorDegrees, true);
		Motor.D.rotate(motorDegrees);
	}

	public static void moveBackward(double distance, int speed) {
		double robotMoveRatio = 0.5;

		int motorDegrees = (int)(distance / robotMoveRatio);
		Motor.A.setSpeed(speed);
		Motor.D.setSpeed(speed);
		Motor.A.rotate(-motorDegrees, true);
		Motor.D.rotate(-motorDegrees);
	}

	public static void turnInPlace(int angle, int speed) {
        double robotTurnRatio = 2.0;

        int motorDegrees = (int)(Math.abs(angle) * robotTurnRatio);

        Motor.A.setSpeed(speed);
        Motor.D.setSpeed(speed);

        if (angle > 0) {
            // Turn right: A forward, D backward
            Motor.A.rotate(motorDegrees, true);
            Motor.D.rotate(-motorDegrees);
        } else {
            // Turn left: A backward, D forward
            Motor.A.rotate(-motorDegrees, true);
            Motor.D.rotate(motorDegrees);
        }
	}
}
