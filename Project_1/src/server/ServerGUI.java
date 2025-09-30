package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerGUI {
    private JTextArea logArea;
    private JTextField commandField;
    private JButton sendButton;
    private JFrame logFrame;
    private JFrame cmdFrame;
    private volatile boolean debugMode = false;
    private JPanel topPanel; // <-- Add this field

    public void setupLogWindow(final BufferedWriter outFinal) {
        logFrame = new JFrame("EV3 Server Log");
        logArea = new JTextArea(30, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // <-- Use field
        final JCheckBox debugBox = new JCheckBox("Debug Mode");
        debugBox.setSelected(debugMode);
        debugBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                debugMode = debugBox.isSelected();
                if (outFinal != null) {
                    try {
                        send(outFinal, "SET_DEBUG:" + (debugMode ? "1" : "0"));
                    } catch (IOException ex) {
                        LogManager.log("Send error: " + ex.getMessage());
                    }
                }
            }
        });
        topPanel.add(debugBox);

        logFrame.setLayout(new BorderLayout());
        logFrame.add(topPanel, BorderLayout.NORTH); // <-- Use field
        logFrame.add(scrollPane, BorderLayout.CENTER);

        logFrame.pack();
        logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logFrame.setLocation(100, 100);
        logFrame.setVisible(true);
    }

    public void setupCommandWindow(final BufferedWriter out, final AtomicInteger frameCount, final AtomicBoolean running) {
        cmdFrame = new JFrame("EV3 Server Command");
        commandField = new JTextField(40);
        sendButton = new JButton("Send");
        JButton batteryButton = new JButton("Request Battery");

        JPanel panel = new JPanel();
        panel.add(commandField);
        panel.add(sendButton);
        panel.add(batteryButton);

        cmdFrame.add(panel);
        cmdFrame.pack();
        cmdFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        cmdFrame.setLocation(100, 500);
        cmdFrame.setVisible(true);

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

        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = commandField.getText().trim();
                if (!line.isEmpty() && out != null) {
                    try {
                        send(out, line + ":" + frameCount.get());
                        if ("BYE".equalsIgnoreCase(line)) {
                            running.set(false);
                            cmdFrame.dispose();
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

        cmdFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                running.set(false);
                cmdFrame.dispose();
            }
        });
    }

    public void closeWindows() {
        if (logFrame != null) logFrame.dispose();
        if (cmdFrame != null) cmdFrame.dispose();
    }

    public void appendLog(String msg, boolean isDebug) {
        if (logArea != null) {
            if (isDebug) {
                if (debugMode) {
                    logArea.append(msg + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            } else {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        }
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        if (line.startsWith("TICK_ACK:")) {
            if (debugMode) LogManager.log("[you] " + line);
        } else {
            LogManager.log("[you] " + line);
        }
    }

    /**
     * Adds buttons for payload selection and browser opening to the log window.
     * Call this after setupLogWindow().
     */
    public void addPayloadButton(final Runnable selectPayload) {
        if (topPanel == null) return;

        JButton selectBtn = new JButton("Select Payload");
        selectBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectPayload.run();
            }
        });

        topPanel.add(selectBtn);

        topPanel.revalidate();
        topPanel.repaint();
    }
}