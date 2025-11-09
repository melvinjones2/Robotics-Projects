package client.motor;

import client.config.RobotConfig;

/**
 * Encapsulates all parameters for a movement command.
 * Provides validation and immutability for movement operations.
 */
public class MovementParameters {
    public enum Direction {
        FORWARD, BACKWARD, LEFT, RIGHT, STOP
    }
    
    private final Direction direction;
    private final int speed;
    private final char port; // 'A', 'B', 'C', 'D', or '*' for all
    private final int distance; // in degrees/rotations, -1 for continuous
    private final boolean immediateReturn; // true for non-blocking
    
    private MovementParameters(Builder builder) {
        this.direction = builder.direction;
        this.speed = builder.speed;
        this.port = builder.port;
        this.distance = builder.distance;
        this.immediateReturn = builder.immediateReturn;
    }
    
    public Direction getDirection() { return direction; }
    public int getSpeed() { return speed; }
    public char getPort() { return port; }
    public int getDistance() { return distance; }
    public boolean isImmediateReturn() { return immediateReturn; }
    public boolean isAllMotors() { return port == '*'; }
    
    @Override
    public String toString() {
        return String.format("Movement[dir=%s, speed=%d, port=%c, dist=%d]", 
                           direction, speed, port, distance);
    }
    
    public static class Builder {
        private Direction direction = Direction.FORWARD;
        private int speed = RobotConfig.DEFAULT_MOTOR_SPEED;
        private char port = '*'; // Default to all motors
        private int distance = -1; // Default to continuous
        private boolean immediateReturn = true;
        
        public Builder direction(Direction direction) {
            this.direction = direction;
            return this;
        }
        
        public Builder speed(int speed) {
            if (speed < RobotConfig.MIN_MOTOR_SPEED || speed > RobotConfig.MAX_MOTOR_SPEED) {
                throw new IllegalArgumentException("Speed must be between " + 
                    RobotConfig.MIN_MOTOR_SPEED + " and " + RobotConfig.MAX_MOTOR_SPEED);
            }
            this.speed = speed;
            return this;
        }
        
        public Builder port(char port) {
            port = Character.toUpperCase(port);
            if (port != '*' && port != 'A' && port != 'B' && port != 'C' && port != 'D') {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
            this.port = port;
            return this;
        }
        
        public Builder distance(int distance) {
            this.distance = distance;
            return this;
        }
        
        public Builder immediateReturn(boolean immediateReturn) {
            this.immediateReturn = immediateReturn;
            return this;
        }
        
        public MovementParameters build() {
            return new MovementParameters(this);
        }
    }
}
