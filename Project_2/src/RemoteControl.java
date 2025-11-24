import java.io.*;
import java.net.*;

public class RemoteControl extends Thread {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private volatile String currentCommand = "STOP";
    private boolean running = true;
    private int port;

    public RemoteControl(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(2000); // Check running flag every 2 seconds
            System.out.println("Remote Control: Server started on port " + port);

            while (running) {
                try {
                    if (clientSocket == null || clientSocket.isClosed()) {
                        try {
                            System.out.println("Remote Control: Waiting for client...");
                            clientSocket = serverSocket.accept();
                            System.out.println("Remote Control: Client connected: " + clientSocket.getInetAddress());
                            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        } catch (SocketTimeoutException e) {
                            continue; // Loop back to check running flag
                        }
                    }

                    String line = in.readLine();
                    if (line != null) {
                        currentCommand = line.trim().toUpperCase();
                        // System.out.println("RC Cmd: " + currentCommand);
                    } else {
                        System.out.println("Remote Control: Client disconnected");
                        closeClient();
                    }
                } catch (IOException e) {
                    System.out.println("Remote Control Error: " + e.getMessage());
                    closeClient();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public String getCommand() {
        return currentCommand;
    }
    
    // Used to consume one-time commands like KICK
    public void clearCommand() {
        currentCommand = "STOP";
    }

    private void closeClient() {
        try {
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {}
        clientSocket = null;
        currentCommand = "STOP";
    }

    public void close() {
        running = false;
        closeClient();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {}
    }
}
