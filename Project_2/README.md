# Robotics Project 2 - Soccer Robot 🤖⚽

This is our code for Project 2. It controls a Lego EV3 robot to play soccer (attacker and defender modes). The system uses a PC server to handle the heavy logic and vision processing, while the robot client handles the motors and sensors.

## 📂 Project Structure

*   **`ServerGUI.java`**: The main control center. Run this on your laptop. It shows the map, camera feed, and lets you control the robot.
*   **`RobotClient.java`**: The code that runs on the EV3 brick. It listens for commands and sends back sensor data.
*   **`VisionService.java`**: Handles the image processing to find the ball using OpenCV.
*   **`RobotController.java`**: A helper class to manage the EV3 hardware (motors, sensors).

## 🚀 How to Run

### 1. Setup the PC (Server)
1.  Open the project in Eclipse/VS Code.
2.  Make sure you have the **OpenCV** libraries linked correctly.
3.  Run `ServerGUI.java`.
4.  A window should pop up showing the map and camera view.

### 2. Setup the Robot (Client)
1.  Make sure the EV3 is connected to the same Wi-Fi as your PC.
2.  Check `RobotClient.java` and update the `SERVER_IP` to match your computer's IP address.
3.  Upload and run `RobotClient` on the EV3.

### 3. Playing
*   Once both are running, the GUI status should say **"Connected"**.
*   **Manual Mode**: Click the "Manual Control" toggle. Use **WASD** to drive, **Space** to stop, and **K** to kick.
*   **Autonomous Mode**: Select your Team (Blue/Green) and Role (Attacker/Defender), then click **Start Match**.

## 🎮 Controls (Manual Mode)

*   **W / S**: Forward / Backward
*   **A / D**: Turn Left / Right
*   **Space**: Emergency Stop
*   **K**: Kick
*   **U / J**: Move Arm Up / Down

## 📝 Notes
*   **Safety First**: The robot has a safety feature where it automatically stops if the ultrasonic sensor sees an obstacle closer than **15cm**.
*   **Camera**: If the camera feed isn't working, check the URL in the text box on the GUI. It usually looks like `http://<PHONE_IP>:4747/video`.
*   **Map**: You can flip the map view using the checkbox if the robot is on the other side of the field.

---
*Created for Intro to Robotics, Fall 2025.*
