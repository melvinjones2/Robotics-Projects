package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMain {
    private static final int PORT = 9999;
    private static BufferedWriter logWriter = null;

    public static void main(String[] args) {
        ServerSocket server = null;
        Socket client = null;
        BufferedWriter out = null;
        BufferedReader in = null;
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger frameCount = new AtomicInteger(0);

        try {
            // Open log file for writing
            logWriter = new BufferedWriter(new FileWriter("server_log.txt", true));
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
                                frameCount.set(clientTick); // Sync server to client
                                send(outFinal, "TICK_ACK:" + clientTick); // Echo back client's tick
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

            // ---- interactive send loop ----
            Scanner sc = new Scanner(System.in);
            try {
                while (running.get()) {
                    // Non-blocking user input check
                    if (System.in.available() > 0) {
                        String line = sc.nextLine();
                        send(out, line + ":" + frameCount.get());
                        if ("BYE".equalsIgnoreCase(line.trim())) {
                            running.set(false);
                            break;
                        }
                    }
                    Thread.sleep(10); // Small sleep to avoid busy-waiting
                }
            } finally {
                sc.close();
            }
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

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        log("[you] " + line);
    }

    private static void log(String msg) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String logMsg = "[" + timestamp + "] " + msg;
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
