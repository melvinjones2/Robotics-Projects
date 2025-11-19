package client.autonomous;

import java.util.ArrayList;
import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Rectangle;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.robotics.pathfinding.ShortestPathFinder;
import lejos.hardware.motor.Motor;

// Custom line map navigator for 46" x 56.5" board with goal zones
public class CustomLineMapNavigator {
    
    private static final float INCH_TO_CM = 2.54f;
    private static final float MAP_WIDTH = 46.0f * INCH_TO_CM;   // 116.84 cm
    private static final float MAP_HEIGHT = 56.5f * INCH_TO_CM;  // 143.51 cm
    
    private final LineMap map;
    private final Navigator navigator;
    private final ShortestPathFinder pathPlanner;
    
    public CustomLineMapNavigator(DifferentialPilot pilot) {
        this.map = createCustomMap();
        this.navigator = new Navigator(pilot);
        this.pathPlanner = new ShortestPathFinder(map);
        
        // Keep robot at least this distance from walls
        float trackWidth = 11.6f;  // Adjust to your robot's track width
        pathPlanner.lengthenLines(trackWidth * 1.2f);
    }
    
    // Create the custom board map with boundaries and center divider
    private LineMap createCustomMap() {
        ArrayList<Line> lines = new ArrayList<>();
        
        // Boundary lines (obstacles)
        lines.add(new Line(0.0f, 0.0f, MAP_WIDTH, 0.0f));           // Bottom
        lines.add(new Line(MAP_WIDTH, 0.0f, MAP_WIDTH, MAP_HEIGHT)); // Right
        lines.add(new Line(MAP_WIDTH, MAP_HEIGHT, 0.0f, MAP_HEIGHT)); // Top
        lines.add(new Line(0.0f, MAP_HEIGHT, 0.0f, 0.0f));           // Left
        
        // Center vertical divider (optional - comment out if robot can cross)
        float centerX = MAP_WIDTH / 2.0f;
        // lines.add(new Line(centerX, 0.0f, centerX, MAP_HEIGHT));
        
        // Note: Tape markers are NOT added as obstacles since they're just floor markings
        
        Line[] lineArr = lines.toArray(new Line[0]);
        return new LineMap(lineArr, new Rectangle(0.0f, 0.0f, MAP_WIDTH, MAP_HEIGHT));
    }
    
    // Get Goal 1 position (top-left)
    public Waypoint getGoal1() {
        float goalSize = 6.0f * INCH_TO_CM;
        float goal1_x = 12.0f * INCH_TO_CM;
        float goal1_y = MAP_HEIGHT - (2.5f * 12.0f * INCH_TO_CM) - goalSize;
        float centerX = goal1_x + (goalSize / 2.0f);
        float centerY = goal1_y + (goalSize / 2.0f);
        return new Waypoint(centerX, centerY);
    }
    
    // Get Goal 2 position (bottom-right)
    public Waypoint getGoal2() {
        float goalSize = 6.0f * INCH_TO_CM;
        float goal2_x = MAP_WIDTH - 12.0f * INCH_TO_CM - goalSize;
        float goal2_y = 2.5f * 12.0f * INCH_TO_CM;
        float centerX = goal2_x + (goalSize / 2.0f);
        float centerY = goal2_y + (goalSize / 2.0f);
        return new Waypoint(centerX, centerY);
    }
    
    // Navigate from start pose to goal with path planning
    public boolean navigateToGoal(Pose startPose, Waypoint goal) {
        try {
            Path path = pathPlanner.findRoute(startPose, goal);
            
            if (path == null || path.isEmpty()) {
                System.out.println("No path found!");
                return false;
            }
            
            System.out.println("Path found with " + path.size() + " waypoints");
            navigator.setPath(path);
            navigator.singleStep(true);
            
            while (!navigator.getPath().isEmpty()) {
                Waypoint wp = navigator.getPath().get(0);
                navigator.followPath();
                navigator.waitForStop();
                System.out.println("Reached waypoint: (" + 
                    Math.round(wp.x) + "," + Math.round(wp.y) + ")");
                
                // Check for obstacles here if needed
                // If obstacle detected:
                // navigator.clearPath();
                // navigator.stop();
                // return false;
            }
            
            navigator.stop();
            System.out.println("Goal reached!");
            return true;
            
        } catch (DestinationUnreachableException e) {
            System.out.println("Destination unreachable: " + e.getMessage());
            return false;
        }
    }
    
    // Navigate from Goal 1 to Goal 2
    public boolean navigateGoal1ToGoal2(float startHeading) {
        Waypoint goal1 = getGoal1();
        Waypoint goal2 = getGoal2();
        Pose startPose = new Pose(goal1.x, goal1.y, startHeading);
        
        System.out.println("Navigating from Goal 1 to Goal 2");
        System.out.println("Start: (" + goal1.x + ", " + goal1.y + ")");
        System.out.println("End: (" + goal2.x + ", " + goal2.y + ")");
        
        return navigateToGoal(startPose, goal2);
    }
    
    // Navigate from Goal 2 to Goal 1
    public boolean navigateGoal2ToGoal1(float startHeading) {
        Waypoint goal2 = getGoal2();
        Waypoint goal1 = getGoal1();
        Pose startPose = new Pose(goal2.x, goal2.y, startHeading);
        
        System.out.println("Navigating from Goal 2 to Goal 1");
        System.out.println("Start: (" + goal2.x + ", " + goal2.y + ")");
        System.out.println("End: (" + goal1.x + ", " + goal1.y + ")");
        
        return navigateToGoal(startPose, goal1);
    }
    
    public Navigator getNavigator() {
        return navigator;
    }
    
    public LineMap getMap() {
        return map;
    }
}
