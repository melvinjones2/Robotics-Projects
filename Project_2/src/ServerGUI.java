import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import org.opencv.core.Core; // OpenCV Import
import org.opencv.core.Point;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;

public class ServerGUI extends JFrame {
    
    // UI Components
    private JLabel statusLabel;
    private JLabel sensorLabel;
    private JTextArea logArea;
    
    // Setup UI
    private JRadioButton rbBlue, rbGreen, rbAttacker, rbDefender;
    
    // Network
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    
    // Robot State
    private volatile float lowDist, highDist, gyro, irDist;
    private volatile boolean isMoving;
    private volatile Pose currentPose = new Pose(0,0,0);
    private volatile boolean connected = false;
    
    // Logic
    private volatile boolean matchRunning = false;
    private volatile boolean cycleRunning = false;
    private final Object startCycleLock = new Object();
    private Thread logicThread;
    
    // Manual Control
    private boolean manualMode = false;
    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean aPressed = false;
    private boolean dPressed = false;
    
    // Game Constants
    private static final float FIELD_WIDTH = 143.0f;
    private static final float FIELD_HEIGHT = 115.0f;
    private static final float GOAL_Y = 57.5f;
    private static final float BLUE_START_Y = 20.0f;
    private static final float GREEN_START_Y = 95.0f;
    private static final float BLUE_START_X = 20.0f;
    private static final float GREEN_START_X = 123.0f;
    
    private static final Waypoint BLUE_GOAL = new Waypoint(15.0f, GOAL_Y); 
    private static final Waypoint GREEN_GOAL = new Waypoint(128.0f, GOAL_Y);
    
    private MapPanel mapPanel; // Visual Debugger
    
    // Vision
    private VisionService visionService;
    private JLabel cameraLabel;
    private JTextField txtCameraURL;
    private JButton btnConnectCamera;
    
    private boolean isBlueTeam = true;
    private boolean isAttacker = true;
    
    // Buttons
    private JButton btnStartMatch;
    private JButton btnStartCycle;
    private JButton btnEndCycle;
    private JButton btnPenalty;

