package client.network.command;

import client.autonomous.CustomLineMapNavigator;
import client.config.RobotConfig;
import client.motor.DifferentialDrive;
import common.ParsedCommand;
import lejos.hardware.lcd.LCD;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import java.io.IOException;

// Navigate using custom line map with path planning
// Syntax: NAVCUSTOM <startX> <startY> <heading> <endX> <endY>
public class NavCustomMapCommand implements ICommand {

    private final ParsedCommand parsedCmd;
    
    public NavCustomMapCommand(ParsedCommand parsedCmd) {
        this.parsedCmd = parsedCmd;
    }

    @Override
    public String getName() {
        return "NAVCUSTOM";
    }

    @Override
    public void execute(final CommandContext context) throws IOException {
        if (parsedCmd.getArgCount() < 5) {
            sendReply(context, "ERROR: NAVCUSTOM requires 5 parameters");
            sendReply(context, "Usage: NAVCUSTOM <startX> <startY> <heading> <endX> <endY>");
            sendReply(context, "Example: NAVCUSTOM 38.1 67.3 0 71.1 83.8");
            return;
        }
        
        final float startX = parsedCmd.getArgAsFloat(0, Float.NaN);
        final float startY = parsedCmd.getArgAsFloat(1, Float.NaN);
        final float heading = parsedCmd.getArgAsFloat(2, Float.NaN);
        final float endX = parsedCmd.getArgAsFloat(3, Float.NaN);
        final float endY = parsedCmd.getArgAsFloat(4, Float.NaN);
        
        if (Float.isNaN(startX) || Float.isNaN(startY) || Float.isNaN(heading) || 
            Float.isNaN(endX) || Float.isNaN(endY)) {
            sendReply(context, "ERROR: Invalid coordinates or heading");
            return;
        }
        
        if (!context.getDrive().isReady()) {
            sendReply(context, "ERROR: Drive motors not available");
            return;
        }

        sendReply(context, "========== CUSTOM MAP NAVIGATION START ==========");
        sendReply(context, String.format("Start: (%.1f, %.1f) heading %.1f deg", startX, startY, heading));
        sendReply(context, String.format("Goal: (%.1f, %.1f)", endX, endY));
        
        LCD.clear(RobotConfig.LCD_COMMAND_LINE);
        LCD.drawString("NavCustom...", 0, RobotConfig.LCD_COMMAND_LINE);
        
        Thread navThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get robot configuration
                    double wheelDiameter = RobotConfig.WHEEL_DIAMETER_CM;
                    double trackWidth = RobotConfig.TRACK_WIDTH_CM;
                    
                    // Create pilot from context's drive controller
                    // Cast to DifferentialDrive to access motor methods
                    // Note: DifferentialPilot expects (leftMotor, rightMotor) order
                    DifferentialDrive drive = (DifferentialDrive) context.getDrive();
                    DifferentialPilot pilot = new DifferentialPilot(
                        wheelDiameter, 
                        trackWidth, 
                        drive.getLeftMotor(), 
                        drive.getRightMotor()
                    );
                    pilot.setLinearSpeed(RobotConfig.COMMAND_DEFAULT_SPEED / 10.0);
                    pilot.setAngularSpeed(45);
                    pilot.setLinearAcceleration(500);
                    
                    // Create navigator with custom map
                    CustomLineMapNavigator navigator = new CustomLineMapNavigator(pilot);
                    
                    // Create start pose and goal waypoint
                    Pose startPose = new Pose(startX, startY, heading);
                    Waypoint goal = new Waypoint(endX, endY);
                    
                    sendReply(context, "Planning path...");
                    boolean success = navigator.navigateToGoal(startPose, goal);
                    
                    if (success) {
                        sendReply(context, "========== GOAL REACHED ==========");
                        sendReply(context, "NAVCUSTOM:SUCCESS");
                    } else {
                        sendReply(context, "========== NAVIGATION FAILED ==========");
                        sendReply(context, "NAVCUSTOM:FAILED");
                    }
                    
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    LCD.drawString(success ? "NavDone" : "NavFailed", 0, RobotConfig.LCD_COMMAND_LINE);
                    
                } catch (Exception e) {
                    try {
                        sendReply(context, "ERROR: " + e.getMessage());
                        sendReply(context, "NAVCUSTOM:ERROR");
                    } catch (IOException ioException) {
                        // Ignore
                    }
                    LCD.clear(RobotConfig.LCD_COMMAND_LINE);
                    LCD.drawString("NavError", 0, RobotConfig.LCD_COMMAND_LINE);
                }
            }
        }, "navcustom");
        
        navThread.start();
        sendReply(context, "Navigation started in background");
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write("REPLY: " + message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
