
import client.GyroSensor;
import client.LightSensor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import lejos.hardware.port.*;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.*;
import lejos.utility.Delay;

public class Project_1 {

    public static void main(String[] args) {
        NXTRegulatedMotor rearWheel = new NXTRegulatedMotor(MotorPort.A);
        EV3LargeRegulatedMotor rotateArm = new EV3LargeRegulatedMotor(MotorPort.D);
        EV3LargeRegulatedMotor liftArm = new EV3LargeRegulatedMotor(MotorPort.B);

        EV3ColorSensor lightSensor = new EV3ColorSensor(SensorPort.S1);
        EV3GyroSensor gyroSensor = new EV3GyroSensor(SensorPort.S2);

        BufferedReader in = null;
        BufferedWriter out = null;
        final AtomicBoolean running = new AtomicBoolean(true);
        int[] lightData = new int[3];
        SampleProvider gyroData = gyroSensor.getAngleAndRateMode();
        float MAX_LEFT = 0, MAX_RIGHT = 0;

        Thread sensorThread = new Thread(new SensorThread_proj1(lightData, gyroData, running, lightSensor, gyroSensor));
        sensorThread.start();

        // initRotateArmLimits(rotateArm, gyroSensor, MAX_LEFT, MAX_RIGHT);
        // Kong_Test(rotateArm, rearWheel, liftArm);

        // testPen(rotateArm, rearWheel, liftArm);
        //		while ( running.get() ) {
        //			// Main loop can be used to process commands or other tasks
        //		}
        // Cleanup

        readAndRewriteDigit(lightSensor, rotateArm, rearWheel, liftArm);
        running.set(false);
        try {
            sensorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rearWheel.close();
        rotateArm.close();
        liftArm.close();
        lightSensor.close();
        gyroSensor.close();

    }

    /**
     * Reads a handwritten digit using the light sensor and redraws it using the robot's motors.
     * This function assumes the robot scans the paper row by row, storing the pixel data,
     * then uses the DrawDigit function to reproduce the digit.
     */
    public static void readAndRewriteDigit(EV3ColorSensor lightSensor, EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm) {
        final int GRID_ROWS = 8; // Adjust for your scanning resolution
        final int GRID_COLS = 8;
        int[][] pixelGrid = new int[GRID_ROWS][GRID_COLS];

        SampleProvider sp = lightSensor.getRedMode();
        float[] sample = new float[sp.sampleSize()];

        LCD.clear();
        LCD.drawString("Scanning...", 0, 0);

        // Scan the paper grid row by row
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                // Move sensor to (row, col) position
                MovePen(row, col, rotateArm, rearWheel, liftArm);
                Delay.msDelay(200); // Allow time for movement

                // Read light value
                sp.fetchSample(sample, 0);
                float value = sample[0];

                // Threshold: 1 = dark (ink), 0 = light (paper)
                pixelGrid[row][col] = (value < 0.3f) ? 1 : 0;

                LCD.drawString("" + pixelGrid[row][col], col, row + 1);
            }
        }

        LCD.clear();
        LCD.drawString("Recognizing...", 0, 0);

        // Simple recognition: find the digit by pattern matching (replace with ML for better results)
        int recognizedDigit = recognizeDigitFromGrid(pixelGrid);

        LCD.drawString("Digit: " + recognizedDigit, 0, 1);
        Delay.msDelay(2000);

        LCD.clear();
        LCD.drawString("Rewriting...", 0, 0);

        // Redraw the digit using the robot
        DrawDigit(recognizedDigit, 0, 0, rotateArm, rearWheel, liftArm);

