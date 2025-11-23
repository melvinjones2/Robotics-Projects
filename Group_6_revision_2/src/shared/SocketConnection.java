package shared;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import static shared.Constants.*;

public class SocketConnection implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    
    public static SocketConnection createClient(String host, int port, int timeoutMs) throws IOException {
        Socket socket = new Socket(host, port);
        if (timeoutMs > 0) {
            socket.setSoTimeout(timeoutMs);
        }
        return new SocketConnection(socket);
    }
    
    public static SocketConnection createClient() throws IOException {
        // No timeout - client waits indefinitely for commands
        return createClient(DEFAULT_HOST, DEFAULT_PORT, 0);
    }
    
    public static SocketConnection createServer(int port) throws IOException {
        Socket clientSocket;
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new java.net.InetSocketAddress(port));
            clientSocket = serverSocket.accept();
        }
        return new SocketConnection(clientSocket);
    }
    
    public static SocketConnection createServer() throws IOException {
        return createServer(DEFAULT_PORT);
    }
    
    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
    
    public String readLine() throws IOException {
        return reader.readLine();
    }
    
    public void sendLine(String message) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
    }
    
    public void sendLog(String logMessage) throws IOException {
        sendLine(LOG_PREFIX + " " + logMessage);
    }
    
    public boolean isLogMessage(String message) {
        return message != null && message.startsWith(LOG_PREFIX);
    }
    
    public String extractLogMessage(String message) {
        if (isLogMessage(message)) {
            return message.substring(LOG_PREFIX.length()).trim();
        }
        return message;
    }
    
    public BufferedReader getReader() {
        return reader;
    }
    
    public BufferedWriter getWriter() {
        return writer;
    }
    
    @Override
    public void close() {
        try { if (writer != null) writer.close(); } catch (IOException e) { System.err.println(LOG_PREFIX + " Failed to close writer: " + e.getMessage()); }
        try { if (reader != null) reader.close(); } catch (IOException e) { System.err.println(LOG_PREFIX + " Failed to close reader: " + e.getMessage()); }
        try { if (socket != null) socket.close(); } catch (IOException e) { System.err.println(LOG_PREFIX + " Failed to close socket: " + e.getMessage()); }
    }

    public Socket getSocket() {
        return socket;
    }


}
