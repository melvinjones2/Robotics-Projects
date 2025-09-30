package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JOptionPane;

public class ServerMain {
    private static final int PORT = 9999;
    private static Payload currentPayload = null;
    private static BufferedWriter out = null; 
    private static AtomicInteger frameCount = new AtomicInteger(0);

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

            sendMessage("HELLO", "");
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
                            Message message = Message.parse(msg);

                            if (currentPayload != null) {
                                currentPayload.handleMessage(msg, outFinal, gui);
                            } else {
                                switch (message.getType()) {
                                    case "BATTERY":
                                        LogManager.log("[EV3][BATTERY] " + message.getPayload());
                                        gui.appendLog(msg, false);
                                        break;
                                    case "REPLY":
                                        LogManager.log("[EV3][REPLY] " + message.getPayload());
                                        gui.appendLog(msg, false);
                                        break;
                                    case "CONTROL":
                                        LogManager.log("[EV3][CONTROL] " + message.getPayload());
                                        gui.appendLog(msg, false);
                                        break;
                                    case "MOTOR":
                                        LogManager.log("[EV3][MOTOR] " + message.getPayload());
                                        gui.appendLog(msg, false);
                                        break;
                                    case "LOG":
                                        LogManager.log("[EV3][LOG] " + message.getPayload());
                                        gui.appendLog(msg, false);
                                        break;
                                    case "TICK_ACK":
                                        if (gui.isDebugMode()) {
                                            LogManager.log("[DEBUG][TICK_ACK] Received " + msg);
                                            gui.appendLog(msg, true);
                                        }
                                        break;
                                    case "TICK":
                                        int clientTick = Integer.parseInt(message.getPayload());
                                        int serverTick = frameCount.incrementAndGet();
                                        sendMessage("TICK_ACK", String.valueOf(serverTick));
                                        if (gui.isDebugMode()) {
                                            LogManager.log("[DEBUG][TICK_ACK] Sent TICK_ACK:" + serverTick);
                                            gui.appendLog("TICK_ACK:" + serverTick, true);
                                        }
                                        break;
                                    case "BYE":
                                        try {
                                            int clientTickBye = Integer.parseInt(message.getPayload());
                                            LogManager.log("[EV3][BYE] Client frame: " + clientTickBye + ", Server frame: " + frameCount.get());
                                            sendMessage("BYE_ACK", String.valueOf(frameCount.get()));
                                        } catch (Exception e) {
                                            sendMessage("BYE_ACK", String.valueOf(frameCount.get()));
                                        }
                                        running.set(false);
                                        gui.closeWindows();
                                        break;
                                    default:
                                        LogManager.log("[EV3][UNKNOWN] " + msg);
                                        gui.appendLog(msg, false);
                                }
                                if ("BYE".equalsIgnoreCase(message.getType())) {
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
    public static int getFrameCount() {
        return frameCount.get();
    }
    public static void sendMessage(String type, String payload, int frameCount) throws IOException {
        if (out != null) {
            String msg = Message.construct(type, payload + frameCount);
            out.write(msg);
            out.write("\n");
            out.flush();
            LogManager.log("[you] " + msg);
        }
    }

    // Overload for messages without frame count
    public static void sendMessage(String type, String payload) throws IOException {
        if (out != null) {
            String msg = Message.construct(type, payload + frameCount.get());
            out.write(msg);
            out.write("\n");
            out.flush();
            LogManager.log("[you] " + msg);
        }
    }
}
