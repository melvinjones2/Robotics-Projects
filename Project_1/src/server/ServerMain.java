package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerMain {
    private static final int PORT = 9999;
    private static final int MAX_LOGS = 10;
    private static BufferedWriter logWriter = null;

    // GUI components
    private static JTextArea logArea;
    private static JTextField commandField;
    private static JButton sendButton;

    public static void main(String[] args) {
        // Rotate log files before opening new log
        rotateLogs();

        // Setup GUI windows
        setupLogWindow();
        BufferedWriter out = null;
        BufferedReader in = null;
        ServerSocket server = null;
        Socket client = null;
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger frameCount = new AtomicInteger(0);

        try {
            logWriter = new BufferedWriter(new FileWriter("server_log0.txt", false)); // always new log
            log("Server listening on " + PORT + " ...");
            server = new ServerSocket(PORT);
            client = server.accept();
            log("Client connected: " + client.getRemoteSocketAddress());

            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            in  = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // ---- handshake ----
            send(out, "HELLO");
            String resp = in.readLine();
            int clientFrame = 0;
            if (resp != null && resp.trim().startsWith("READY:")) {
                try {
                    clientFrame = Integer.parseInt(resp.trim().split(":")[1]);
                } catch (Exception e) {
                    log("Invalid READY frame: " + resp);
                    return;
                }
            } else {
                log("Handshake failed, got: " + resp);
                return;
            }
            log("Handshake OK. Type messages (BEEP/ BYE supported).");

            final BufferedWriter outFinal = out;
            final BufferedReader inRef = in;
            Thread reader = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        String line;
                        while (running.get() && (line = inRef.readLine()) != null) {
                            String msg = line.trim();
                            if (msg.startsWith("BATTERY:")) {
                                log("[EV3][BATTERY] " + msg.substring(8).trim());
                            } else if (msg.startsWith("REPLY:")) {
                                log("[EV3][REPLY] " + msg.substring(6).trim());
                            } else if (msg.startsWith("CONTROL:")) {
                                log("[EV3][CONTROL] " + msg.substring(8).trim());
                            } else if (msg.startsWith("MOTOR:")) {
                                log("[EV3][MOTOR] " + msg.substring(8).trim());
                            } else if (msg.startsWith("TICK:")) {
                                int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                                int serverTick = frameCount.incrementAndGet();
                                send(outFinal, "TICK_ACK:" + serverTick);
                            } else if (msg.startsWith("BYE:")) {
                                try {
                                    int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                                    log("[EV3][BYE] Client frame: " + clientTick + ", Server frame: " + frameCount.get());
                                    send(outFinal, "BYE_ACK:" + frameCount.get());
                                } catch (Exception e) {
                                    send(outFinal, "BYE_ACK:" + frameCount.get());
                                }
                                running.set(false);
                                break;
                            } else {
                                log("[EV3][UNKNOWN] " + msg);
                            }
                            if ("BYE".equalsIgnoreCase(msg)) {
                                running.set(false);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        if (running.get()) log("Read error: " + e.getMessage());
                    }
                }
            }, "server-reader");
            reader.setDaemon(true);
            reader.start();

            // ---- command window logic ----
            setupCommandWindow(out, frameCount, running);

            // Wait for the reader thread to finish
            reader.join();

        } catch (IOException | InterruptedException e) {
            log("Server error: " + e.getMessage());
        } finally {
            running.set(false);
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (client != null) client.close(); } catch (IOException ignored) {}
            try { if (server != null) server.close(); } catch (IOException ignored) {}
            log("Server closed.");
            try { if (logWriter != null) logWriter.close(); } catch (IOException ignored) {}
        }
    }

    // Rotates server_log0.txt ... server_log9.txt, deleting the oldest and shifting others up
    private static void rotateLogs() {
        try {
            // Delete the oldest log if it exists
            Path oldest = Paths.get("server_log" + (MAX_LOGS - 1) + ".txt");
            if (Files.exists(oldest)) {
                Files.delete(oldest);
            }
            // Move logs N-2 ... 0 up by one
            for (int i = MAX_LOGS - 2; i >= 0; i--) {
                Path src = Paths.get("server_log" + i + ".txt");
                Path dest = Paths.get("server_log" + (i + 1) + ".txt");
                if (Files.exists(src)) {
                    Files.move(src, dest);
                }
            }
        } catch (IOException e) {
            System.err.println("Log rotation error: " + e.getMessage());
        }
    }

    private static void setupLogWindow() {
        JFrame logFrame = new JFrame("EV3 Server Log");
        logArea = new JTextArea(30, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        logFrame.add(scrollPane);
        logFrame.pack();
        logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logFrame.setLocation(100, 100);
        logFrame.setVisible(true);
    }

    private static void setupCommandWindow(final BufferedWriter out, final AtomicInteger frameCount, final AtomicBoolean running) {
        final JFrame cmdFrame = new JFrame("EV3 Server Command");
        commandField = new JTextField(40);
        sendButton = new JButton("Send");
        JPanel panel = new JPanel();
        panel.add(commandField);
        panel.add(sendButton);
        cmdFrame.add(panel);
        cmdFrame.pack();
        cmdFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        cmdFrame.setLocation(100, 500);
        cmdFrame.setVisible(true);

        // Send command on button click or Enter key
        ActionListener sendAction = new ActionListener() {
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
                        log("Send error: " + ex.getMessage());
                    }
                    commandField.setText("");
                }
            }
        };
        sendButton.addActionListener(sendAction);
        commandField.addActionListener(sendAction);

        // Allow closing the command window to exit the server
        cmdFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                running.set(false);
                cmdFrame.dispose();
            }
        });
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        log("[you] " + line);
    }

    private static void log(String msg) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String logMsg = "[" + timestamp + "] " + msg;
        if (logArea != null) {
            logArea.append(logMsg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
        System.out.println(logMsg);
        if (logWriter != null) {
            try {
                logWriter.write(logMsg);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException ignored) {}
        }
    }
}
