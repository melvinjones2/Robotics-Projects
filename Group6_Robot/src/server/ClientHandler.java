package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler {
    private final Socket client;
    private final ServerGUI gui;
    private final AtomicBoolean running;
    private final AtomicInteger frameCount = new AtomicInteger(0);

    public ClientHandler(Socket client, ServerGUI gui, AtomicBoolean running) {
        this.client = client;
        this.gui = gui;
        this.running = running;
    }

    public void handle() {
        try (
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))
        ) {
            gui.setupLogWindow(out);

            send(out, "HELLO");
            String resp = in.readLine();
            int clientFrame = handshake(resp);
            if (clientFrame == -1) return;

            LogManager.log("Handshake OK. Type messages (BEEP/ BYE supported).");

            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    readLoop(in, out);
                }
            }, "server-reader");
            reader.setDaemon(true);
            reader.start();

            gui.setupCommandWindow(out, frameCount, running);

            reader.join();
        } catch (IOException | InterruptedException e) {
            LogManager.log("Handler error: " + e.getMessage());
        } finally {
            running.set(false);
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    private int handshake(String resp) throws IOException {
        if (resp != null && resp.trim().startsWith("READY:")) {
            try {
                return Integer.parseInt(resp.trim().split(":")[1]);
            } catch (Exception e) {
                LogManager.log("Invalid READY frame: " + resp);
                return -1;
            }
        } else {
            LogManager.log("Handshake failed, got: " + resp);
            return -1;
        }
    }

    private void readLoop(BufferedReader in, BufferedWriter out) {
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                handleMessage(line.trim(), out);
            }
        } catch (IOException e) {
            if (running.get()) LogManager.log("Read error: " + e.getMessage());
        }
    }

    private void handleMessage(String msg, BufferedWriter out) throws IOException {
        if (msg.startsWith("BATTERY:")) {
            logAndGui("[EV3][BATTERY] ", msg, 8, false);
        } else if (msg.startsWith("REPLY:")) {
            logAndGui("[EV3][REPLY] ", msg, 6, false);
        } else if (msg.startsWith("CONTROL:")) {
            logAndGui("[EV3][CONTROL] ", msg, 8, false);
        } else if (msg.startsWith("MOTOR:")) {
            logAndGui("[EV3][MOTOR] ", msg, 8, false);
        } else if (msg.startsWith("LOG:")) {
            logAndGui("[EV3][LOG] ", msg, 4, false);
        } else if (msg.startsWith("TICK_ACK:")) {
            if (gui.isDebugMode()) logAndGui("[DEBUG][TICK_ACK] Received ", msg, 0, true);
        } else if (msg.startsWith("TICK:")) {
            int clientTick = Integer.parseInt(msg.split(":")[1].trim());
            int serverTick = frameCount.incrementAndGet();
            send(out, "TICK_ACK:" + serverTick);
            if (gui.isDebugMode()) logAndGui("[DEBUG][TICK_ACK] Sent ", "TICK_ACK:" + serverTick, 0, true);
        } else if (msg.startsWith("BYE:")) {
            try {
                int clientTick = Integer.parseInt(msg.split(":")[1].trim());
                LogManager.log("[EV3][BYE] Client frame: " + clientTick + ", Server frame: " + frameCount.get());
                send(out, "BYE_ACK:" + frameCount.get());
            } catch (Exception e) {
                send(out, "BYE_ACK:" + frameCount.get());
            }
            running.set(false);
        } else {
            logAndGui("[EV3][UNKNOWN] ", msg, 0, false);
        }
        if ("BYE".equalsIgnoreCase(msg)) {
            running.set(false);
        }
    }

    private void logAndGui(String prefix, String msg, int skip, boolean debug) {
        LogManager.log(prefix + msg.substring(skip).trim());
        gui.appendLog(msg, debug);
    }

    private void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        LogManager.log("[you] " + line);
    }
}