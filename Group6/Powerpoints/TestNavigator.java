package motors;

import lejos.hardware.lcd.*;
import lejos.hardware.motor.*;
import lejos.hardware.port.MotorPort;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.*;
import lejos.robotics.pathfinding.Path;
import lejos.utility.Delay;

public class TestNavigator {
  private Navigator nav;
  private PoseProvider pp;
	
  public static void main(String [] args) {
	TestNavigator test = new TestNavigator();
	test.initializeNavigator();
	
	test.setNavigatorPath();
	// Always set the flag after setting the path
	//boolean singleStepFlag = test.setSingleStepMode();
	test.testMoves(test.setSingleStepMode());
	
	test.stop();
  }
  
  /*
   * Initialize the navigator.
   */
  public void initializeNavigator() {
	// It seems the Navigator is not compatible with MovePilot by default config: the reason is unknown
    // So we will use DifferentialPilot instead, though it's deprecated
    // Even with the fact that DifferentialPilot will no longer be further supported,
    // we can still use it.
    // Pay attention: the unit for DifferentialPilot is centimeter (cm)
	double diam = DifferentialPilot.WHEEL_SIZE_NXT1;  // change this accordingly, unit is cm
	double trackwidth = 11.6; // change this accordingly, unit is cm

	// Switch these two motor ports accordingly
	DifferentialPilot rov3r = new DifferentialPilot(diam, trackwidth, Motor.B, Motor.A);
	rov3r.setLinearSpeed(6);	// 6 cm/s for linear movement
	rov3r.setAngularSpeed(45);	// 45 degree/s for rotation in place
	rov3r.setLinearAcceleration(500);	// gradually increase to the desired linear speed

    nav = new Navigator(rov3r);
    
    // Default pose provider: OdometryPoseProvider, good at measuring travelled distance
 	pp = nav.getPoseProvider();
  }
  
  /*
   * Play a short notification sound.
   */
  public void playNotificationSound() {
	Sound.playTone(400, 100, 10);
  }
  
  /*
   * Set a path for the navigator
   */
  public Path setNavigatorPath() {
	Path path = new Path(); // (0, 0)
    path.add(new Waypoint(20, 0));
    path.add(new Waypoint(20, 20));
    path.add(new Waypoint(0, 20));
    path.add(new Waypoint(0, 0, 0));
    
    // setPath will reset necessary control parameters
    nav.setPath(path); 
    // setting single step should be after setPath()
    return path;
  }
  
  /*
   * Get the single step configuration from the user.
   */
  public boolean setSingleStepMode() {
	playNotificationSound();
	boolean singleStepFlag = false;
    LCD.drawString("SingleStep mode:", 0, 0);
    LCD.drawString("Enter for true", 0, 1);
    LCD.drawString("Other for false", 0, 2);
    
    int id = Button.waitForAnyPress();	// once clicked, the button id will be returned
    if (id == Button.ID_ENTER) {
      singleStepFlag = true;
      LCD.drawString("SingleStep enabled.", 0, 3);
    }
    Delay.msDelay(3000);
    LCD.clear();
    System.out.println("SingleStep:" + singleStepFlag);
    
    nav.singleStep(singleStepFlag); 
    
    return singleStepFlag;
  }
  
  /*
   * Once the nav is ready (the path and flag are set), we can test the moves
   * If in single step mode, call followPath() for every waypoint.
   */
  public void testMoves(boolean singleStepFlag) {
	// To make things consistent, we can always use followPath() to navigate the robot
	// And call waitforStop(): yield to the navigator thread to finish the path
	// This is because goTo() will directly call followPath() every time, 
	// so goTo() will actually finish the entire path, not just a single waypoint 
	// If singleStep is true, followPath() will only move one waypoint and wait for the next call
	// Otherwise followPath() will finish entire path
	Path path = nav.getPath();
	if (singleStepFlag) { 
	  while (!path.isEmpty()) { 
	    // With single step mode, every call of followPath() will move to the next waypoint
	    // Once a waypoint is finished, it will be removed from the list/path
	    // Single step mode: stop at each waypoint -- call followPath() for every waypoint
	    nav.followPath(); 
	    nav.waitForStop();
	    displayPose();
	  } // Once the path is finished, single step is reset to false
		  
      // A further test: what's the current single step mode?
	  testSingleStepAfterwards();      
	}
	else { // finish entire path all at once
	  nav.followPath();
	  nav.waitForStop();
	  displayPose();
    }
  }
  
  public void testSingleStepAfterwards() {
	LCD.clear();
	System.out.println("Further test?");
	System.out.println("Enter for Yes:");
	int id = Button.waitForAnyPress();
	if (id != Button.ID_ENTER) {
		return;
	}
	Path path = nav.getPath();
	nav.addWaypoint(30, 30);
    path.add(new Waypoint(0, 30));	// To showcase different ways to control the path
    nav.followPath();
    nav.waitForStop();
    displayPose();
  }
  
  /*
   * Output the current pose to the standard output.
   */
  public void displayPose() {
	Pose pose = pp.getPose(); 
	// The pose provider will provide the estimated current pose
	// When a move is finished, the pose provider will be notified with the new pose
	System.out.println("Position: (" + pose.getX() + ", " + pose.getY() + ")");
	playNotificationSound();
  }
  
  /*
   * Stop the test.
   */
  public void stop() {
	nav.stop();
  }
  
}
