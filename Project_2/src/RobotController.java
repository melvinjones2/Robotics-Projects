import lejos.hardware.Button;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.NXTUltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Navigator;

public class RobotController {
    // Hardware
    private EV3UltrasonicSensor ev3UltrasonicSensor;
    private NXTUltrasonicSensor nxtUltrasonicSensor;
    private EV3GyroSensor gyroSensor;
    private EV3IRSensor irSensor;
    
    private EV3MediumRegulatedMotor armMotor;
    private EV3LargeRegulatedMotor leftMotor;
    private EV3LargeRegulatedMotor rightMotor;
    
    // Navigation
    public DifferentialPilot pilot;
    public Navigator nav;
    
    // Volatile Sensor Data (Thread-safe)
    private volatile float highDistance = 255;
    private volatile float lowDistance = 255;
    private volatile float gyroAngle = 0;
    private volatile float irDistance = 255;
    
    private volatile boolean running = true;
    
    public RobotController() {
        // Hardware init and threads start explicitly called by main program
    }
    
    public void initHardware() {
        System.out.println("Initializing Hardware...");
        
        // Sensors
        try {
            ev3UltrasonicSensor = new EV3UltrasonicSensor(SensorPort.S2);
        } catch (Exception e) { System.out.println("Error init EV3 US: " + e.getMessage()); }
        
        try {
            nxtUltrasonicSensor = new NXTUltrasonicSensor(SensorPort.S4);
        } catch (Exception e) { System.out.println("Error init NXT US: " + e.getMessage()); }
        
        try {
            gyroSensor = new EV3GyroSensor(SensorPort.S3);
            System.out.println("Gyro Calibrating...");
            gyroSensor.reset();
        } catch (Exception e) { System.out.println("Error init Gyro: " + e.getMessage()); }
        
        try {
            irSensor = new EV3IRSensor(SensorPort.S1);
        } catch (Exception e) { System.out.println("Error init IR: " + e.getMessage()); }
        
        // Motors
        armMotor = new EV3MediumRegulatedMotor(MotorPort.A);
        leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
        rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
        
        // Navigation Setup
        double diam = DifferentialPilot.WHEEL_SIZE_NXT1; // 5.6 cm
        double trackWidth = 15.5; // cm
        
        pilot = new DifferentialPilot(diam, trackWidth, leftMotor, rightMotor);
        pilot.setLinearSpeed(6);
        pilot.setAngularSpeed(15);
        pilot.setLinearAcceleration(200);
        
        nav = new Navigator(pilot);
        
        System.out.println("Hardware Initialized.");
    }
    
    public void startThreads() {
        // 1. Sensor Polling Thread
        Thread sensorThread = new Thread(new Runnable() {
            public void run() {
                SampleProvider ev3Dist = (ev3UltrasonicSensor != null) ? ev3UltrasonicSensor.getDistanceMode() : null;
                SampleProvider nxtDist = (nxtUltrasonicSensor != null) ? nxtUltrasonicSensor.getDistanceMode() : null;
                SampleProvider gyroAng = (gyroSensor != null) ? gyroSensor.getAngleMode() : null;
                SampleProvider irDistMode = (irSensor != null) ? irSensor.getDistanceMode() : null;
                
                float[] sample = new float[1];
                
                while (running) {
                    // High Sensor
                    if (ev3Dist != null) {
                        ev3Dist.fetchSample(sample, 0);
                        float val = sample[0] * 100;
                        if (Float.isInfinite(val) || val > 255) val = 255;
                        highDistance = val;
                    }
                    
                    // Low Sensor
                    if (nxtDist != null) {
                        nxtDist.fetchSample(sample, 0);
                        float val = sample[0] * 100;
                        if (Float.isInfinite(val) || val > 255) val = 255;
                        lowDistance = val;
                    }
                    
                    // Gyro
                    if (gyroAng != null) {
                        gyroAng.fetchSample(sample, 0);
                        gyroAngle = sample[0];
                    }
                    
                    // IR
                    if (irDistMode != null) {
                        irDistMode.fetchSample(sample, 0);
                        irDistance = sample[0];
                    }
                    
                    try { Thread.sleep(20); } catch (InterruptedException e) {}
                }
            }
        });
        sensorThread.setDaemon(true);
        sensorThread.start();
        
        // 2. Safety/Escape Thread
        Thread safetyThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    if (Button.ESCAPE.isDown()) {
                        System.out.println("\n*** EMERGENCY STOP ***");
                        System.exit(0);
                    }
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }
            }
        });
        safetyThread.setDaemon(true);
        safetyThread.start();
    }
    
    // Accessors
    public float getHighDistance() { return highDistance; }
    public float getLowDistance() { return lowDistance; }
    public float getGyroAngle() { return gyroAngle; }
    public float getIRDistance() { return irDistance; }
    
    public EV3MediumRegulatedMotor getArm() { return armMotor; }
    
    public void resetGyro() {
        if (gyroSensor != null) gyroSensor.reset();
    }
    
    public void stopThreads() {
        running = false;
    }

    public void close() {
        running = false;
        if (ev3UltrasonicSensor != null) ev3UltrasonicSensor.close();
        if (nxtUltrasonicSensor != null) nxtUltrasonicSensor.close();
        if (gyroSensor != null) gyroSensor.close();
        if (irSensor != null) irSensor.close();
        if (armMotor != null) armMotor.close();
        if (leftMotor != null) leftMotor.close();
        if (rightMotor != null) rightMotor.close();
    }
    
    // Additional Accessors for Project2 compatibility
    public DifferentialPilot getPilot() { return pilot; }
    public Navigator getNav() { return nav; }
    public EV3LargeRegulatedMotor getLeftMotor() { return leftMotor; }
    public EV3MediumRegulatedMotor getArmMotor() { return armMotor; }
    
    // Aliases
    public float getNXTDistance() { return getLowDistance(); }
    public float getEV3Distance() { return getHighDistance(); }
}
