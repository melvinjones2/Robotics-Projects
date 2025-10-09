package client;

public interface ISensor {
    String readValue(); // Returns formatted sensor data string, or null if not available
    void close();
}