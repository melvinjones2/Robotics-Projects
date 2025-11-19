

import java.util.ArrayList;

import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Rectangle;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.robotics.pathfinding.ShortestPathFinder;
import lejos.utility.Delay;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.*;
import lejos.hardware.port.MotorPort;

public class TestLineMap {


	public static void main(String[] args) {
		
		TestLineMap tester = new TestLineMap();
		LineMap map = tester.initialize();
		
		double diam = DifferentialPilot.WHEEL_SIZE_NXT1;  // change this accordingly, unit is cm
		double trackWidth = 11.6; // change this accordingly, unit is cm

		// Switch these two motor ports accordingly
		DifferentialPilot rov3r = new DifferentialPilot(diam, trackWidth, Motor.B, Motor.A);
		rov3r.setLinearSpeed(6);	// 6 cm/s for linear movement
		rov3r.setAngularSpeed(45);	// 45 degree/s for rotation in place
		rov3r.setLinearAcceleration(500);	// gradually increase to the desired linear speed

	    Navigator nav = new Navigator(rov3r);
		
		ShortestPathFinder pathPlanner = new ShortestPathFinder(map);
		pathPlanner.lengthenLines((float)trackWidth * 1.2f);	// min distance away from the line
		
		try {
			Pose start = new Pose(10.0f, 40.0f, 0.0f);
			Waypoint end = new Waypoint(70.0f, 20.0f);
			
			Path path = pathPlanner.findRoute(start, end);
			nav.setPath(path);
			nav.singleStep(true);
			
			while (!nav.getPath().isEmpty()) {
				Waypoint wp = nav.getPath().get(0);
				nav.followPath();
				nav.waitForStop();
				System.out.println("Waypoint: (" + Math.round(wp.x) + "," + Math.round(wp.y) + ")");

				// you can put object detection here, or in another thread/behavior
				// if objected detected: clear the path, stop, and replan with a new LineMap
				// nav.clearPath();
				// nav.stop();
				// break;
	
			}
			
			nav.stop();
			
		} catch (DestinationUnreachableException e) {
			e.printStackTrace();
		}
	}
	
	public LineMap initialize() {
		ArrayList<Line> lines = new ArrayList<>();
		
		lines.add(new Line(0.0f, 0.0f, 100.0f, 0.0f));
		lines.add(new Line(100.0f, 0.0f, 100.0f, 100.0f));
		lines.add(new Line(100.0f, 100.0f, 0.0f, 100.0f));
		lines.add(new Line(0.0f, 100.0f, 0.0f, 0.0f));
		
		lines.add(new Line(20.0f, 0.0f, 20.0f, 20.0f));
		lines.add(new Line(20.0f, 20.0f, 50.0f, 20.0f));
		lines.add(new Line(50.0f, 20.0f, 50.0f, 70.0f));
		lines.add(new Line(50.0f, 50.0f, 65.0f, 50.0f));
		lines.add(new Line(0.0f, 70.0f, 25.0f, 70.0f));

		/*
		     _________________________
		    |		      s2		  |
		    |                         |
		    |______   s1              |
		    |             |           |
		    |             |___    s3  |
		    |  s          |           |
		    |             |           |
		    |     ________|    d      |
		    |    |                    |     
		    |s0__|____________________|
		*/
		
		Line[] lineArr = new Line[lines.size()];
		/*
		for(int i = 0; i < lines.size(); i++) {
			lineArr[i] = lines.get(i);
		}*/
		// or combine the lines above:
		lineArr = lines.toArray(lineArr);
		
		return new LineMap(lineArr, new Rectangle(0.0f, 0.0f , 100.0f, 100.0f));
	}
}
