import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lejos.hardware.Button;

import lejos.robotics.navigation.Pose;

public class RobotClient {
    
    private static final String SERVER_IP = "10.0.1.8";
    private static final int SERVER_PORT = 12345;
    
    private RobotController robot;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public static void main(String[] args) {
        new RobotClient().run();
    }

    public void run() {
        robot = new RobotController();
        robot.initHardware();
        robot.startThreads(); // Starts sensor polling (Warehouse Thread)
        
        // Connect to Server
        connectToServer();
        
        // Thread 1: Communication (Sender)
        Thread senderThread = new Thread(new Runnable() {
            public void run() {
                while (running && !socket.isClosed()) {
                    try {
                        // Format: DATA:LOW,HIGH,GYRO,IR,IS_MOVING,X,Y,HEADING
                        Pose pose = robot.getNav().getPoseProvider().getPose();
                        String data = String.format("DATA:%.2f,%.2f,%.2f,%.2f,%b,%.2f,%.2f,%.2f", 
                            robot.getLowDistance(),
                            robot.getHighDistance(),
                            robot.getGyroAngle(),
                            robot.getIRDistance(),
                            robot.getPilot().isMoving(),
                            pose.getX(),
                            pose.getY(),
                            pose.getHeading()
                        );
                        if (out != null) {
                            out.println(data);
                        }
                        Thread.sleep(50); // 20Hz update rate
                    } catch (Exception e) {
                        System.out.println("Sender Error: " + e.getMessage());
                        break;
                    }
                }
            }
        });
        senderThread.start();
        
        // Thread 2: Communication (Receiver)
        Thread receiverThread = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while (running && (line = in.readLine()) != null) {
                        System.out.println("Received: " + line);
                        commandQueue.offer(line);
                    }
                } catch (IOException e) {
                    System.out.println("Receiver Error: " + e.getMessage());
                }
            }
        });
        receiverThread.start();
        
        // Thread 3: Pilot Control (Consumer)
        Thread pilotThread = new Thread(new Runnable() {
            public void run() {
                controlPilot();
            }
        });
        pilotThread.start();
        
        // Main thread waits for Escape
        while (running) {
            if (Button.ESCAPE.isDown()) {
                running = false;
                System.exit(0);
            }
            try { Thread.sleep(100); } catch (Exception e) {}
        }
    }
    
    private void connectToServer() {
        while (true) {
            try {
                System.out.println("Connecting to " + SERVER_IP + ":" + SERVER_PORT + "...");
                socket = new Socket(SERVER_IP, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.println("Connected!");
                break;
            } catch (Exception e) {
                System.out.println("Connection failed. Retrying in 2s...");
                try { Thread.sleep(2000); } catch (Exception ex) {}
            }
        }
    }
    
    private void controlPilot() {
        while (running) {
            try {
                // Check Asimov's Laws (Highest Priority)
                checkSafety();
                
                // Process Commands
                if (!commandQueue.isEmpty()) {
                    String cmd = commandQueue.poll();
                    executeCommand(cmd);
                }
                
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void checkSafety() {
        // Asimov's First Law: A robot may not allow harm to come to itself.
        // If moving forward and obstacle is close, STOP.
        if (robot.getPilot().isMoving()) {
            float dist = robot.getEV3Distance(); // High sensor for walls
            if (dist < 20 && dist > 0) {
                // Check if we are actually moving forward (not backward or rotating in place)
                // DifferentialPilot doesn't easily expose "moving forward", but we can assume
                // if distance is decreasing rapidly... or just be safe.
                // For now, if close, just stop.
                
                // Exception: If we are rotating, we might be fine? 
                // But if we are too close to a wall, rotation might hit it.
                
                System.out.println("SAFETY STOP: Obstacle at " + dist);
                robot.getPilot().stop();
                commandQueue.clear(); // Clear pending commands to prevent immediate resume
                lejos.hardware.Sound.buzz();
            }
        }
    }
    
    private void executeCommand(String cmd) {
        String[] parts = cmd.split(" ");
        String action = parts[0];
        
        // Pre-execution Safety Check
        if (action.equals("FORWARD") || action.equals("TRAVEL")) {
            if (robot.getEV3Distance() < 25) {
                System.out.println("Refusing to move forward: Obstacle detected.");
                lejos.hardware.Sound.buzz();
                return;
            }
        }
        
        switch (action) {
            case "FORWARD":
                robot.getPilot().forward();
                break;
            case "BACKWARD":
                robot.getPilot().backward();
                break;
            case "LEFT":
                robot.getPilot().rotateLeft();
                break;
            case "RIGHT":
                robot.getPilot().rotateRight();
                break;
            case "STOP":
                robot.getPilot().stop();
                break;
            case "KICK":
                robot.getPilot().setLinearSpeed(robot.getPilot().getMaxLinearSpeed());
                robot.getPilot().travel(15);
                robot.getPilot().travel(-15);
                robot.getPilot().setLinearSpeed(6); // Reset speed
                break;
            case "ARM_UP":
                robot.getArmMotor().rotate(90);
                break;
            case "ARM_DOWN":
                robot.getArmMotor().rotate(-90);
                break;
            case "ROTATE":
                if (parts.length > 1) {
                    float angle = Float.parseFloat(parts[1]);
                    // Non-blocking rotate to allow safety checks and command interrupts
                    robot.getPilot().rotate(angle, true);
                }
                break;
            case "TRAVEL":
                if (parts.length > 1) {
                    float dist = Float.parseFloat(parts[1]);
                    // Non-blocking travel to allow safety checks and command interrupts
                    robot.getPilot().travel(dist, true);
                }
                break;
            case "SET_POSE":
                if (parts.length > 3) {
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float heading = Float.parseFloat(parts[3]);
                    robot.getNav().getPoseProvider().setPose(new Pose(x, y, heading));
                }
                break;
            case "SET_SPEED":
                if (parts.length > 1) {
                    float speed = Float.parseFloat(parts[1]);
                    robot.getPilot().setLinearSpeed(speed);
                }
                break;
        }
    }
    
    // Removed waitForCompletion() as we are now non-blocking
}
