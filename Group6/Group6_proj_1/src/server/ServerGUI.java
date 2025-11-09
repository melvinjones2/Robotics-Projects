package server;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

public class ServerGUI {

    private JTextArea logArea;
    private JTextField commandField;
    private JButton sendButton;
    private volatile boolean debugMode = false;

    @SuppressWarnings("Convert2Lambda")
    public void setupMainWindow(final BufferedWriter out, final AtomicInteger frameCount, final AtomicBoolean running) {
        final JFrame mainFrame = new JFrame("EV3 Server");
        mainFrame.setLayout(new BorderLayout());

        // Log area
        logArea = new JTextArea(25, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Top panel with debug checkbox
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JCheckBox debugBox = new JCheckBox("Debug Mode");
        debugBox.setSelected(debugMode);
        debugBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                debugMode = debugBox.isSelected();
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
        
        // Turn & Stop Commands Panel
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
        
        // Individual Motor Control Panel
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
        
        // Rotation Control Panel
        JPanel rotatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rotatePanel.setBorder(BorderFactory.createTitledBorder("Rotation Control"));
        rotatePanel.add(createCommandButton("Rotate A 90°", "ROTATE A 90", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate A -90°", "ROTATE A -90", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate D 90°", "ROTATE D 90", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate D -90°", "ROTATE D -90", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate 180°", "ROTATE D 180", out, frameCount));
        rotatePanel.add(createCommandButton("Rotate 360°", "ROTATE D 360", out, frameCount));
        commandsPanel.add(rotatePanel);
        
        // Sensor & Battery Panel
        JPanel sensorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sensorPanel.setBorder(BorderFactory.createTitledBorder("Sensors & Battery"));
        sensorPanel.add(createCommandButton("Get Battery", "GET_BATTERY", out, frameCount));
        sensorPanel.add(createTestButton("Battery Log Test", out, frameCount));
        commandsPanel.add(sensorPanel);
        
        // System Commands Panel
        JPanel systemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        systemPanel.setBorder(BorderFactory.createTitledBorder("System Commands"));
        systemPanel.add(createCommandButton("Beep", "BEEP", out, frameCount));
        systemPanel.add(createCommandButton("Beep x3", "BEEP 3", out, frameCount));
        systemPanel.add(createCommandButton("Log Message", "LOG Test message from server", out, frameCount));
        JButton byeButton = createCommandButton("Disconnect", "BYE", out, frameCount);
        byeButton.setBackground(new Color(255, 200, 200));
        systemPanel.add(byeButton);
        commandsPanel.add(systemPanel);
        
        // Manual Command Panel
        JPanel manualPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        manualPanel.setBorder(BorderFactory.createTitledBorder("Manual Command"));
        commandField = new JTextField(40);
        sendButton = new JButton("Send Command");
        manualPanel.add(new JLabel("Command:"));
        manualPanel.add(commandField);
        manualPanel.add(sendButton);
        commandsPanel.add(manualPanel);

        // Add panels to frame
        mainFrame.add(topPanel, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.add(commandsPanel, BorderLayout.SOUTH);

        mainFrame.setSize(1200, 700);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setLocation(100, 100);
        mainFrame.setVisible(true);

        // Manual command send action
        @SuppressWarnings("Convert2Lambda")
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = commandField.getText().trim();
                if (!line.isEmpty() && out != null) {
                    try {
                        send(out, line + ":" + frameCount.get());
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
    
    // Helper method to create command buttons
    private JButton createCommandButton(String label, final String command, 
                                       final BufferedWriter out, final AtomicInteger frameCount) {
        JButton button = new JButton(label);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    try {
                        send(out, command + ":" + frameCount.get());
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
    
    // Helper method to create the battery log test button
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
                                send(out, "MOVE_AND_LOG " + speeds[i] + " " + logCount + " " + intervalMs + ":" + frameCount.get());
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