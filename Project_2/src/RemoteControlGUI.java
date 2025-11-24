import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class RemoteControlGUI extends JFrame {
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private JLabel statusLabel;
    
    private Socket socket;
    private PrintWriter out;
    
    private boolean isConnected = false;
    
    // Track key states to prevent spamming commands on key repeat
    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean aPressed = false;
    private boolean dPressed = false;

    public RemoteControlGUI() {
        setTitle("Robot Remote Control");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Connection Panel
        JPanel connectionPanel = new JPanel();
        ipField = new JTextField("10.0.1.8", 10); // Default EV3 IP
        portField = new JTextField("12345", 5);
        connectButton = new JButton("Connect");
        
        connectionPanel.add(new JLabel("IP:"));
        connectionPanel.add(ipField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        connectionPanel.add(connectButton);
        
        add(connectionPanel, BorderLayout.NORTH);
        
        // Status
        statusLabel = new JLabel("Status: Disconnected", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(statusLabel, BorderLayout.SOUTH);
        
        // Controls Panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // --- Setup Panel ---
        JPanel setupPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setupPanel.setBorder(BorderFactory.createTitledBorder("Game Setup"));
        
        final JRadioButton rbBlue = new JRadioButton("Blue", true);
        final JRadioButton rbGreen = new JRadioButton("Green");
        ButtonGroup teamGroup = new ButtonGroup();
        teamGroup.add(rbBlue);
        teamGroup.add(rbGreen);
        
        final JRadioButton rbAttacker = new JRadioButton("Attacker", true);
        final JRadioButton rbDefender = new JRadioButton("Defender");
        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(rbAttacker);
        roleGroup.add(rbDefender);
        
        JButton btnSendSetup = new JButton("Send Setup");
        
        setupPanel.add(new JLabel("Team:"));
        setupPanel.add(rbBlue);
        setupPanel.add(rbGreen);
        setupPanel.add(new JSeparator(SwingConstants.VERTICAL));
        setupPanel.add(new JLabel("Role:"));
        setupPanel.add(rbAttacker);
        setupPanel.add(rbDefender);
        setupPanel.add(btnSendSetup);
        
        centerPanel.add(setupPanel, BorderLayout.NORTH);
        
        // Directional Controls (Grid)
        JPanel dirPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        dirPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        final JButton btnUp = createStyledButton("FORWARD (W)");
        final JButton btnDown = createStyledButton("BACKWARD (S)");
        final JButton btnLeft = createStyledButton("LEFT (A)");
        final JButton btnRight = createStyledButton("RIGHT (D)");
        final JButton btnStop = createStyledButton("STOP (Space)");
        
        // Add to grid
        dirPanel.add(new JLabel("")); 
        dirPanel.add(btnUp);
        dirPanel.add(new JLabel("")); 
        
        dirPanel.add(btnLeft);
        dirPanel.add(btnStop);
        dirPanel.add(btnRight);
        
        dirPanel.add(new JLabel("")); 
        dirPanel.add(btnDown);
        dirPanel.add(new JLabel("")); 
        
        centerPanel.add(dirPanel, BorderLayout.CENTER);
        
        // Action Controls
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        final JButton btnKick = createStyledButton("KICK (K)");
        final JButton btnArmUp = createStyledButton("ARM UP (U)");
        final JButton btnArmDown = createStyledButton("ARM DOWN (J)");
        final JButton btnEnter = createStyledButton("ENTER (Ent)");
        final JButton btnExit = createStyledButton("EXIT APP (Esc)");
        
        btnKick.setBackground(new Color(255, 100, 100)); // Reddish
        btnEnter.setBackground(new Color(100, 255, 100)); // Greenish
        btnExit.setBackground(Color.RED);
        btnExit.setForeground(Color.WHITE);
        
        actionPanel.add(btnKick);
        actionPanel.add(btnArmUp);
        actionPanel.add(btnArmDown);
        actionPanel.add(btnEnter);
        actionPanel.add(btnExit);
        
        centerPanel.add(actionPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Event Listeners
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleConnection();
            }
        });
        
        btnSendSetup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String team = rbBlue.isSelected() ? "BLUE" : "GREEN";
                String role = rbAttacker.isSelected() ? "ATTACKER" : "DEFENDER";
                sendCommand("SETUP " + team + " " + role);
                requestFocus(); // Return focus to frame for keyboard controls
            }
        });
        
        // Mouse Listeners for Buttons (Hold to move)
        setupButton(btnUp, "FORWARD");
        setupButton(btnDown, "BACKWARD");
        setupButton(btnLeft, "LEFT");
        setupButton(btnRight, "RIGHT");
        setupButton(btnStop, "STOP");
        
        // Click actions for others
        btnKick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("KICK");
                requestFocus();
            }
        });
        
        btnArmUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("ARM_UP");
                requestFocus();
            }
        });
        
        btnArmDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("ARM_DOWN");
                requestFocus();
            }
        });
        
        btnEnter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("ENTER");
                requestFocus();
            }
        });
        
        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCommand("EXIT");
                requestFocus();
            }
        });
        
        // Keyboard Listeners
        setFocusable(true);
        requestFocus();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isConnected) return;
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        sendCommand("ENTER");
                        btnEnter.doClick(100);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        sendCommand("EXIT");
                        btnExit.doClick(100);
                        break;
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_UP:
                        if (!wPressed) {
                            sendCommand("FORWARD");
                            wPressed = true;
                            btnUp.getModel().setPressed(true);
                        }
                        break;
                    case KeyEvent.VK_S:
                    case KeyEvent.VK_DOWN:
                        if (!sPressed) {
                            sendCommand("BACKWARD");
                            sPressed = true;
                            btnDown.getModel().setPressed(true);
                        }
                        break;
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_LEFT:
                        if (!aPressed) {
                            sendCommand("LEFT");
                            aPressed = true;
                            btnLeft.getModel().setPressed(true);
                        }
                        break;
                    case KeyEvent.VK_D:
                    case KeyEvent.VK_RIGHT:
                        if (!dPressed) {
                            sendCommand("RIGHT");
                            dPressed = true;
                            btnRight.getModel().setPressed(true);
                        }
                        break;
                    case KeyEvent.VK_SPACE:
                        sendCommand("STOP");
                        btnStop.getModel().setPressed(true);
                        break;
                    case KeyEvent.VK_K:
                        sendCommand("KICK");
                        btnKick.doClick(100);
                        break;
                    case KeyEvent.VK_U:
                        sendCommand("ARM_UP");
                        btnArmUp.doClick(100);
                        break;
                    case KeyEvent.VK_J:
                        sendCommand("ARM_DOWN");
                        btnArmDown.doClick(100);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!isConnected) return;
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_UP:
                        wPressed = false;
                        sendCommand("STOP");
                        btnUp.getModel().setPressed(false);
                        break;
                    case KeyEvent.VK_S:
                    case KeyEvent.VK_DOWN:
                        sPressed = false;
                        sendCommand("STOP");
                        btnDown.getModel().setPressed(false);
                        break;
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_LEFT:
                        aPressed = false;
                        sendCommand("STOP");
                        btnLeft.getModel().setPressed(false);
                        break;
                    case KeyEvent.VK_D:
                    case KeyEvent.VK_RIGHT:
                        dPressed = false;
                        sendCommand("STOP");
                        btnRight.getModel().setPressed(false);
                        break;
                    case KeyEvent.VK_SPACE:
                        btnStop.getModel().setPressed(false);
                        break;
                }
            }
        });
    }
    
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusable(false); // Important: prevent buttons from stealing focus from KeyListener
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        return btn;
    }
    
    private void setupButton(JButton btn, final String command) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sendCommand(command);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (command.equals("FORWARD") || command.equals("BACKWARD") || 
                    command.equals("LEFT") || command.equals("RIGHT")) {
                    sendCommand("STOP");
                }
            }
        });
    }

    private void toggleConnection() {
        if (isConnected) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        String ip = ipField.getText();
        try {
            int port = Integer.parseInt(portField.getText());
            
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 3000); // 3s timeout
            out = new PrintWriter(socket.getOutputStream(), true);
            
            isConnected = true;
            connectButton.setText("Disconnect");
            statusLabel.setText("Status: Connected to " + ip);
            statusLabel.setForeground(new Color(0, 150, 0));
            
            // Focus frame for keys
            this.requestFocus();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection Failed: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {}
        
        isConnected = false;
        connectButton.setText("Connect");
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.BLACK);
    }

    private void sendCommand(String cmd) {
        if (isConnected && out != null) {
            System.out.println("Sending: " + cmd);
            out.println(cmd);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RemoteControlGUI().setVisible(true);
            }
        });
    }
}
