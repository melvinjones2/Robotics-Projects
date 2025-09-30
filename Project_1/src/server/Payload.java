package server;

import java.io.BufferedWriter;

public interface Payload {

    String getName();

    void start();

    void stop();

    void handleMessage(String msg, BufferedWriter out, ServerGUI gui);
}
