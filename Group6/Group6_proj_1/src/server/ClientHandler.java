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
    private final MessageDispatcher dispatcher;

    public ClientHandler(Socket client, ServerGUI gui, AtomicBoolean running) {
        this.client = client;
        this.gui = gui;
        this.running = running;
        this.dispatcher = new MessageDispatcher(gui, running, frameCount);
    }

    public void handle() {
        try (
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream())); BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            gui.setupMainWindow(out, frameCount, running);

            Server.send(out, "HELLO");
            String resp = in.readLine();
            int clientFrame = handshake(resp);
            if (clientFrame == -1) {
                return;
            }

            LogManager.log("Handshake OK. Type messages (BEEP/ BYE supported).");

            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    readLoop(in, out);
                }
            }, "server-reader");
            reader.setDaemon(true);
            reader.start();

            reader.join();
        } catch (IOException | InterruptedException e) {
            LogManager.log("Handler error: " + e.getMessage());
        } finally {
            running.set(false);
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private int handshake(String resp) throws IOException {
        if (resp != null && resp.trim().startsWith("READY:")) {
            try {
                return Integer.parseInt(resp.trim().split(":")[1]);
            } catch (NumberFormatException e) {
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
                dispatcher.dispatch(line.trim(), out);
            }
        } catch (IOException e) {
            if (running.get()) {
                LogManager.log("Read error: " + e.getMessage());
            }
        }
    }
}