package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.Desktop;
import java.net.URI;
import javax.swing.JOptionPane;
import java.util.List;

public class ServerMain {
    private static final int PORT = 9999;
    private static Payload currentPayload = null;
    private static BufferedWriter out = null; // Make static

    public static void main(String[] args) {
        LogManager.rotateLogs();
        final ServerGUI gui = new ServerGUI();

        BufferedReader in = null;
        ServerSocket server = null;
        Socket client = null;
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger frameCount = new AtomicInteger(0);

        try {
            LogManager.openLog();
            LogManager.log("Server listening on " + PORT + " ...");
            server = new ServerSocket(PORT);
            client = server.accept();
            LogManager.log("Client connected: " + client.getRemoteSocketAddress());

            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            in  = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // Only call setupLogWindow ONCE, now with the correct writer
            gui.setupLogWindow(out);

            // Now add the payload buttons
            gui.addPayloadButton(
                new Runnable() { public void run() { showPayloadSelection(); } }
            );

            send(out, "HELLO");
            String resp = in.readLine();
            int clientFrame = 0;
            if (resp != null && resp.trim().startsWith("READY:")) {
                try {
                    clientFrame = Integer.parseInt(resp.trim().split(":")[1]);
                } catch (Exception e) {
                    LogManager.log("Invalid READY frame: " + resp);
                    return;
                }
            } else {
                LogManager.log("Handshake failed, got: " + resp);
                return;
            }
            LogManager.log("Handshake OK. Type messages (BEEP/ BYE supported).");

            final BufferedWriter outFinal = out;
            final BufferedReader inRef = in;
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    try {
                        String line;
                        while (running.get() && (line = inRef.readLine()) != null) {
                            String msg = line.trim();
                            if (currentPayload != null) {
                                currentPayload.handleMessage(msg, outFinal, gui);
                            } else {
                                // fallback to default handling
                                if (msg.startsWith("BATTERY:")) {
                                    LogManager.log("[EV3][BATTERY] " + msg.substring(8).trim());
                                    gui.appendLog(msg, false);
                                } else if (msg.startsWith("REPLY:")) {
                                    LogManager.log("[EV3][REPLY] " + msg.substring(6).trim());
                                    gui.appendLog(msg, false);
                                } else if (msg.startsWith("CONTROL:")) {
                                    LogManager.log("[EV3][CONTROL] " + msg.substring(8).trim());
                                    gui.appendLog(msg, false);
                                } else if (msg.startsWith("MOTOR:")) {
                                    LogManager.log("[EV3][MOTOR] " + msg.substring(8).trim());
                                    gui.appendLog(msg, false);
                                } else if (msg.startsWith("LOG:")) {
                                    LogManager.log("[EV3][LOG] " + msg.substring(4).trim());
                                    gui.appendLog(msg, false);
                                } else if (msg.startsWith("TICK_ACK:")) {
                                    if (gui.isDebugMode()) {
                                        LogManager.log("[DEBUG][TICK_ACK] Received " + msg);
                                        gui.appendLog(msg, true);
                                    }
                                } else if (msg.startsWith("TICK:")) {
                                    int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                                    int serverTick = frameCount.incrementAndGet();
                                    send(outFinal, "TICK_ACK:" + serverTick);
                                    if (gui.isDebugMode()) {
                                        LogManager.log("[DEBUG][TICK_ACK] Sent TICK_ACK:" + serverTick);
                                        gui.appendLog("TICK_ACK:" + serverTick, true);
                                    }
                                } else if (msg.startsWith("BYE:")) {
                                    try {
                                        int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                                        LogManager.log("[EV3][BYE] Client frame: " + clientTick + ", Server frame: " + frameCount.get());
                                        send(outFinal, "BYE_ACK:" + frameCount.get());
                                    } catch (Exception e) {
                                        send(outFinal, "BYE_ACK:" + frameCount.get());
                                    }
                                    running.set(false);
                                    gui.closeWindows();
                                    break;
                                } else {
                                    LogManager.log("[EV3][UNKNOWN] " + msg);
                                    gui.appendLog(msg, false);
                                }
                                if ("BYE".equalsIgnoreCase(msg)) {
                                    running.set(false);
                                    gui.closeWindows();
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        if (running.get()) LogManager.log("Read error: " + e.getMessage());
                    }
                }
            }, "server-reader");
            reader.setDaemon(true);
            reader.start();

            gui.setupCommandWindow(out, frameCount, running);

            reader.join();

        } catch (IOException e) {
            LogManager.log("Server error: " + e.getMessage());
        } catch (InterruptedException e) {
            LogManager.log("Server interrupted: " + e.getMessage());
        } finally {
            running.set(false);
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (client != null) client.close(); } catch (IOException ignored) {}
            try { if (server != null) server.close(); } catch (IOException ignored) {}
            LogManager.log("Server closed.");
            LogManager.close();
        }
    }

    public static void setPayload(Payload payload) {
        if (currentPayload != null) currentPayload.stop();
        currentPayload = payload;
        if (currentPayload != null) currentPayload.start();
        LogManager.log("Payload injected: " + (payload != null ? payload.getName() : "None"));
    }

    public static void showPayloadSelection() {
        List<Class<? extends Payload>> payloads = PayloadLoader.discoverPayloads();
        if (payloads.isEmpty()) {
            LogManager.log("No payloads found.");
            return;
        }
        String[] names = new String[payloads.size()];
        for (int i = 0; i < payloads.size(); i++) {
            try {
                names[i] = payloads.get(i).newInstance().getName();
            } catch (Exception e) {
                names[i] = payloads.get(i).getSimpleName();
            }
        }
        String selected = (String) JOptionPane.showInputDialog(
            null, "Select a payload:", "Payload Loader",
            JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (selected != null) {
            Payload p = PayloadLoader.findPayloadByName(selected);
            if (p != null) {
                setPayload(p);
                LogManager.log("Loaded payload: " + p.getName());
            }
        }
    }

    public static BufferedWriter getWriter() {
        return out;
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        LogManager.log("[you] " + line);
    }
}
