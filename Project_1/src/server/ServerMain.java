package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain {
    private static final int PORT = 9999;

    public static void main(String[] args) {
        System.out.println("Server listening on " + PORT + " ...");
        ServerSocket server = null;
        Socket client = null;
        BufferedWriter out = null;
        BufferedReader in = null;
        final AtomicBoolean running = new AtomicBoolean(true);

        try {
            server = new ServerSocket(PORT);
            client = server.accept();
            System.out.println("Client connected: " + client.getRemoteSocketAddress());

            out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            in  = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // ---- handshake ----
            send(out, "HELLO");
            String resp = in.readLine();
            if (!"READY".equalsIgnoreCase(resp != null ? resp.trim() : "")) {
                System.err.println("Handshake failed, got: " + resp);
                return;
            }
            System.out.println("Handshake OK. Type messages (BEEP/ BYE supported).");

            // ---- reader thread: print anything EV3 sends back ----
            final BufferedReader inRef = in;
            Thread reader = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        String line;
                        while (running.get() && (line = inRef.readLine()) != null) {
                            String msg = line.trim();
                            // Categorize by tag
                            if (msg.startsWith("BATTERY:")) {
                                System.out.println("[EV3][BATTERY] " + msg.substring(8).trim());
                                // TODO: Add battery-specific handling here
                            } else if (msg.startsWith("REPLY:")) {
                                System.out.println("[EV3][REPLY] " + msg.substring(6).trim());
                                // TODO: Add reply-specific handling here
                            } else if (msg.startsWith("CONTROL:")) {
                                System.out.println("[EV3][CONTROL] " + msg.substring(8).trim());
                                // TODO: Add control-specific handling here
                            } else {
                                System.out.println("[EV3][UNKNOWN] " + msg);
                            }
                            if ("BYE".equalsIgnoreCase(msg)) {
                                running.set(false);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        if (running.get()) System.err.println("Read error: " + e.getMessage());
                    }
                }
            }, "server-reader");
            reader.setDaemon(true);
            reader.start();

            // ---- interactive send loop ----
            Scanner sc = new Scanner(System.in);
            try {
                while (running.get() && sc.hasNextLine()) {
                    String line = sc.nextLine();
                    send(out, line);
                    if ("BYE".equalsIgnoreCase(line.trim())) {
                        running.set(false);
                        break;
                    }
                }
            } finally {
                sc.close();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            running.set(false);
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (client != null) client.close(); } catch (IOException ignored) {}
            try { if (server != null) server.close(); } catch (IOException ignored) {}
            System.out.println("Server closed.");
        }
    }

    private static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        System.out.println("[you] " + line);
    }
}
