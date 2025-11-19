package first;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.utility.Delay;

public class TurnTest {

	static int TIMEOUT = 20000;
	
	public static void main(String[] args) throws InterruptedException {
		EV3LargeRegulatedMotor left = new EV3LargeRegulatedMotor(MotorPort.A);
		EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.B);
//		left.setSpeed(60);
//		right.setSpeed(60);
//		left.setAcceleration(1000);
//		right.setAcceleration(1000);
		Wheel wheel1 = WheeledChassis
				.modelWheel(left, MovePilot.WHEEL_SIZE_NXT1 * 10).offset(-56);
		Wheel wheel2 = WheeledChassis
				.modelWheel(right, MovePilot.WHEEL_SIZE_NXT1 * 10).offset(56);
		Chassis chassis = new WheeledChassis(new Wheel[]{wheel1, wheel2}, 
				WheeledChassis.TYPE_DIFFERENTIAL); 
		MovePilot pilot = new MovePilot(chassis);
		pilot.setLinearSpeed(30);
		pilot.setAngularSpeed(45);	
		
		// wait till sensors and thread are ready
		SensorThread st = new SensorThread();
		st.start();
//		while(!st.getReady()) {
//			Delay.msDelay(100);
//		}
		
		Button.waitForAnyPress();	
		
		// start the rotation
//		left.forward();
//		right.backward();
//		for (int angle = 0; angle < 90 && st.getAngle() > -90; 
//				angle = st.getAngle()) {
//			//pilot.rotate(1.5);
//			Delay.msDelay(100);
//		}
//		left.stop(true);
//		right.stop(true);
		pilot.rotate(360);
		
		LCD.drawString("End... ", 1, 3);
		Button.waitForAnyPress(TIMEOUT);
		st.setRunning(false);
		pilot.stop();
		left.close();
		right.close();
		Delay.msDelay(1000);
		System.exit(0);
	}

}

class SensorThread extends Thread {
	private int angle;
	private EV3GyroSensor gs;
//	private boolean ready = false;
	private boolean running;
	
	public SensorThread() {
		angle = 0;
		gs = new EV3GyroSensor(SensorPort.S1);
		LCD.drawString("Sensors Ready.", 1, 1);
//		ready = true;
		running = true;
	}
	
	public int getAngle() {
		return angle;
	}
	
//	public boolean getReady() {
//		return ready;
//	}
	
	public void setRunning(boolean value) {
		running = value;
	}
	
	public void run() {
		
		SampleProvider sp = gs.getAngleMode();
		
		// keep monitoring the angle until reaching angle
		while (running) {
			float[] sample = new float[sp.sampleSize()];
			sp.fetchSample(sample, 0);
			angle = (int)sample[0];
			LCD.drawString("Angle: " + angle, 1, 2);
			Delay.msDelay(100);			
		}

	}
}
