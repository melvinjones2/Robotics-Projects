package client;

public interface ISensor {
    String getName(); // Returns sensor name (e.g., "ultrasonic", "touch", "light", "gyro")
    String readValue(); // Returns formatted sensor data string, or null if not available
    void close();
}