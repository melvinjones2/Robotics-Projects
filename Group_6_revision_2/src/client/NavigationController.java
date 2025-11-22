package client;

import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Waypoint;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import shared.SocketConnection;
import java.io.IOException;

public class NavigationController {
	private MovePilot pilot;
	private Navigator navigator;
	private EV3UltrasonicSensor ultrasonicSensor;
	private SocketConnection connection;
	private SampleProvider distanceProvider;
	private float[] sample;
	
	private static final double OBSTACLE_THRESHOLD = 20.0; // cm
	private static final double SAFE_DISTANCE = 30.0; // cm
	
	public NavigationController(MovePilot pilot, EV3UltrasonicSensor ultrasonicSensor, SocketConnection connection) {
		this.pilot = pilot;
		this.navigator = new Navigator(pilot);
		this.ultrasonicSensor = ultrasonicSensor;
		this.connection = connection;
		
		if (ultrasonicSensor != null) {
			this.distanceProvider = ultrasonicSensor.getDistanceMode();
			this.sample = new float[distanceProvider.sampleSize()];
		}
	}
	
	// Strategy 1: Simple Obstacle Avoidance
	public void simpleAvoidanceMove(double distance) throws IOException {
		log("Starting simple avoidance move: " + distance + " cm");
		
		double traveled = 0;
		while (traveled < Math.abs(distance)) {
			double distanceToObstacle = getDistance();
			
			if (distanceToObstacle < OBSTACLE_THRESHOLD) {
				log("Obstacle detected at " + distanceToObstacle + " cm - avoiding");
				pilot.stop();
				
				// Back up
				pilot.travel(-10);
				
				// Turn to avoid obstacle
				pilot.rotate(90);
				
				// Move around obstacle
				pilot.travel(20);
				pilot.rotate(-90);
			} else {
				// Move forward in small increments
				double moveDistance = Math.min(10, Math.abs(distance) - traveled);
				pilot.travel(distance > 0 ? moveDistance : -moveDistance);
				traveled += moveDistance;
			}
		}
		
		log("Simple avoidance move complete");
	}
	
	// Strategy 2: Waypoint Navigation with Obstacle Checking
	public void navigateToGoal(double targetX, double targetY) throws IOException {
		log("Navigating to goal: (" + targetX + ", " + targetY + ")");
		
		navigator.addWaypoint(new Waypoint(targetX, targetY));
		
		while (navigator.isMoving()) {
			double distanceToObstacle = getDistance();
			
			if (distanceToObstacle < OBSTACLE_THRESHOLD) {
				log("Obstacle detected - recalculating path");
				navigator.stop();
				
				// Simple avoidance: move to side waypoint
				double currentX = navigator.getPoseProvider().getPose().getX();
				double currentY = navigator.getPoseProvider().getPose().getY();
				
				// Add detour waypoint
				navigator.addWaypoint(new Waypoint(currentX + 20, currentY + 20));
				navigator.addWaypoint(new Waypoint(targetX, targetY));
			}
			
			try {
				Thread.sleep(200); // Check every 200ms
			} catch (InterruptedException e) {
				break;
			}
		}
		
		log("Navigation complete");
	}
	
	// Strategy 3: Continuous Scanning and Dynamic Path Adjustment
	public void dynamicNavigationMove(double distance) throws IOException {
		log("Starting dynamic navigation: " + distance + " cm");
		
		pilot.travel(distance, true); // Non-blocking move
		
		while (pilot.isMoving()) {
			double distanceToObstacle = getDistance();
			
			if (distanceToObstacle < OBSTACLE_THRESHOLD) {
				log("Obstacle detected at " + distanceToObstacle + " cm - taking evasive action");
				pilot.stop();
				
				// Scan for best direction
				double bestAngle = findBestDirection();
				log("Best direction found at " + bestAngle + " degrees");
				
				// Turn to best direction
				pilot.rotate(bestAngle);
				
				// Continue moving
				double remaining = distance - pilot.getMovement().getDistanceTraveled();
				pilot.travel(remaining, true);
			}
			
			try {
				Thread.sleep(100); // Check every 100ms
			} catch (InterruptedException e) {
				break;
			}
		}
		
		log("Dynamic navigation complete");
	}
	
	// Scan environment to find direction with most clearance
	private double findBestDirection() throws IOException {
		double[] angles = {-90, -45, 0, 45, 90};
		double maxDistance = 0;
		double bestAngle = 0;
		
		log("Scanning for best direction...");
		
		for (double angle : angles) {
			pilot.rotate(angle - bestAngle); // Rotate to scan angle
			double distance = getDistance();
			log("Distance at " + angle + " degrees: " + distance + " cm");
			
			if (distance > maxDistance) {
				maxDistance = distance;
				bestAngle = angle;
			}
		}
		
		// Return to best angle
		pilot.rotate(bestAngle - angles[angles.length - 1]);
		
		return 0; // Already rotated to best angle
	}
	
	// Get distance from ultrasonic sensor
	private double getDistance() {
		if (distanceProvider == null) {
			return 100; // Default safe distance if no sensor
		}
		
		distanceProvider.fetchSample(sample, 0);
		float distance = sample[0] * 100; // Convert meters to cm
		
		// Handle invalid readings
		if (Float.isInfinite(distance) || distance > 255) {
			return 255; // Max sensor range
		}
		
		return distance;
	}
	
	// Get Navigator for advanced use
	public Navigator getNavigator() {
		return navigator;
	}
	
	// Helper to log messages to server
	private void log(String message) {
		try {
			if (connection != null) {
				connection.sendLog(message);
			}
		} catch (IOException e) {
			// Silent fail
		}
	}
}
