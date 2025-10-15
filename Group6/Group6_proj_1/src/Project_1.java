
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
import lejos.hardware.Button;
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

//        Draw_Numbers(rotateArm, rearWheel, liftArm);
//
//        //		while ( running.get() ) {
//        //			// Main loop can be used to process commands or other tasks
//        //		}
//        int[][] pixelGrid = new int[8][8];
//        rollingSweepScan(lightSensor, rotateArm, rearWheel, pixelGrid);
//        int digit = recognizeDigitFromGrid(pixelGrid);
//
//        DrawDigit(digit, 0, 0, rotateArm, rearWheel, liftArm);
        
        Draw_Numbers(rotateArm, rearWheel, liftArm);

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

    public static void rollingScanRow(int row, int gridCols, NXTRegulatedMotor rearWheel, EV3ColorSensor lightSensor, int[][] pixelGrid) {
        final int COL_STEP = 10; // degrees per column
        final int SCAN_SPEED = 20; // slow speed for smooth scan
        final int SAMPLE_INTERVAL_MS = 100; // sample every 100 ms

        rearWheel.setSpeed(SCAN_SPEED);

        int startPos = 0;
        int endPos = (gridCols - 1) * COL_STEP;

        rearWheel.rotateTo(startPos, false); // Move to start
        SampleProvider sp = lightSensor.getRedMode();
        float[] sample = new float[sp.sampleSize()];

        rearWheel.rotateTo(endPos, true); // Start moving

        int col = 0;
        while (rearWheel.isMoving() && col < gridCols) {
            sp.fetchSample(sample, 0);
            float value = sample[0];
            pixelGrid[row][col] = (value < 0.3f) ? 1 : 0;
            LCD.drawString("" + pixelGrid[row][col], col, row + 1);
            Delay.msDelay(SAMPLE_INTERVAL_MS);
            col++;
        }
        rearWheel.stop();
    }

    public static void rollingSweepScan(EV3ColorSensor lightSensor, EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, int[][] pixelGrid) {
        final int GRID_ROWS = pixelGrid.length;
        final int GRID_COLS = pixelGrid[0].length;
        final int ROW_STEP = 5; // degrees per row

        for (int row = 0; row < GRID_ROWS; row++) {
            rotateArm.rotateTo(row * ROW_STEP, false); // Move to row
            rollingScanRow(row, GRID_COLS, rearWheel, lightSensor, pixelGrid);
        }
    }

    /**
     * Moves the pen to a specific grid position (row, col). If isDrawing is
     * true, lowers the pen after moving; otherwise, keeps it up for scanning.
     */
    public static void MovePen(int row, int col, EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm, boolean isDrawing) {
        final int ROW_STEP = 5;  // Smaller movement per row (degrees)
        final int COL_STEP = 10; // Smaller movement per col (degrees)
        final int ARM_SPEED = 20; // Slow for smooth movement
        final int WHEEL_SPEED = 30; // Slow for rear wheel

        penUp(liftArm, 100, 200); // Always lift pen before moving

        rotateArm.setSpeed(ARM_SPEED);
        rearWheel.setSpeed(WHEEL_SPEED);

        int armMove = row * ROW_STEP;
        int wheelMove = col * COL_STEP;

        rotateArm.rotateTo(armMove, false); // Blocking
        rearWheel.rotateTo(wheelMove, false); // Blocking

        if (isDrawing) {
            penDown(liftArm, 100, 200); // Lower pen after moving
        }
        LCD.drawString("Pen at (" + row + "," + col + ")", 0, 4);
    }

    /**
     * Simple digit recognition from pixel grid. This is a stub. Replace with a
     * real recognition algorithm or ML model.
     */
    public static int recognizeDigitFromGrid(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        // Count dark pixels in center column
        int centerCol = cols / 2;
        int verticalCount = 0;
        for (int row = 0; row < rows; row++) {
            if (grid[row][centerCol] == 1) {
                verticalCount++;
            }
        }

        // Count dark pixels in top row
        int topCount = 0;
        for (int col = 0; col < cols; col++) {
            if (grid[0][col] == 1) {
                topCount++;
            }
        }

        // Count dark pixels in bottom row
        int bottomCount = 0;
        for (int col = 0; col < cols; col++) {
            if (grid[rows - 1][col] == 1) {
                bottomCount++;
            }
        }

        // Count dark pixels in left and right columns
        int leftCount = 0, rightCount = 0;
        for (int row = 0; row < rows; row++) {
            if (grid[row][0] == 1) {
                leftCount++;
            }
            if (grid[row][cols - 1] == 1) {
                rightCount++;
            }
        }

        // Count total dark pixels
        int totalDark = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] == 1) {
                    totalDark++;
                }
            }
        }

        // Simple rules for digit recognition
        if (verticalCount > rows * 0.7 && topCount < cols * 0.3 && bottomCount < cols * 0.3) {
            return 1; // vertical line only

        }
        if (topCount > cols * 0.7 && verticalCount < rows * 0.5 && bottomCount < cols * 0.3) {
            return 7; // top line only

        }
        if (topCount > cols * 0.7 && bottomCount > cols * 0.7 && leftCount > rows * 0.7 && rightCount > rows * 0.7) {
            return 0; // closed loop

        }
        if (topCount > cols * 0.7 && bottomCount > cols * 0.7 && verticalCount > rows * 0.7) {
            return 8; // double loop (8)

        }
        if (topCount > cols * 0.7 && rightCount > rows * 0.7 && bottomCount > cols * 0.7 && leftCount < rows * 0.3) {
            return 9; // right loop

        }
        if (topCount > cols * 0.7 && leftCount > rows * 0.7 && bottomCount > cols * 0.7 && rightCount < rows * 0.3) {
            return 6; // left loop

        }
        if (topCount > cols * 0.7 && verticalCount > rows * 0.3 && bottomCount < cols * 0.3 && rightCount > rows * 0.7) {
            return 2; // top, right, curve

        }
        if (topCount < cols * 0.3 && bottomCount > cols * 0.7 && leftCount > rows * 0.7 && verticalCount > rows * 0.3) {
            return 5; // bottom, left, curve

        }
        if (leftCount > rows * 0.7 && verticalCount > rows * 0.3 && topCount < cols * 0.3 && bottomCount < cols * 0.3) {
            return 4; // left and vertical

        }
        if (topCount > cols * 0.7 && verticalCount > rows * 0.3 && bottomCount > cols * 0.7 && rightCount < rows * 0.3 && leftCount < rows * 0.3) {
            return 3; // top, vertical, bottom
        }
        return 8; // fallback if no rule matches
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

    public static void Draw_Numbers(EV3LargeRegulatedMotor rotateArm, NXTRegulatedMotor rearWheel, EV3LargeRegulatedMotor liftArm) {
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
        final int SMALL_MOVE = -60;
        final int MED_MOVE = -30;
        final int LARGE_MOVE = 120 * 2;
        final int DELAY_MS = 1000;

        // Move pen to starting position
        int startTacho = rotateArm.getTachoCount();
        rearWheel.setSpeed(ARM_SPEED);
        rotateArm.setSpeed(ARM_SPEED);
        liftArm.setSpeed(PEN_SPEED);

        switch (digit) {
            case 0:
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveWheel(rearWheel, -MED_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, MED_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 1:
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveWheel(rearWheel, MED_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 2:
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, -SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, -SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 3:
            	moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 4:
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, -SMALL_MOVE, DELAY_MS);
                moveWheel(rearWheel, 2 * SMALL_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 5:
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 6:
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, MED_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, -SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 7:
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 8:
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, -SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveWheel(rearWheel, -2 * SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            case 9:
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                penDown(liftArm, PEN_SPEED, PEN_ANGLE);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, SMALL_MOVE, DELAY_MS);
                moveArm(rotateArm, -LARGE_MOVE, DELAY_MS);
                moveWheel(rearWheel, -SMALL_MOVE, DELAY_MS);
                moveWheel(rearWheel, MED_MOVE, DELAY_MS);
                moveArm(rotateArm, LARGE_MOVE, DELAY_MS);
                penUp(liftArm, PEN_SPEED, PEN_ANGLE);
                Delay.msDelay(DELAY_MS);
                break;
            default:
                LCD.drawString("Invalid digit", 0, 0);
                Delay.msDelay(DELAY_MS);
                break;
        }
        moveWheel(rearWheel, MED_MOVE * 2, DELAY_MS); // Move to next digit position

        int currentTacho = rotateArm.getTachoCount();
        int deltaTacho = currentTacho - startTacho;
        moveArm(rotateArm, -deltaTacho, DELAY_MS); // Return to starting position
        penUp(liftArm, PEN_SPEED, PEN_ANGLE);
    }

    public static void moveArm(EV3LargeRegulatedMotor rotateArm, int amount, int delay) {
        rotateArm.rotate(amount);
        Delay.msDelay(delay);
    }

    public static void moveWheel(NXTRegulatedMotor rearWheel, int amount, int delay) {
        rearWheel.rotate(amount);
        Delay.msDelay(delay);
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
