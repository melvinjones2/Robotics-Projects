package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        LogManager.rotateLogs();
        ServerGUI gui = new ServerGUI();
        try {
            LogManager.openLog();
            LogManager.log("Server listening on " + port + " ...");
            serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();
            LogManager.log("Client connected: " + clientSocket.getRemoteSocketAddress());

            ClientHandler handler = new ClientHandler(clientSocket, gui, running);
            handler.handle();

        } catch (IOException e) {
            LogManager.log("Server error: " + e.getMessage());
        } finally {
            running.set(false);
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
            LogManager.log("Server closed.");
            LogManager.close();
        }
    }

    public static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        LogManager.log("[you] " + line);
    }

    public static void logAndGui(ServerGUI gui, String prefix, String msg, int skip, boolean debug) {
        LogManager.log(prefix + msg.substring(skip).trim());
        gui.appendLog(msg, debug);
    }
}