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

        // Bottom panel with command field and buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        commandField = new JTextField(30); // Reduce width to fit more buttons
        sendButton = new JButton("Send");
        JButton batteryButton = new JButton("Request Battery");
        JButton testButton = new JButton("Battery Log Test");
        bottomPanel.add(commandField);
        bottomPanel.add(sendButton);
        bottomPanel.add(batteryButton);
        bottomPanel.add(testButton);

        // Add panels to frame
        mainFrame.add(topPanel, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.add(bottomPanel, BorderLayout.SOUTH);

        mainFrame.setSize(1000, 600);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setLocation(100, 100);
        mainFrame.setVisible(true);

        // Button actions
        batteryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (out != null) {
                    try {
                        send(out, "GET_BATTERY");
                    } catch (IOException ex) {
                        LogManager.log("Send error: " + ex.getMessage());
                    }
                }
            }
        });

        // New test button action
        testButton.addActionListener(new ActionListener() {
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
                                // Send the new combined command to the client
                                send(out, "MOVE_AND_LOG " + speeds[i] + " " + logCount + " " + intervalMs);
                                appendLog("[auto] Sent MOVE_AND_LOG " + speeds[i] + " " + logCount + " " + intervalMs, false);

                                // Wait for the duration of the logging before next speed
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