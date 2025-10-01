package client;

public interface IMotorControl {
    void forward(int speed);
    void backward(int speed);
    void stop();
}