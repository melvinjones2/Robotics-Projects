package server.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

import server.autonomous.ServerAutonomousController;
import server.logging.LogManager;

// GUI for EV3 Robot Server with control panel, sensor monitoring, and autonomous features
public class ServerGUI {

    private JTextArea logArea;
    private JTextField commandField;
    private JButton sendButton;
    private volatile boolean debugMode = false;
    private ServerAutonomousController autonomousController;
    private JLabel sensorStatusLabel;
    private JLabel threatLevelLabel;
    private JButton autoEnableButton;
    private JPanel sensorDataPanel;
    private JLabel ultrasonicLabel;
    private JLabel gyroLabel;
    private JLabel infraredLabel;
    private JLabel touchLabel;
    private JLabel colorLabel;
    
    private long lastCommandTime = 0;
    private static final long MIN_COMMAND_INTERVAL_MS = 200;

    public void setupMainWindow(final BufferedWriter out, final AtomicInteger frameCount, final AtomicBoolean running) {
        setupMainWindow(out, frameCount, running, null);
    }
    
    public void setupMainWindow(final BufferedWriter out, final AtomicInteger frameCount, 
                               final AtomicBoolean running, ServerAutonomousController autoController) {
        this.autonomousController = autoController;
        final JFrame mainFrame = new JFrame("EV3 Server");
        mainFrame.setLayout(new BorderLayout());

        logArea = new JTextArea(25, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JCheckBox debugBox = new JCheckBox("Debug Mode");
        debugBox.setSelected(debugMode);
        debugBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                debugMode = debugBox.isSelected();
                if (sensorDataPanel != null) {
                    sensorDataPanel.setVisible(debugMode);
                }
                if (out != null) {
                    try {
                        send(out, "SET_DEBUG:" + (debugMode ? "1" : "0"));
                    } catch (IOException ex) {
                        LogManager.log("Send error: " + ex.getMessage());
                    }
                }
            }
        });
        topPanel.add(debugBox);
        
        sensorDataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sensorDataPanel.setBorder(BorderFactory.createTitledBorder("Sensor Data (Debug Mode)"));
        sensorDataPanel.setVisible(false);
        
        ultrasonicLabel = new JLabel("Ultrasonic: --");
        gyroLabel = new JLabel("Gyro: --");
        infraredLabel = new JLabel("Infrared: --");
        touchLabel = new JLabel("Touch: --");
        colorLabel = new JLabel("Color: --");
        
        sensorDataPanel.add(ultrasonicLabel);
        sensorDataPanel.add(new JLabel(" | "));
        sensorDataPanel.add(gyroLabel);
        sensorDataPanel.add(new JLabel(" | "));
        sensorDataPanel.add(infraredLabel);
        sensorDataPanel.add(new JLabel(" | "));
        sensorDataPanel.add(touchLabel);
        sensorDataPanel.add(new JLabel(" | "));
        sensorDataPanel.add(colorLabel);
        
        topPanel.add(sensorDataPanel);

        // Command panels organized by category
        JPanel commandsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        
        // Movement Commands Panel
        JPanel movementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        movementPanel.setBorder(BorderFactory.createTitledBorder("Movement Commands"));
        movementPanel.add(createCommandButton("Forward Slow", "MOVE 150", out, frameCount));
        movementPanel.add(createCommandButton("Forward Med", "MOVE 300", out, frameCount));
        movementPanel.add(createCommandButton("Forward Fast", "MOVE 500", out, frameCount));
        movementPanel.add(createCommandButton("Backward Slow", "BWD 150", out, frameCount));
        movementPanel.add(createCommandButton("Backward Med", "BWD 300", out, frameCount));
        movementPanel.add(createCommandButton("Backward Fast", "BWD 500", out, frameCount));
        commandsPanel.add(movementPanel);
        
        JPanel turnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        turnPanel.setBorder(BorderFactory.createTitledBorder("Turn & Stop"));
        turnPanel.add(createCommandButton("Turn Left", "LEFT 200", out, frameCount));
        turnPanel.add(createCommandButton("Turn Right", "RIGHT 200", out, frameCount));
        JButton stopButton = createCommandButton("Stop All", "STOP", out, frameCount);
        stopButton.setBackground(new Color(255, 230, 150));
        turnPanel.add(stopButton);
        JButton emergencyButton = createCommandButton("EMERGENCY STOP", "STOP EMERGENCY", out, frameCount);
        emergencyButton.setBackground(new Color(255, 100, 100));
        emergencyButton.setForeground(Color.WHITE);
        emergencyButton.setFont(emergencyButton.getFont().deriveFont(Font.BOLD));
        turnPanel.add(emergencyButton);
        commandsPanel.add(turnPanel);
        
        JPanel motorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        motorPanel.setBorder(BorderFactory.createTitledBorder("Individual Motor Control"));
        motorPanel.add(createCommandButton("Motor A Fwd", "MOVE A 200", out, frameCount));
        motorPanel.add(createCommandButton("Motor B Fwd", "MOVE B 200", out, frameCount));
        motorPanel.add(createCommandButton("Motor C Fwd", "MOVE C 200", out, frameCount));
        motorPanel.add(createCommandButton("Motor D Fwd", "MOVE D 200", out, frameCount));
        motorPanel.add(createCommandButton("Stop A", "STOP A", out, frameCount));
        motorPanel.add(createCommandButton("Stop B", "STOP B", out, frameCount));
        motorPanel.add(createCommandButton("Stop C", "STOP C", out, frameCount));
        motorPanel.add(createCommandButton("Stop D", "STOP D", out, frameCount));
        commandsPanel.add(motorPanel);
        
        JPanel rotatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rotatePanel.setBorder(BorderFactory.createTitledBorder("Rotation Control"));
        rotatePanel.add(createCommandButton("Rotate Left 90°", "ROTATE ROBOT -90", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate Right 90°", "ROTATE ROBOT 90", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate 180°", "ROTATE ROBOT 180", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate 360°", "ROTATE ROBOT 360", out, frameCount));
        commandsPanel.add(rotatePanel);
        
        JPanel armPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        armPanel.setBorder(BorderFactory.createTitledBorder("Arm Control"));
        armPanel.add(createCommandButton("Arm Up", "ARM UP", out, frameCount));
        armPanel.add(createCommandButton("Arm Down", "ARM DOWN", out, frameCount));
        armPanel.add(createCommandButton("Arm -45°", "ARM -45", out, frameCount));
        armPanel.add(createCommandButton("Arm -90°", "ARM -90", out, frameCount));
        armPanel.add(createCommandButton("Arm 0°", "ARM 0", out, frameCount));
        commandsPanel.add(armPanel);
        
        // Course Materials Navigation Commands
        JPanel courseNavPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        courseNavPanel.setBorder(BorderFactory.createTitledBorder("Course Navigation (PowerPoint Examples)"));
        courseNavPanel.add(createCommandButton("Square 20cm", "NAVSQUARE 20", out, frameCount));
        courseNavPanel.add(createCommandButton("Square 30cm", "NAVSQUARE 30", out, frameCount));
        courseNavPanel.add(createCommandButton("Rotate 90°", "NAVROTATE 90", out, frameCount));
        courseNavPanel.add(createCommandButton("Rotate -90°", "NAVROTATE -90", out, frameCount));
        courseNavPanel.add(createCommandButton("Travel 30cm", "NAVTRAVEL 30", out, frameCount));
        courseNavPanel.add(createCommandButton("Travel -30cm", "NAVTRAVEL -30", out, frameCount));
        commandsPanel.add(courseNavPanel);
        
        // Mapping Commands
        JPanel mappingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mappingPanel.setBorder(BorderFactory.createTitledBorder("Mapping Strategies"));
        mappingPanel.add(createCommandButton("LineMap Test", "NAVLINEMAP 10 40 70 20", out, frameCount));
        mappingPanel.add(createCommandButton("Grid Map Scan", "NAVGRIDMAP 50 30", out, frameCount));
        mappingPanel.add(createCommandButton("Grid Map 100cm", "NAVGRIDMAP 100 15", out, frameCount));
        mappingPanel.add(createCommandButton("NavBall Strategy", "NAVBALL 5", out, frameCount));
        commandsPanel.add(mappingPanel);
        
        // Custom Map Navigation
        JPanel customMapPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customMapPanel.setBorder(BorderFactory.createTitledBorder("Custom Board Navigation"));
        customMapPanel.add(createCommandButton("Load Custom Map", "LOADCUSTOMMAP", out, frameCount));
        customMapPanel.add(createCommandButton("Goal 1 → Goal 2", "NAVCUSTOM 38.1 67.3 0 71.1 83.8", out, frameCount));
        customMapPanel.add(createCommandButton("Goal 2 → Goal 1", "NAVCUSTOM 71.1 83.8 180 38.1 67.3", out, frameCount));
        JButton customNavButton = createCommandButton("Custom Nav...", "NAVCUSTOM", out, frameCount);
        customNavButton.setBackground(new Color(200, 200, 255));
        customMapPanel.add(customNavButton);
        commandsPanel.add(customMapPanel);
        
        JPanel sensorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sensorPanel.setBorder(BorderFactory.createTitledBorder("Sensors & Battery"));
        sensorPanel.add(createCommandButton("Get Battery", "GET_BATTERY", out, frameCount));
        sensorPanel.add(createTestButton("Battery Log Test", out, frameCount));
        commandsPanel.add(sensorPanel);
        
        JPanel systemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        systemPanel.setBorder(BorderFactory.createTitledBorder("System Commands"));
        systemPanel.add(createCommandButton("Beep", "BEEP", out, frameCount));
        systemPanel.add(createCommandButton("Beep x3", "BEEP 3", out, frameCount));
        systemPanel.add(createCommandButton("Log Message", "LOG Test message from server", out, frameCount));
        JButton byeButton = createCommandButton("Disconnect", "BYE", out, frameCount);
        byeButton.setBackground(new Color(255, 200, 200));
        systemPanel.add(byeButton);
        commandsPanel.add(systemPanel);
        
        if (autonomousController != null) {
            JPanel autoPanel = createAutonomousPanel(out, frameCount);
            commandsPanel.add(autoPanel);
        }
        
        JPanel manualPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        manualPanel.setBorder(BorderFactory.createTitledBorder("Manual Command"));
        commandField = new JTextField(40);
        sendButton = new JButton("Send Command");
        manualPanel.add(new JLabel("Command:"));
        manualPanel.add(commandField);
        manualPanel.add(sendButton);
        commandsPanel.add(manualPanel);

        mainFrame.add(topPanel, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.add(commandsPanel, BorderLayout.SOUTH);

        mainFrame.setSize(1200, 700);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setLocation(100, 100);
        mainFrame.setVisible(true);

        @SuppressWarnings("Convert2Lambda")
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = commandField.getText().trim();
                if (!line.isEmpty() && out != null) {
                    try {
                        send(out, line);
                        if ("BYE".equalsIgnoreCase(line)) {
                            running.set(false);
                            mainFrame.dispose();
                        }
                    } catch (IOException ex) {
                        LogManager.log("Send error: " + ex.getMessage());
                    }
                    commandField.setText("");
                }
            }
        };
        sendButton.addActionListener(sendAction);
        commandField.addActionListener(sendAction);

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running.set(false);
                mainFrame.dispose();
            }
        });
    }
    
    private JPanel createAutonomousPanel(final BufferedWriter out, final AtomicInteger frameCount) {
        JPanel autoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoPanel.setBorder(BorderFactory.createTitledBorder("Server Autonomous Control (SERVER Priority)"));
        
        // Enable/Disable autonomous mode
        autoEnableButton = new JButton("Enable Server Auto");
        autoEnableButton.setBackground(new Color(150, 255, 150));
        autoEnableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autonomousController != null) {
                    boolean newState = !autonomousController.isEnabled();
                    autonomousController.setEnabled(newState);
                    autoEnableButton.setText(newState ? "Disable Server Auto" : "Enable Server Auto");
                    autoEnableButton.setBackground(newState ? new Color(255, 150, 150) : new Color(150, 255, 150));
                    appendLog("Server autonomous mode: " + (newState ? "ENABLED" : "DISABLED"), false);
                }
            }
        });
        autoPanel.add(autoEnableButton);
        
        // Sensor status display
        sensorStatusLabel = new JLabel("Sensors: No data");
        autoPanel.add(sensorStatusLabel);
        
        // Threat level display
        threatLevelLabel = new JLabel("Threat: NONE");
        threatLevelLabel.setFont(threatLevelLabel.getFont().deriveFont(Font.BOLD));
        autoPanel.add(threatLevelLabel);
        
        // Get suggestion button
        JButton suggestButton = new JButton("Get Suggestion");
        suggestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autonomousController != null) {
                    String suggestion = autonomousController.analyzeAndSuggest();
                    if (suggestion != null) {
                        appendLog("[SERVER AUTO] Suggestion: " + suggestion, false);
                    } else {
                        appendLog("[SERVER AUTO] No action suggested", false);
                    }
                }
            }
        });
        autoPanel.add(suggestButton);
        
        // Execute suggestion button
        JButton executeButton = new JButton("Execute Suggestion");
        executeButton.setBackground(new Color(255, 200, 100));
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autonomousController != null && out != null) {
                    String suggestion = autonomousController.analyzeAndSuggest();
                    if (suggestion != null) {
                        try {
                            // Send command WITHOUT frame number - client doesn't expect it
                            send(out, suggestion);
                            appendLog("[SERVER AUTO] Executed: " + suggestion, false);
                        } catch (IOException ex) {
                            appendLog("Error executing suggestion: " + ex.getMessage(), false);
                        }
                    } else {
                        appendLog("[SERVER AUTO] No action to execute", false);
                    }
                }
            }
        });
        autoPanel.add(executeButton);
        
        // Show sensor summary button
        JButton summaryButton = new JButton("Sensor Summary");
        summaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autonomousController != null) {
                    String summary = autonomousController.getSensorSummary();
                    appendLog(summary, false);
                }
            }
        });
        autoPanel.add(summaryButton);
        
        // Scan for ball button (client-side autonomous - single scan)
        JButton scanBallButton = new JButton("Scan for Ball");
        scanBallButton.setBackground(new Color(100, 200, 255));
        scanBallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    try {
                        send(out, "SCAN");
                        appendLog("[CLIENT SCAN] Ball scan initiated", false);
                    } catch (IOException ex) {
                        appendLog("Error sending SCAN command: " + ex.getMessage(), false);
                    }
                } else {
                    appendLog("No client connected", false);
                }
            }
        });
        autoPanel.add(scanBallButton);
        
        // Auto search button (client-side autonomous - continuous exploration)
        final JButton autoSearchButton = new JButton("Auto Search: OFF");
        autoSearchButton.setBackground(new Color(150, 255, 150));
        autoSearchButton.addActionListener(new ActionListener() {
            private boolean searchEnabled = false;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    try {
                        searchEnabled = !searchEnabled;
                        send(out, "AUTOSEARCH " + (searchEnabled ? "ON" : "OFF"));
                        autoSearchButton.setText("Auto Search: " + (searchEnabled ? "ON" : "OFF"));
                        autoSearchButton.setBackground(searchEnabled ? 
                            new Color(255, 150, 150) : new Color(150, 255, 150));
                        appendLog("[CLIENT AUTOSEARCH] " + (searchEnabled ? "Started" : "Stopped"), false);
                    } catch (IOException ex) {
                        appendLog("Error sending AUTOSEARCH command: " + ex.getMessage(), false);
                    }
                } else {
                    appendLog("No client connected", false);
                }
            }
        });
        autoPanel.add(autoSearchButton);
        
        // Color selection for ball detection
        autoPanel.add(new JLabel("Target Ball:"));
        final JComboBox<String> colorCombo = new JComboBox<String>(
            new String[]{"Black", "Blue", "Green", "Yellow", "Red", "White"}
        );
        colorCombo.setSelectedIndex(4); // Default to Red (index 4, colorId 5)
        colorCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    int colorId = colorCombo.getSelectedIndex() + 1; // 1=Black, 2=Blue, etc.
                    String colorName = (String) colorCombo.getSelectedItem();
                    try {
                        send(out, "SETCOLOR " + colorId);
                        appendLog("[COLOR] Target ball set to: " + colorName, false);
                    } catch (IOException ex) {
                        appendLog("Error sending SETCOLOR command: " + ex.getMessage(), false);
                    }
                } else {
                    appendLog("No client connected", false);
                }
            }
        });
        autoPanel.add(colorCombo);
        
        // Start a timer to update sensor status
        Timer updateTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSensorStatus();
            }
        });
        updateTimer.start();
        
        return autoPanel;
    }
    
    // Update sensor status labels
    private void updateSensorStatus() {
        if (autonomousController == null) return;
        
        Float ultrasonic = autonomousController.getSensorValue("ultrasonic");
        Float touch = autonomousController.getSensorValue("touch");
        Float gyro = autonomousController.getSensorValue("gyro");
        Float infrared = autonomousController.getSensorValue("infrared");
        Float light = autonomousController.getSensorValue("light");
        
        String status = String.format("Distance:%.1fcm Touch:%.0f", 
                                    ultrasonic != null ? ultrasonic : 0.0, 
                                    touch != null ? touch : 0.0);
        sensorStatusLabel.setText(status);
        
        // Update detailed sensor data labels (for debug panel)
        if (debugMode && sensorDataPanel != null && sensorDataPanel.isVisible()) {
            ultrasonicLabel.setText(String.format("Ultrasonic: %.1fcm", 
                ultrasonic != null ? ultrasonic : 0.0));
            gyroLabel.setText(String.format("Gyro: %.1f°/s", 
                gyro != null ? gyro : 0.0));
            infraredLabel.setText(String.format("Infrared: %.1fcm", 
                infrared != null ? infrared : 0.0));
            touchLabel.setText(String.format("Touch: %.0f", 
                touch != null ? touch : 0.0));
            colorLabel.setText(String.format("Light: %.1f", 
                light != null ? light : 0.0));
        }
        
        ServerAutonomousController.ThreatLevel threat = autonomousController.getThreatLevel();
        threatLevelLabel.setText("Threat: " + threat);
        switch (threat) {
            case CRITICAL:
                threatLevelLabel.setForeground(Color.RED);
                break;
            case HIGH:
                threatLevelLabel.setForeground(new Color(255, 140, 0)); // Orange
                break;
            case NONE:
                threatLevelLabel.setForeground(new Color(0, 128, 0)); // Green
                break;
        }
    }
    
    private JButton createCommandButton(String label, final String command, 
                                       final BufferedWriter out, final AtomicInteger frameCount) {
        JButton button = new JButton(label);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    // Throttle commands to prevent flooding
                    long now = System.currentTimeMillis();
                    long timeSinceLastCommand = now - lastCommandTime;
                    
                    // Allow STOP commands immediately, throttle others
                    if (!command.startsWith("STOP") && timeSinceLastCommand < MIN_COMMAND_INTERVAL_MS) {
                        appendLog("[THROTTLED] Command ignored (too fast): " + command, false);
                        return;
                    }
                    
                    lastCommandTime = now;
                    
                    try {
                        // Send command WITHOUT frame number - client doesn't expect it
                        send(out, command);
                        appendLog("[button] Sent: " + command, false);
                    } catch (IOException ex) {
                        LogManager.log("Send error: " + ex.getMessage());
                        appendLog("Error sending command: " + ex.getMessage(), false);
                    }
                }
            }
        });
        return button;
    }
    
    private JButton createTestButton(String label, final BufferedWriter out, final AtomicInteger frameCount) {
        JButton button = new JButton(label);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int[] speeds = {50, 100, 150, 200, 250, 300, 350, 400, 450, 500};
                        int logCount = 5;
                        int intervalMs = 2000; 
                        for (int i = 0; i < speeds.length; i++) {
                            try {
                                // Send command WITHOUT frame number - client doesn't expect it
                                send(out, "MOVE_AND_LOG " + speeds[i] + " " + logCount + " " + intervalMs);
                                appendLog("[auto] Sent MOVE_AND_LOG " + speeds[i] + " " + logCount + " " + intervalMs, false);
                                Thread.sleep(logCount * intervalMs);
                            } catch (Exception ex) {
                                appendLog("Automated test error: " + ex.getMessage(), false);
                                break;
                            }
                        }
                    }
                }, "battery-log-test").start();
            }
        });
        return button;
    }

    public void appendLog(String msg, boolean isDebug) {
        if (logArea != null) {
            if (isDebug && !debugMode) {
                return;
            }

            if(msg.startsWith("TICK") || msg.startsWith("TICK_ACK")) {
                // Don't log TICK messages to avoid clutter
                return;
            }
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        LogManager.log("[you] " + line);
    }
}