    public ServerGUI() {
        setupUI();
        startServer();
        
        // Update Camera UI
        new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (visionService != null && visionService.getCurrentFrame() != null) {
                    cameraLabel.setIcon(new ImageIcon(visionService.getCurrentFrame()));
                    cameraLabel.setText("");
                }
            }
        }).start();
    }
    
    private void setupUI() {
        setTitle("Robot Brain (Server) - Simplified");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Top Panel: Status & Setup
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        
        statusLabel = new JLabel("Status: Waiting for Robot...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(statusLabel);
        
        JPanel setupPanel = new JPanel(new FlowLayout());
        rbBlue = new JRadioButton("Blue", true);
        rbGreen = new JRadioButton("Green");
        ButtonGroup bgTeam = new ButtonGroup(); bgTeam.add(rbBlue); bgTeam.add(rbGreen);
        
        rbAttacker = new JRadioButton("Attacker", true);
        rbDefender = new JRadioButton("Defender");
        ButtonGroup bgRole = new ButtonGroup(); bgRole.add(rbAttacker); bgRole.add(rbDefender);
        
        setupPanel.add(new JLabel("Team:"));
        setupPanel.add(rbBlue);
        setupPanel.add(rbGreen);
        setupPanel.add(new JLabel("Role:"));
        setupPanel.add(rbAttacker);
        setupPanel.add(rbDefender);
        
        topPanel.add(setupPanel);
        add(topPanel, BorderLayout.NORTH);
        
        sensorLabel = new JLabel("Sensors: --");
        sensorLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        sensorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Map Visualization
        mapPanel = new MapPanel();
        
        // Camera View
        cameraLabel = new JLabel("Waiting for Camera...", SwingConstants.CENTER);
        JPanel cameraPanel = new JPanel(new BorderLayout());
        cameraPanel.setBorder(BorderFactory.createTitledBorder("Camera View"));
        cameraPanel.add(new JScrollPane(cameraLabel), BorderLayout.CENTER);
        cameraPanel.setPreferredSize(new Dimension(320, 240));
        
        // Camera Controls
        JPanel camControlPanel = new JPanel(new BorderLayout());
        txtCameraURL = new JTextField("http://10.88.36.132:4747/video");
        btnConnectCamera = new JButton("Connect");
        
        btnConnectCamera.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connectCamera();
            }
        });
        
        camControlPanel.add(txtCameraURL, BorderLayout.CENTER);
        camControlPanel.add(btnConnectCamera, BorderLayout.EAST);
        
        cameraPanel.add(camControlPanel, BorderLayout.SOUTH);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, cameraPanel);
        centerSplit.setResizeWeight(0.7);
        add(centerSplit, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(sensorLabel, BorderLayout.NORTH);
        
        logArea = new JTextArea(5, 40);
        logArea.setEditable(false);
        bottomPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Right Panel: Controls
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Match Controls
        btnStartMatch = new JButton("Start Match (5m)");
        btnStartMatch.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStartMatch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleMatch();
            }
        });
        btnPanel.add(btnStartMatch);
        btnPanel.add(Box.createVerticalStrut(10));

        btnStartCycle = new JButton("Start Cycle");
        btnStartCycle.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStartCycle.setEnabled(false);
        btnStartCycle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startCycle();
            }
        });
        btnPanel.add(btnStartCycle);
        btnPanel.add(Box.createVerticalStrut(5));

        btnEndCycle = new JButton("End Cycle / Goal");
        btnEndCycle.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEndCycle.setEnabled(false);
        btnEndCycle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                endCycle();
            }
        });
        btnPanel.add(btnEndCycle);
        btnPanel.add(Box.createVerticalStrut(5));
        
        btnPenalty = new JButton("Penalty Reset");
        btnPenalty.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPenalty.setEnabled(false);
        btnPenalty.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                penaltyReset();
            }
        });
        btnPanel.add(btnPenalty);
        btnPanel.add(Box.createVerticalStrut(20));

        // Manual Control Section
        final JToggleButton manualToggle = new JToggleButton("Manual Control (WASD)");
        manualToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        manualToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleManualMode(manualToggle.isSelected());
            }
        });
        btnPanel.add(manualToggle);
        btnPanel.add(Box.createVerticalStrut(10));
        
        // Action Buttons
        JPanel actionGrid = new JPanel(new GridLayout(2, 1, 5, 5));
        JButton btnKick = new JButton("KICK (K)");
        btnKick.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { sendCommand("KICK"); }});
        
        JButton btnArm = new JButton("ARM (U/J)");
        btnArm.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { sendCommand("ARM_UP"); }});
        
        actionGrid.add(btnKick);
        actionGrid.add(btnArm);
        actionGrid.setMaximumSize(new Dimension(150, 80));
        
        btnPanel.add(Box.createVerticalStrut(10));
        btnPanel.add(actionGrid);

        add(btnPanel, BorderLayout.EAST);
        
        // Add Key Listener
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (!manualMode) return;
                handleManualKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!manualMode) return;
                handleManualKey(e.getKeyCode(), false);
            }
        });
        setFocusable(true);
        
        // Add Mouse Listener for Click-to-Move (Simplified)
        mapPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (manualMode) {
                    log("Click-to-move disabled in simplified mode.");
                }
            }
        });
    }
    
    private void startServer() {
        new Thread(new Runnable() {
            public void run() {
                log("System Info:");
                log("OS Arch: " + System.getProperty("os.arch"));
                log("Java Lib Path: " + System.getProperty("java.library.path"));
                File f = new File("opencv_ffmpeg2413_64.dll");
                log("FFmpeg DLL exists: " + f.exists() + " at " + f.getAbsolutePath());
                
                try {
                    serverSocket = new ServerSocket(12345);
                    log("Server started on port 12345");
                    
                    while (true) {
                        clientSocket = serverSocket.accept();
                        connected = true;
                        updateStatus("Connected to Robot: " + clientSocket.getInetAddress());
                        
                        out = new PrintWriter(clientSocket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        
                        // Start Receiver
                        handleClient();
                    }
                } catch (IOException e) {
                    log("Server Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    private void handleClient() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("DATA:")) {
                    parseData(line.substring(5));
                }
            }
        } catch (IOException e) {
            log("Client Disconnected");
        } finally {
            connected = false;
            updateStatus("Waiting for Robot...");
        }
    }
    
    private void parseData(String data) {
        try {
            String[] parts = data.split(",");
            lowDist = Float.parseFloat(parts[0]);
            highDist = Float.parseFloat(parts[1]);
            gyro = Float.parseFloat(parts[2]);
            irDist = Float.parseFloat(parts[3]);
            isMoving = Boolean.parseBoolean(parts[4]);
            
            if (parts.length > 7) {
                float x = Float.parseFloat(parts[5]);
                float y = Float.parseFloat(parts[6]);
                float h = Float.parseFloat(parts[7]);
                currentPose = new Pose(x, y, h);
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sensorLabel.setText(String.format("<html>Low: %.1f cm<br>High: %.1f cm<br>Gyro: %.1f deg<br>IR: %.1f cm<br>Moving: %b<br>Pose: (%.1f, %.1f, %.1f)</html>", 
                        lowDist, highDist, gyro, irDist, isMoving, currentPose.getX(), currentPose.getY(), currentPose.getHeading()));
                    if (mapPanel != null) mapPanel.repaint();
                }
            });
        } catch (Exception e) {}
    }
    
    private void toggleMatch() {
        if (matchRunning) {
            // Stop Match
            matchRunning = false;
            cycleRunning = false;
            synchronized(startCycleLock) {
                startCycleLock.notifyAll();
            }
            btnStartMatch.setText("Start Match (5m)");
            btnStartCycle.setEnabled(false);
            btnEndCycle.setEnabled(false);
            btnPenalty.setEnabled(false);
            sendCommand("STOP");
            log("Match Stopped.");
        } else {
            if (manualMode) {
                log("Cannot start match in Manual Mode.");
                return;
            }
            matchRunning = true;
            btnStartMatch.setText("Stop Match");
            btnStartCycle.setEnabled(true);
            btnPenalty.setEnabled(true);
            
            // Update Settings
            isBlueTeam = rbBlue.isSelected();
            isAttacker = rbAttacker.isSelected();
            
            startLogic();
        }
    }
    
    private void startCycle() {
        if (!matchRunning) return;
        if (cycleRunning) {
            log("Cycle already running.");
            return;
        }
        
        log("Starting Cycle...");
        cycleRunning = true;
        btnStartCycle.setEnabled(false);
        btnEndCycle.setEnabled(true);
        
        synchronized(startCycleLock) {
            startCycleLock.notifyAll();
        }
    }
    
    private void endCycle() {
        if (!cycleRunning) return;
        log("Ending Cycle (Goal or Manual Stop)...");
        cycleRunning = false;
        btnStartCycle.setEnabled(true);
        btnEndCycle.setEnabled(false);
        sendCommand("STOP");
    }
    
    private void penaltyReset() {
        log("Penalty! Resetting to Initial State...");
        cycleRunning = false;
        btnStartCycle.setEnabled(true);
        btnEndCycle.setEnabled(false);
        sendCommand("STOP");
        // Logic thread will loop back to wait for start signal
    }

    private void toggleManualMode(boolean enabled) {
        manualMode = enabled;
        if (manualMode) {
            if (matchRunning) toggleMatch(); // Stop game if running
            log("Manual Control Enabled. Use WASD keys.");
            requestFocus(); // Important for KeyListener
        } else {
            log("Manual Control Disabled.");
            sendCommand("STOP");
        }
    }

    private void handleManualKey(int keyCode, boolean pressed) {
        if (pressed) {
            if (keyCode == KeyEvent.VK_W && !wPressed) { wPressed = true; sendCommand("FORWARD"); }
            if (keyCode == KeyEvent.VK_S && !sPressed) { sPressed = true; sendCommand("BACKWARD"); }
            if (keyCode == KeyEvent.VK_A && !aPressed) { aPressed = true; sendCommand("LEFT"); }
            if (keyCode == KeyEvent.VK_D && !dPressed) { dPressed = true; sendCommand("RIGHT"); }
            if (keyCode == KeyEvent.VK_SPACE) { sendCommand("STOP"); }
            if (keyCode == KeyEvent.VK_K) { sendCommand("KICK"); }
            if (keyCode == KeyEvent.VK_U) { sendCommand("ARM_UP"); }
            if (keyCode == KeyEvent.VK_J) { sendCommand("ARM_DOWN"); }
        } else {
            if (keyCode == KeyEvent.VK_W) wPressed = false;
            if (keyCode == KeyEvent.VK_S) sPressed = false;
            if (keyCode == KeyEvent.VK_A) aPressed = false;
            if (keyCode == KeyEvent.VK_D) dPressed = false;
            
            // If all movement keys released, stop (optional)
            if (!wPressed && !sPressed && !aPressed && !dPressed) {
                // sendCommand("STOP"); 
            }
        }
    }
    
    private void startLogic() {
        logicThread = new Thread(new Runnable() {
            public void run() {
                log("Match Started. Waiting for Start Cycle signal...");
                
                // Initialize Pose
                if (isBlueTeam) {
                    sendCommand("SET_POSE " + BLUE_START_X + " " + BLUE_START_Y + " 0");
                } else {
                    sendCommand("SET_POSE " + GREEN_START_X + " " + GREEN_START_Y + " 180");
                }
                sleep(500); // Wait for pose update
                
                sendCommand("ARM_DOWN"); // Reset arm
                
                long matchEndTime = System.currentTimeMillis() + 5 * 60 * 1000;
                
                while (matchRunning && connected) {
                    if (System.currentTimeMillis() > matchEndTime) {
                        log("Match Time Over!");
                        SwingUtilities.invokeLater(new Runnable() { public void run() { toggleMatch(); }});
                        break;
                    }
                    
                    updateStatus("Initial State: Waiting...");
                    
                    // Wait for Start Cycle Signal
                    synchronized(startCycleLock) {
                        while (!cycleRunning && matchRunning) {
                            try { startCycleLock.wait(); } catch (InterruptedException e) {}
                        }
                    }
                    
                    if (!matchRunning) break;
                    
                    updateStatus("Cycle Started!");
                    
                    if (isAttacker) {
                        runOffense();
                    } else {
                        log("Defender: Waiting 3 seconds...");
                        sleep(3000);
                        if (cycleRunning) runDefense();
                    }
                    
                    // Cycle Ended
                    log("Cycle Ended. Returning to Initial State.");
                    cycleRunning = false;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            btnStartCycle.setEnabled(true);
                            btnEndCycle.setEnabled(false);
                        }
                    });
                }
                log("Logic Thread Stopped");
            }
        });
        logicThread.start();
    }
    
    // --- Simplified Game Logic ---
    
    private void runOffense() {
        log("OFFENSE: Simple Mode");
        
        // 1. Find Ball
        if (!searchForBall()) return;
        
        // 2. Approach Ball (Visual Servoing + US Distance)
        if (!approachBall()) return;
        
        // 3. Capture Ball
        captureBallSequence();
        
        // 4. Deliver (Blind navigation to goal coordinates)
        deliverToGoal();
    }

    private void runDefense() {
        log("DEFENSE STATE");
        // Simple defense: Scan and intercept
        while (cycleRunning && matchRunning) {
            // Defense logic here
            // For now, just wait
            sleep(1000);
        }
    }
    
    private boolean searchForBall() {
        log("Searching for ball...");
        long startTime = System.currentTimeMillis();
        
        // Increased timeout to 30 seconds
        while (cycleRunning && System.currentTimeMillis() - startTime < 30000) {
            if (visionService.getBallPosition() != null) {
                log("Ball Found (Vision)!");
                return true;
            }
            
            // Spin
            sendCommand("ROTATE 45"); // Faster spin
            waitForMove();
            sleep(200); // Wait for camera
        }
        
        log("Search timed out.");
        return false;
    }
    
    private boolean approachBall() {
        log("Approaching Ball...");
        final int CENTER_X = 320;
        final int DEAD_ZONE = 40;
        long startTime = System.currentTimeMillis();
        
        // Added 60s timeout
        while (cycleRunning && System.currentTimeMillis() - startTime < 60000) {
            // Check Distance (Lower US)
            float dist = lowDist;
            
            // Capture Condition
            if (dist > 0 && dist <= 20) {
                log("Ball in Capture Range (" + dist + "cm).");
                sendCommand("STOP");
                return true;
            }
            
            org.opencv.core.Point ballPos = visionService.getBallPosition();
            
            if (ballPos == null) {
                // Blind Tracking if close
                if (dist > 0 && dist < 50) {
                    log("Blind Tracking (US: " + dist + "cm)");
                    sendCommand("TRAVEL 10");
                    waitForMove();
                    continue;
                } else {
                    log("Lost Ball.");
                    sendCommand("STOP");
                    return false;
                }
            }
            
            // Visual Servoing
            double x = ballPos.x;
            double error = x - CENTER_X;
            
            if (Math.abs(error) > DEAD_ZONE) {
                // Turn
                if (error > 0) sendCommand("ROTATE -10");
                else sendCommand("ROTATE 10");
                waitForMove();
            } else {
                // Forward
                if (dist > 50 || dist == 0) {
                    sendCommand("TRAVEL 20");
                } else {
                    sendCommand("TRAVEL 10");
                }
                waitForMove();
            }
            sleep(50);
        }
        
        log("Approach timed out.");
        return false;
    }
    
    private void captureBallSequence() {
        log("Executing Capture Sequence...");
        if (!cycleRunning) return;
        
        // 1. Lift Arm
        sendCommand("ARM_UP");
        // Wait for arm to go up (mechanical time)
        sleep(1000); 
        
        // 2. Drive Over (30cm)
        // The arm stays UP during this move because we haven't sent ARM_DOWN yet
        sendCommand("TRAVEL 30");
        waitForMove(); // Now robustly waits for the move to complete
        
        // 3. Trap
        sleep(500); // Brief pause
        sendCommand("ARM_DOWN");
        sleep(1000); // Wait for arm to lower
        
        // 4. Verify
        if (irDist < 18) {
            log("Possession Confirmed (IR: " + irDist + "cm).");
        } else {
            log("Warning: Capture may have failed (IR: " + irDist + "cm).");
        }
    }
    
    private void deliverToGoal() {
        log("Delivering to Goal...");
        if (!cycleRunning) return;
        
        Waypoint goal = isBlueTeam ? GREEN_GOAL : BLUE_GOAL;
        
        // Calculate Angle to Goal
        float dx = goal.x - currentPose.getX();
        float dy = goal.y - currentPose.getY();
        float targetAngle = (float)Math.toDegrees(Math.atan2(dy, dx));
        
        // Turn
        float turn = targetAngle - currentPose.getHeading();
        while (turn > 180) turn -= 360;
        while (turn < -180) turn += 360;
        
        log("Turning " + (int)turn + " deg to goal.");
        sendCommand("ROTATE " + turn);
        waitForMove();
        
        // Drive
        float distance = (float)Math.sqrt(dx*dx + dy*dy);
        log("Driving " + (int)distance + " cm to goal.");
        sendCommand("TRAVEL " + distance);
        waitForMove();
        
        // Sit on goal and beep
        log("Arrived at Goal. Beeping...");
        sendCommand("STOP");
        for (int i = 0; i < 3; i++) {
            sendCommand("BEEP");
            sleep(500);
        }
        
        endCycle();
    }
    
    private void waitForMove() {
        // Wait for movement to start (up to 2s) to ensure we don't skip the wait
        long startWait = System.currentTimeMillis();
        while (!isMoving && connected && cycleRunning && (System.currentTimeMillis() - startWait < 2000)) {
            sleep(50);
        }
        
        // Wait for movement to stop
        while (isMoving && connected && cycleRunning) {
            sleep(50);
        }
        sleep(100); // Settle
    }
    
    private void sendCommand(String cmd) {
        if (out != null) {
            out.println(cmd);
            log("Sent: " + cmd);
        }
    }
    
    private void log(String msg) {
        final String finalMsg = msg;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logArea.append(finalMsg + "\n");
            }
        });
    }
    
    private void updateStatus(String msg) {
        final String finalMsg = msg;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusLabel.setText(finalMsg);
            }
        });
    }
    
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }

    public static void main(String[] args) {
        // Load the OpenCV native library from the environment (java.library.path)
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        SwingUtilities.invokeLater(new Runnable() {
            public void actionPerformed(ActionEvent e) {
                new ServerGUI().setVisible(true);
            }
            public void run() {
                new ServerGUI().setVisible(true);
            }
        });
    }

    private class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate Bounds of all objects
            float minX = 0, maxX = FIELD_WIDTH;
            float minY = 0, maxY = FIELD_HEIGHT;
            
            if (currentPose != null) {
                minX = Math.min(minX, currentPose.getX());
                maxX = Math.max(maxX, currentPose.getX());
                minY = Math.min(minY, currentPose.getY());
                maxY = Math.max(maxY, currentPose.getY());
            }
            
            // Add padding
            float padding = 10.0f;
            minX -= padding; maxX += padding;
            minY -= padding; maxY += padding;
            
            float contentWidth = maxX - minX;
            float contentHeight = maxY - minY;

            // Calculate Scale to fit content
            float scale = Math.min(getWidth() / contentWidth, getHeight() / contentHeight);
            
            // Calculate Offset to center content
            int xMargin = (int) ((getWidth() - contentWidth * scale) / 2);
            int yMargin = (int) ((getHeight() - contentHeight * scale) / 2);

            // Draw Field Boundary
            g2.setColor(Color.LIGHT_GRAY);
            drawRect(g2, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, minX, minY, maxY, scale, xMargin, yMargin);
            g2.setColor(Color.BLACK);
            drawRectOutline(g2, 0, 0, FIELD_WIDTH, FIELD_HEIGHT, minX, minY, maxY, scale, xMargin, yMargin);

            // Draw Robot
            if (currentPose != null) {
                int rx = toScreenX(currentPose.getX(), minX, scale, xMargin);
                int ry = toScreenY(currentPose.getY(), minY, maxY, scale, yMargin);
                int r = 10;
                g2.setColor(Color.BLUE);
                g2.fillOval(rx - r, ry - r, 2 * r, 2 * r);
                
                float hx = currentPose.getX() + 20 * (float)Math.cos(Math.toRadians(currentPose.getHeading()));
                float hy = currentPose.getY() + 20 * (float)Math.sin(Math.toRadians(currentPose.getHeading()));
                
                drawLine(g2, currentPose.getX(), currentPose.getY(), hx, hy, minX, minY, maxY, scale, xMargin, yMargin);
            }
        }
        
        private int toScreenX(float worldX, float minX, float scale, int xMargin) {
            return xMargin + (int)((worldX - minX) * scale);
        }
        
        private int toScreenY(float worldY, float minY, float maxY, float scale, int yMargin) {
            // Flip Y: maxY is at top (yMargin), minY is at bottom
            return yMargin + (int)((maxY - worldY) * scale);
        }
        
        private void drawLine(Graphics2D g, float x1, float y1, float x2, float y2, float minX, float minY, float maxY, float scale, int xMargin, int yMargin) {
            g.drawLine(toScreenX(x1, minX, scale, xMargin), toScreenY(y1, minY, maxY, scale, yMargin),
                       toScreenX(x2, minX, scale, xMargin), toScreenY(y2, minY, maxY, scale, yMargin));
        }
        
        private void drawRect(Graphics2D g, float x, float y, float w, float h, float minX, float minY, float maxY, float scale, int xMargin, int yMargin) {
            int sx = toScreenX(x, minX, scale, xMargin);
            int sy = toScreenY(y + h, minY, maxY, scale, yMargin); // Top Left in Screen Coords (since Y is flipped, y+h is top)
            int sw = (int)(w * scale);
            int sh = (int)(h * scale);
            g.fillRect(sx, sy, sw, sh);
        }
        
        private void drawRectOutline(Graphics2D g, float x, float y, float w, float h, float minX, float minY, float maxY, float scale, int xMargin, int yMargin) {
            int sx = toScreenX(x, minX, scale, xMargin);
            int sy = toScreenY(y + h, minY, maxY, scale, yMargin);
            int sw = (int)(w * scale);
            int sh = (int)(h * scale);
            g.drawRect(sx, sy, sw, sh);
        }
    }

    private void connectCamera() {
        String url = txtCameraURL.getText();
        if (visionService != null) {
            visionService.stop();
            try { Thread.sleep(500); } catch (Exception e) {}
        }
        
        if (url.isEmpty() || url.equals("0")) {
             visionService = new VisionService(0);
        } else {
             visionService = new VisionService(url);
        }
        
        new Thread(visionService).start();
    }
}