        LCD.drawString("Done!", 0, 2);
    }

    /**
     * Simple digit recognition from pixel grid.
     * This is a stub. Replace with a real recognition algorithm or ML model.
     */
    public static int recognizeDigitFromGrid(int[][] grid) {
        // Example: If the center column is mostly dark, guess "1"
        int cols = grid[0].length;
        int darkCount = 0;
        for (int row = 0; row < grid.length; row++) {
            if (grid[row][cols / 2] == 1) darkCount++;
        }
        if (darkCount > grid.length / 2) return 1;

        // Add more pattern checks for other digits
        // For now, always return 1
        return 1;
    }

    private static void initRotateArmLimits(EV3LargeRegulatedMotor rotateArm, EV3GyroSensor gyroSensor, float maxLeft, float maxRight) {
        final int step = 720; // degrees
        final int speed = 500; // motor speed
        float lastAngleLeft, lastAngleRight, currentAngle;

        rotateArm.setSpeed(speed);

        float[] sample = new float[1];
        gyroSensor.getAngleMode().fetchSample(sample, 0);
        lastAngleLeft = sample[0];
        LCD.clear();
        LCD.drawString("Init: Left Sweep", 0, 0);

        while (true) {
            rotateArm.rotate(-step, true); // non-blocking
            while (rotateArm.isMoving()) {
                Thread.yield();
            }
            gyroSensor.getAngleMode().fetchSample(sample, 0);
            currentAngle = sample[0];
            LCD.drawString("Angle: " + currentAngle, 0, 1);
            if (Math.abs(currentAngle - lastAngleLeft) < 1) {
                LCD.drawString("Left limit: " + currentAngle, 0, 2);
                break;
            }
            lastAngleLeft = currentAngle;
        }

        rotateArm.stop();
        rotateArm.resetTachoCount();
        rotateArm.setSpeed(speed);

        gyroSensor.getAngleMode().fetchSample(sample, 0);
        lastAngleRight = sample[0];
        LCD.clear();
        LCD.drawString("Init: Right Sweep", 0, 0);

        while (true) {
            rotateArm.rotate(step, true); // non-blocking
            while (rotateArm.isMoving()) {
                Thread.yield();
            }
            gyroSensor.getAngleMode().fetchSample(sample, 0);
            currentAngle = sample[0];
            LCD.drawString("Angle: " + currentAngle, 0, 1);
            if (Math.abs(currentAngle - lastAngleRight) < 1) {
                LCD.drawString("Right limit: " + currentAngle, 0, 2);
                break;
            }
            lastAngleRight = currentAngle;
        }
        LCD.drawString("Init Done", 0, 4);
        rotateArm.stop();
        maxLeft = lastAngleLeft;
        maxRight = lastAngleRight;
    }

    public static void penDown(EV3LargeRegulatedMotor liftArm, int PEN_SPEED, int PEN_ANGLE) {
        liftArm.setSpeed(PEN_SPEED);
        liftArm.rotate(PEN_ANGLE);
    }

    public static void penUp(EV3LargeRegulatedMotor liftArm, int PEN_SPEED, int PEN_ANGLE) {
        liftArm.setSpeed(PEN_SPEED);
        liftArm.rotate(-PEN_ANGLE); // Negative angle lifts pen
    }

    public static void testPen(EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm) {
        final int PEN_ANGLE = 200;
        final int PEN_SPEED = 100;

        final int ARM_DRAW_SPEED = 100;

        rotateArm.setSpeed(ARM_DRAW_SPEED);
        rearWheel.setSpeed(100);
        penDown(liftArm, PEN_SPEED, PEN_ANGLE);

        // Move arm to starting position
        rotateArm.rotate(-270);

        // Draw 1
        rearWheel.rotate(30);

        Delay.msDelay(3000);

        rotateArm.rotate(270);
        Delay.msDelay(1000);
        rearWheel.rotate(30);

        rotateArm.rotate(-270);
        penUp(liftArm, PEN_SPEED, PEN_ANGLE); //shift horizontally down
        rearWheel.rotate(30);
        // RECUURSIVE LOOP BELOW
        // testpen(rotateArm, rearWheel, liftArm);
    }

    public static void Kong_Test(EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm) {
        final int PEN_ANGLE = 200;
        final int PEN_SPEED = 100;

         DrawDigit(1, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(2, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(3, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(4, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(5, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(6, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(7, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(8, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(9, 0, 0, rotateArm, rearWheel, liftArm);
         DrawDigit(0, 0, 0, rotateArm, rearWheel, liftArm);
    }

    public static void DrawDigit(int digit, int row, int col, EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm) {
    final int PEN_ANGLE = 200;
    final int PEN_SPEED = 100;
    final int ARM_SPEED = 100;
    final int SMALL_MOVE = -20 * 2;
    final int MED_MOVE = -30 * 2;
    final int LARGE_MOVE = 120 * 2;
    final int DELAY_MS = 1000;

    // Move pen to starting position
    int startTacho = rotateArm.getTachoCount();
    rearWheel.setSpeed(ARM_SPEED);
    rotateArm.setSpeed(ARM_SPEED);
    liftArm.setSpeed(PEN_SPEED);

    int rotateAngle = col * LARGE_MOVE; // Example calculation, adjust as needed
    int moveDistance = row * MED_MOVE;  // Example calculation, adjust as needed

    switch (digit) {
        case 0:
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rearWheel.rotate(-MED_MOVE); // move back
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE); // move right
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(MED_MOVE); // move forward
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE); // move left
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 1:
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rearWheel.rotate(MED_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 2:
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(LARGE_MOVE); // move right
            Delay.msDelay(DELAY_MS);
            // rotateArm.rotate(LARGE_MOVE); // move left
            // Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE); // move back
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE); // move left
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE); // move back
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE); // move right
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 3:
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(-LARGE_MOVE); // move right
            Delay.msDelay(DELAY_MS);
            // rotateArm.rotate(LARGE_MOVE); // move left
            // Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE); // move back
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE); //move left
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE); // move right
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE); // move back 
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE); // move left 
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE); // move right
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 4:
        	rotateArm.rotate(LARGE_MOVE);
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rearWheel.rotate(SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(2 * SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 5:
        	rotateArm.rotate(-LARGE_MOVE);
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(MED_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(MED_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 6:
        	rotateArm.rotate(-LARGE_MOVE);
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(MED_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 7:
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(MED_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 8:
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-2 * SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        case 9:
        	rotateArm.rotate(-LARGE_MOVE);
            penDown(liftArm, PEN_SPEED, PEN_ANGLE);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(-LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(-SMALL_MOVE);
            Delay.msDelay(DELAY_MS);
            rearWheel.rotate(MED_MOVE);
            Delay.msDelay(DELAY_MS);
            rotateArm.rotate(LARGE_MOVE);
            Delay.msDelay(DELAY_MS);
            penUp(liftArm, PEN_SPEED, PEN_ANGLE);
            Delay.msDelay(DELAY_MS);
            break;
        default:
            LCD.drawString("Invalid digit", 0, 0);
            Delay.msDelay(DELAY_MS);
            break;
    }
    rearWheel.rotate(MED_MOVE * 2); // Move to next digit position
    Delay.msDelay(DELAY_MS);

    int currentTacho = rotateArm.getTachoCount();
    int deltaTacho = currentTacho - startTacho;
    rotateArm.rotate(-deltaTacho); // Return to starting position
    Delay.msDelay(DELAY_MS);
    penUp(liftArm, PEN_SPEED, PEN_ANGLE);
}

  public static void MovePen(int row, int col, EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm) {
      final int ROW_STEP = 30; // degrees or mm per row
      final int COL_STEP = 30; // degrees or mm per col

      penUp(liftArm, 100, 200); // Lift pen before moving

      // Calculate movement needed from current position (assumes starting at 0,0)
      int armMove = row * ROW_STEP;   // vertical movement
      int wheelMove = col * COL_STEP; // horizontal movement

      rotateArm.rotateTo(armMove);    // Move to target row
      rearWheel.rotateTo(wheelMove);  // Move to target column

      penDown(liftArm, 100, 200); // Lower pen after moving (if drawing)
      LCD.drawString("Pen at (" + row + "," + col + ")", 0, 4);
  }
////////////////////////////////////////// ADD FUNCTIONS BEFORE THIS LINE //////////////////////////////////////////
}



class SensorThread_proj1 implements Runnable {

    private AtomicBoolean running;
    private EV3ColorSensor lightSensor;
    private EV3GyroSensor gyroSensor;
    private int[] lightData;
    private SampleProvider gyroData;

    public SensorThread_proj1(int[] lightData, SampleProvider gyroData, AtomicBoolean running, EV3ColorSensor lightSensor, EV3GyroSensor gyroSensor) {
        this.lightData = lightData;
        this.gyroData = gyroData;
        this.running = running;
        this.lightSensor = lightSensor;
        this.gyroSensor = gyroSensor;

        this.lightSensor.setCurrentMode("RGB");
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                float[] rgb = new float[3];
                SampleProvider sp = lightSensor.getRGBMode();
                sp.fetchSample(rgb, 0);
                lightData[0] = (int) (rgb[0] * 100);
                lightData[1] = (int) (rgb[1] * 100);
                lightData[2] = (int) (rgb[2] * 100);

                float[] sample = new float[gyroData.sampleSize()];
                gyroData.fetchSample(sample, 0);
                // sample[0] = angle (degrees)
                // sample[1] = rate (degrees/second)
                Thread.sleep(100); // Adjust the sleep time as needed
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lightSensor.close();
            gyroSensor.close();
        }
    }
}

// Mel's section (The void)

/*

static final int PEN_ANGLE = 10;
static final int PEN_SPEED = 50; 

static final int ARM_DRAW_SPEED = 150;

		NXTRegulatedMotor rearWheel = new NXTRegulatedMotor(MotorPort.A);
		EV3LargeRegulatedMotor rotateArm = new EV3LargeRegulatedMotor(MotorPort.D);
		EV3LargeRegulatedMotor liftArm = new EV3LargeRegulatedMotor(MotorPort.B);

public void penDown(EV3LargeRegulatedMotor liftArm) {
    liftArm.setSpeed(PEN_SPEED);
    liftArm.rotate(PEN_ANGLE);
}

public void penUp(EV3LargeRegulatedMotor liftArm) {
    liftArm.setSpeed(PEN_SPEED);
    liftArm.rotate(-PEN_ANGLE); // Negative angle lifts pen
}


public void testPen(EV3LargeRegulatedMotor rotateArm, NXTLargeRegulatedMotor rearWheel) {
    rotateArm.setSpeed(ARM_DRAW_SPEED);

    // Move arm to starting position
    rotateArm.rotate(-20);

    penDown(); 

    // Draw 1
    rearWheel.rotate(90);

    penUp();

	Delay.msDelay(3000);

    // Return to starting position
    motorB.rotate(-20);

	Delay.msDelay(3000);

	//Draw 7
	rotateArm.rotate(90);
		Delay.msDelay(1000);
	rearWheel.rotate(90);

	rotateArm.rotate(-90);
}


public class WriteDigit(int digit, int row, int col) { // maybe change parameters later if needed

	final int PEN_ANGLE = 40;
	final int PEN_SPEED = 120; 
	final int ARM_DRAW_SPEED = 100;

	static int ZERO = 0
	static int ONE = 1
	static int TWO = 2
	static int THREE = 3
	static int FOUR = 4
	static int FIVE = 5
	static int SIX = 6
	static int SEVEN = 7
	static int EIGHT = 8
	static int NINE = 9

	switch(digit) // switch statement 
	  {
	case ZERO: //DONE ???
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
			rearWheel.rotate(-40);
			rotateArm.rotate(-90);
			rearWheel.rotate(40);
			rotateArm.rotate(90);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case ONE: //DONE ???
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
			rearWheel.rotate(40);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case TWO: //DONE ???
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
			rotateArm.rotate(90);
			rearWheel.rotate(-40);
			rotateArm.rotate(-90);
			rearWheel.rotate(-40);
			rotateArm.rotate(90);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case THREE: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
			rotateArm.rotate(-90);
			rotateArm.rotate(90);
			rearWheel.rotate(40);
			rotateArm.rotate(-90);
			rotateArm.rotate(90);
			rearWheel.rotate(40);
		penUp(liftArm,  PEN_SP
			rotateArm.rotate(-90);
			rotateArm.rotate(90);EED, PEN_ANGLE);
    break;
    case FOUR: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
		

				penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case FIVE: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case SIX: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case SEVEN: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
    case EIGHT: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
	break;
    case NINE: //NOT DONE
		penDown(liftArm,  PEN_SPEED, PEN_ANGLE);
		penUp(liftArm,  PEN_SPEED, PEN_ANGLE);
    break;
  }
  MovePen(row,col,startX * xStep,startY*yStep); //Have 
  PenDown();
  for(int i=0;i<StrLen(sequence);i++)
  {
}

char c=StrCharAt(sequence,i);
	switch(c)
	{
	  case 'U':
		motorB.rotate(-yStep);
		break;
	  case 'D':
		motorB.rotate(yStep);
		break;
	  case 'L':
		motorA.rotate(-xStep);
		break;
	  case 'R':
		motorA.rotate(xStep);
		break;
	}
  }
  PenUp();
}
void DrawImage()
{
  for(int y=0;y<SIZE_Y;y++)
  {
    RectOut(0,y*2,17,1);
    for(int x=0;x<SIZE_X;x++)
    {
      if(image[y*SIZE_X+x]==0)
        RectOut(18+x*2,y*2,1,1);
    }
    RectOut(82,y*2,17,1);
  }
}


void Recognization(int r, int c) // Can be used for the bonus assignment((IF WE HAVE TIME))
{
  int topPixel;
  int botPixel=0;
  int leftPixel=SIZE_X;
  int rightPixel=0;
  for(int i=0;i<SIZE_X*SIZE_Y;i++)
    if(image[i])
    {
      if(!botPixel)
        botPixel=i;
      topPixel=i;
      int col=i%SIZE_X;
      if(col<leftPixel)
        leftPixel=col;
      if(col>rightPixel)
        rightPixel=col;
    }
  int topRow=topPixel/SIZE_X;
  int botRow=botPixel/SIZE_X;
  int width=rightPixel-leftPixel+1;
  int longTipUpper=0;
  int longTipLower=0;
  int leftTips=0;
  int topTip=0;
  int botTip=0;
  for(int i=0;i<SIZE_X*SIZE_Y;i++)
    if(image[i])
    {
      bool longTip=false;
      int tip=Tip(i,longTip);
      if(tip)
      {
        if(longTip)
        {
          if(!longTipLower)
            longTipLower=tip;
          else if(!longTipUpper)
            longTipUpper=tip;
        }
        if(tip==WEST || tip==NORTHWEST || tip==SOUTHWEST)
          leftTips++;
        if(i/SIZE_X==topRow)
          topTip=tip;
        if(i/SIZE_X==botRow)
          botTip=tip;
      }
    }
  int digit;
  if(width<6)
    digit=1;
  else if(leftTips==3 ||((longTipUpper == WEST||longTipUpper==NORTHWEST) && (longTipLower==WEST ||longTipLower==NORTHWEST) && botTip==0))
    digit=3;
  else if((longTipUpper==WEST||longTipUpper==NORTHWEST||longTipUpper==SOUTHWEST) && (longTipLower==WEST||longTipLower==SOUTHWEST||longTipLower==SOUTH))
    digit=7;
  else if(leftTips && (longTipLower==EAST||longTipLower==SOUTHEAST||longTipLower==NORTHEAST) && topTip!=NORTHEAST && topTip!=EAST)
    digit=2;
  else if((longTipUpper==EAST||longTipUpper==NORTHEAST||longTipUpper==SOUTHEAST) && (longTipLower==WEST||longTipLower==SOUTHWEST||longTipLower==NORTHWEST))
    digit=5;
  else if(topTip==NORTH||topTip==NORTHEAST||topTip==EAST)
    digit=6;
  else if(botTip==SOUTH||botTip==SOUTHWEST||botTip==WEST || longTipLower==WEST||longTipLower==SOUTHWEST||longTipLower==SOUTH)
    digit=9;
  else if((topTip==NORTH||topTip==NORTHEAST||topTip==NORTHWEST) &&(botTip==SOUTH||botTip==SOUTHWEST||botTip==SOUTHEAST))
    digit=4;
  else
    digit=8;
  NumOut(45,LCD_LINE5,digit,true);
  string soundFile;
  string ds=NumToStr(digit);
  soundFile=StrCat("0",ds,".rso");
  PlayFile(soundFile);
  sudoku[r*9 + c] = 1<<(digit-1);
}
*/
