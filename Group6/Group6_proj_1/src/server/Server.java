package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import server.client.ClientHandler;
import server.gui.ServerGUI;
import server.logging.LogLevel;
import server.logging.LogManager;

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
            LogManager.setLogLevel(LogLevel.INFO); // Set desired log level
            LogManager.info("Server listening on port " + port);
            serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();
            LogManager.info("Client connected: " + clientSocket.getRemoteSocketAddress());

            ClientHandler handler = new ClientHandler(clientSocket, gui, running);
            handler.handle();

        } catch (IOException e) {
            LogManager.error("Server error", e);
        } finally {
            running.set(false);
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
            LogManager.info("Server shutdown complete");
            LogManager.close();
        }
    }

    public static void send(BufferedWriter out, String line) throws IOException {
        out.write(line);
        out.write("\n");
        out.flush();
        LogManager.debug("[you] " + line);
    }

    public static void logAndGui(ServerGUI gui, String prefix, String msg, int skip, boolean debug) {
        String logMsg = prefix + msg.substring(skip).trim();
        if (debug) {
            LogManager.debug(logMsg);
        } else {
            LogManager.info(logMsg);
        }
        gui.appendLog(msg, debug);
    }
}