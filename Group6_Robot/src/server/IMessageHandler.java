package server;

import java.io.BufferedWriter;
import java.io.IOException;

public interface IMessageHandler {
    void handle(String msg, BufferedWriter out) throws IOException;
}