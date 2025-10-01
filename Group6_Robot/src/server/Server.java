package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
}