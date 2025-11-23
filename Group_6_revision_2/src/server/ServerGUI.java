package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import shared.CommandBuilder;
import shared.SocketConnection;

public class ServerGUI extends JFrame {
	private JTextArea logArea;
	private JTextField commandInput;
	private JButton sendButton;
	private SocketConnection connection;
	private Thread logListenerThread;
	private volatile boolean keepListening = true;
	
	// Control buttons
	private JButton connectButton;
	private JButton disconnectButton;
	
	public ServerGUI() {
		initializeUI();
	}
	
	private void initializeUI() {
		setTitle("EV3 Robot Controller");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLocationRelativeTo(null);
		
		// Main layout
		setLayout(new BorderLayout(10, 10));
		
		// Top panel - Connection controls
		JPanel topPanel = createConnectionPanel();
		add(topPanel, BorderLayout.NORTH);
		
		// Center panel - Log viewer
		JPanel centerPanel = createLogPanel();
		add(centerPanel, BorderLayout.CENTER);
		
		// Right panel - Command buttons
		JPanel rightPanel = createCommandButtonPanel();
		add(rightPanel, BorderLayout.EAST);
		
		// Bottom panel - Command input
		JPanel bottomPanel = createCommandInputPanel();
		add(bottomPanel, BorderLayout.SOUTH);
	}
	
	private JPanel createConnectionPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		connectButton = new JButton("Start Server");
		disconnectButton = new JButton("Stop Server");
		disconnectButton.setEnabled(false);
		
		connectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startServer();
			}
		});
		
		disconnectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopServer();
			}
		});
		
		panel.add(connectButton);
		panel.add(disconnectButton);
		
		return panel;
	}
	
	private JPanel createLogPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder("Robot Logs"));
		
		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		logArea.setLineWrap(true);
		logArea.setWrapStyleWord(true);
		
		JScrollPane scrollPane = new JScrollPane(logArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		// Clear button
		JButton clearButton = new JButton("Clear Logs");
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logArea.setText("");
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(clearButton);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		
		return panel;
	}
	
	private JPanel createCommandButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new TitledBorder("Quick Commands"));
		panel.setPreferredSize(new Dimension(200, 0));
		
		// Movement commands
		addSectionLabel(panel, "Movement");
		addCommandButton(panel, "Move Forward 50", CommandBuilder.build(CommandBuilder.MOVE, 50));
		addCommandButton(panel, "Move Back 30", CommandBuilder.build(CommandBuilder.MOVE, -30));
		addCommandButton(panel, "Rotate 90°", CommandBuilder.build(CommandBuilder.ROTATE, 90));
		addCommandButton(panel, "Rotate -90°", CommandBuilder.build(CommandBuilder.ROTATE, -90));
		addCommandButton(panel, "Stop", CommandBuilder.build(CommandBuilder.STOP));
		
		panel.add(Box.createVerticalStrut(10));
		
		// Speed commands
		addSectionLabel(panel, "Speed");
		addCommandButton(panel, "Speed Slow (5)", CommandBuilder.build(CommandBuilder.SPEED, 5));
		addCommandButton(panel, "Speed Medium (10)", CommandBuilder.build(CommandBuilder.SPEED, 10));
		addCommandButton(panel, "Speed Fast (20)", CommandBuilder.build(CommandBuilder.SPEED, 20));
		
		panel.add(Box.createVerticalStrut(10));
		
		// Navigation commands
		addSectionLabel(panel, "Navigation");
		addCommandButton(panel, "Simple Nav 100", CommandBuilder.build(CommandBuilder.NAV_SIMPLE, 100));
		addCommandButton(panel, "Dynamic Nav 80", CommandBuilder.build(CommandBuilder.NAV_DYNAMIC, 80));
		addCommandButton(panel, "Go to (50,50)", CommandBuilder.build(CommandBuilder.NAV_GOTO, 50, 50));
		
		panel.add(Box.createVerticalStrut(10));
		
		// Ball tracking commands
		addSectionLabel(panel, "Ball Tracking");
		addCommandButton(panel, "Find Ball", CommandBuilder.build(CommandBuilder.FIND_BALL));
		addCommandButton(panel, "Approach Ball", CommandBuilder.build(CommandBuilder.APPROACH_BALL));
		addCommandButton(panel, "Track Ball 30s", CommandBuilder.build(CommandBuilder.TRACK_BALL, 30));
		
		panel.add(Box.createVerticalStrut(10));
		
		// Exit command
		addCommandButton(panel, "Exit", CommandBuilder.build(CommandBuilder.EXIT));
		
		panel.add(Box.createVerticalGlue());
		
		return panel;
	}
	
	private void addSectionLabel(JPanel panel, String text) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(label);
		panel.add(Box.createVerticalStrut(5));
	}
	
	private void addCommandButton(JPanel panel, String buttonText, final String command) {
		JButton button = new JButton(buttonText);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand(command);
			}
		});
		
		panel.add(button);
		panel.add(Box.createVerticalStrut(3));
	}
	
	private JPanel createCommandInputPanel() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Manual Command Input"));
		
		commandInput = new JTextField();
		commandInput.setFont(new Font("Monospaced", Font.PLAIN, 12));
		
		sendButton = new JButton("Send");
		sendButton.setEnabled(false);
		
		// Send on Enter key
		commandInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand(commandInput.getText());
			}
		});
		
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand(commandInput.getText());
			}
		});
		
		panel.add(new JLabel("Command: "), BorderLayout.WEST);
		panel.add(commandInput, BorderLayout.CENTER);
		panel.add(sendButton, BorderLayout.EAST);
		
		return panel;
	}
	
	private void startServer() {
		// Check if previous thread is still running
		if (logListenerThread != null && logListenerThread.isAlive()) {
			appendLog("[ERROR] Previous connection still cleaning up, please wait...");
			return;
		}
		
		// Disable start button immediately
		connectButton.setEnabled(false);
		appendLog("[SERVER] Starting server and waiting for client connection...");
		
		// Accept connection in background thread to avoid freezing GUI
		Thread acceptThread = new Thread(new Runnable() {
			public void run() {
				try {
					connection = SocketConnection.createServer();
					
					// Update UI on success
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							appendLog("[SERVER] Client connected!");
							disconnectButton.setEnabled(true);
							sendButton.setEnabled(true);
						}
					});
					
					// Start log listener
					keepListening = true;
					logListenerThread = new Thread(new Runnable() {
						public void run() {
							listenForLogs();
						}
					});
					logListenerThread.start();
					
				} catch (final IOException e) {
					// Update UI on error
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							appendLog("[ERROR] Failed to start server: " + e.getMessage());
							connectButton.setEnabled(true);
							JOptionPane.showMessageDialog(ServerGUI.this, 
								"Failed to start server: " + e.getMessage(),
								"Server Error", 
								JOptionPane.ERROR_MESSAGE);
						}
					});
				}
			}
		});
		acceptThread.setDaemon(true);
		acceptThread.start();
	}
	
	private void stopServer() {
		keepListening = false;
		
		// Interrupt log listener thread first
		if (logListenerThread != null && logListenerThread.isAlive()) {
			logListenerThread.interrupt();
		}
		
		// Try to send exit command if connection is still alive
		if (connection != null) {
			try {
				connection.sendLine(CommandBuilder.build(CommandBuilder.EXIT));
			} catch (IOException e) {
				// Connection already broken, ignore
			}
			
			// Close connection
			try {
				connection.close();
			} catch (Exception e) {
				// Ignore close errors
			}
			
			connection = null;
		}
		
		// Wait for log thread to finish
		if (logListenerThread != null) {
			try {
				logListenerThread.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			logListenerThread = null;
		}
		
		// Give socket time to fully release (Windows needs longer)
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		appendLog("[SERVER] Server stopped");
		
		// Disable controls
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);
		sendButton.setEnabled(false);
	}
	
	private void sendCommand(String command) {
		if (connection == null) {
			appendLog("[ERROR] Server not started or no client connected");
			return;
		}
		
		if (command == null || command.trim().isEmpty()) {
			return;
		}
		
		try {
			appendLog("[SEND] " + command);
			connection.sendLine(command);
			commandInput.setText("");
			
		} catch (IOException e) {
			appendLog("[ERROR] Failed to send command: " + e.getMessage());
			appendLog("[ERROR] Connection lost - stopping server");
			stopServer();
		}
	}
	
	private void listenForLogs() {
		try {
			while (keepListening && connection != null && !Thread.currentThread().isInterrupted()) {
				String line = connection.readLine();
				if (line == null) {
					appendLog("[SERVER] Client disconnected");
					break;
				}
				
				if (connection.isLogMessage(line)) {
					String log = connection.extractLogMessage(line);
					appendLog("[CLIENT LOG] " + log);
				} else {
					appendLog("[CLIENT] " + line);
				}
			}
		} catch (IOException e) {
			if (keepListening) {
				appendLog("[ERROR] Connection lost: " + e.getMessage());
			}
		} finally {
			// Auto-cleanup on connection loss
			if (keepListening) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						stopServer();
					}
				});
			}
		}
	}
	
	private void appendLog(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				logArea.append(message + "\n");
				logArea.setCaretPosition(logArea.getDocument().getLength());
			}
		});
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ServerGUI gui = new ServerGUI();
				gui.setVisible(true);
			}
		});
	}
}
