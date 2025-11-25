import org.opencv.core.*;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import java.awt.Graphics2D;

public class VisionService implements Runnable {

    private VideoCapture camera;
    private volatile Point ballPosition = null; // Relative to camera center
    private volatile double ballDistance = -1.0;
    private volatile BufferedImage currentFrame = null;
    private boolean running = true;
    
    // Fallback for when OpenCV VideoCapture fails
    private boolean useJavaNetworkStream = false;
    private String streamUrlString;

    // Configuration
    private static final double KNOWN_WIDTH = 5.0; // cm
    private static final double FOCAL_LENGTH = 600.0; // Calibrate this!
    
    // HSV Tuning
    public volatile int hMin = 0, sMin = 160, vMin = 140;
    public volatile int hMax = 10, sMax = 255, vMax = 255;

    public VisionService(int cameraIndex) {
        // Load library must be done in main, but we can check here
        camera = new VideoCapture(cameraIndex);
    }

    public VisionService(String streamAddress) {
        this.streamUrlString = streamAddress;
        // Open video stream from URL (e.g. http://192.168.1.100:8080/video)
        System.out.println("Attempting to connect to IP Camera at: " + streamAddress);
        
        // 1. Network Pre-check
        try {
            java.net.URL url = new java.net.URL(streamAddress);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            System.out.println("Network Check: Server returned HTTP " + code + " (This is good!)");
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("Network Check Failed: Could not reach " + streamAddress);
            System.out.println("Error: " + e.getMessage());
            System.out.println("-> Please check if the IP is correct and the phone is on the same Wi-Fi.");
        }

        // 2. OpenCV Connection Strategy
        // Strategy A: Direct Connection
        camera = new VideoCapture(streamAddress);
        
        if (!camera.isOpened()) {
            System.out.println("Strategy A failed. Trying Strategy B (Append .mjpg extension)...");
            // Strategy B: Append dummy extension to help OpenCV recognize MJPEG
            String altUrl = streamAddress;
            if (streamAddress.contains("?")) altUrl += "&dummy=param.mjpg";
            else altUrl += "?dummy=param.mjpg";
            
            camera = new VideoCapture(altUrl);
        }
        
        if (!camera.isOpened()) {
            System.out.println("Strategy B failed. Trying Strategy C (DroidCam /mjpegfeed)...");
            // Strategy C: Try /mjpegfeed if user used /video
            if (streamAddress.contains("/video")) {
                String altUrl = streamAddress.replace("/video", "/mjpegfeed?640x480");
                camera = new VideoCapture(altUrl);
            }
        }
        
        if (!camera.isOpened()) {
            System.out.println("All OpenCV Strategies failed. Switching to Java MJPEG Streamer (Fallback).");
            useJavaNetworkStream = true;
        }
    }

    public Point getBallPosition() { return ballPosition; }
    public double getBallDistance() { return ballDistance; }
    public BufferedImage getCurrentFrame() { return currentFrame; }

    public void stop() { running = false; }

    @Override
    public void run() {
        if (useJavaNetworkStream) {
            runJavaStream();
        } else {
            runOpenCVStream();
        }
    }
    
    private void runOpenCVStream() {
        if (!camera.isOpened()) {
            System.out.println("Vision Error: Camera not found!");
            return;
        }

        Mat frame = new Mat();
        
        while (running) {
            if (camera.read(frame)) {
                processFrame(frame);
            }
            try { Thread.sleep(30); } catch (InterruptedException e) {}
        }
        camera.release();
    }
    
    private void runJavaStream() {
        System.out.println("Starting Java MJPEG Streamer...");
        
        // Try the modified URL first, then the original
        String[] urlsToTry = new String[2];
        String modifiedUrl = streamUrlString;
        if (modifiedUrl.contains("/video")) {
            modifiedUrl = modifiedUrl.replace("/video", "/mjpegfeed?640x480");
        }
        urlsToTry[0] = modifiedUrl;
        urlsToTry[1] = streamUrlString; // Fallback to original

        for (int i = 0; i < urlsToTry.length; i++) {
            String targetUrl = urlsToTry[i];
            if (targetUrl == null) continue;
            // Skip duplicate if modified is same as original (e.g. user already provided mjpegfeed)
            if (i == 1 && targetUrl.equals(urlsToTry[0])) continue;

            System.out.println("Java Stream: Connecting to " + targetUrl);
            InputStream in = null;
            HttpURLConnection conn = null;
            int framesDecoded = 0;
            
            try {
                URL url = new URL(targetUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000); // 10s read timeout
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                try {
                    int responseCode = conn.getResponseCode();
                    System.out.println("Java Stream: Response Code: " + responseCode);
                    if (responseCode != 200) {
                        System.out.println("Java Stream: Failed with code " + responseCode);
                        continue; 
                    }
                } catch (Exception e) {
                    System.out.println("Java Stream: Could not get response code, trying to read anyway...");
                }

                in = new BufferedInputStream(conn.getInputStream());
                
                ByteArrayOutputStream jpgBuffer = new ByteArrayOutputStream();
                boolean recording = false;
                int prev = 0;
                int cur = 0;
                
                while (running) {
                    cur = in.read();
                    if (cur == -1) {
                        System.out.println("Java Stream: End of Stream (Server closed connection)");
                        break;
                    }
                    
                    if (recording) {
                        jpgBuffer.write(cur);
                        if (prev == 0xFF && cur == 0xD9) { // End of Image
                            recording = false;
                            byte[] imgBytes = jpgBuffer.toByteArray();
                            try {
                                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imgBytes));
                                if (bi != null) {
                                    processFrame(bufferedImageToMat(bi));
                                    framesDecoded++;
                                    if (framesDecoded == 1) System.out.println("Java Stream: First frame decoded successfully!");
                                }
                            } catch (Exception e) {
                                // System.out.println("Frame Decode Error: " + e.getMessage());
                            }
                            jpgBuffer.reset();
                        }
                    } else {
                        if (prev == 0xFF && cur == 0xD8) { // Start of Image
                            recording = true;
                            jpgBuffer.write(prev);
                            jpgBuffer.write(cur);
                        }
                    }
                    prev = cur;
                }
                
                // If we decoded frames, we consider this a "good" URL that just disconnected.
                // If we didn't decode any frames, it's a "bad" URL, so we continue to the next one.
                if (framesDecoded > 0) {
                    System.out.println("Java Stream: Stream disconnected after " + framesDecoded + " frames.");
                    // If user didn't stop it, maybe we should retry the SAME url? 
                    // For now, let's just break, or maybe loop back to start?
                    // If we break here, the thread ends.
                    if (running) {
                         System.out.println("Java Stream: Attempting to reconnect...");
                         i--; // Retry this same URL
                         try { Thread.sleep(1000); } catch (Exception e) {}
                         continue;
                    }
                }
                
            } catch (Exception e) {
                System.out.println("Java Stream Error on " + targetUrl + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                try { if (in != null) in.close(); } catch (Exception e) {}
                try { if (conn != null) conn.disconnect(); } catch (Exception e) {}
            }
        }
        System.out.println("Java Stream: Connection Closed.");
    }
    
    private Mat bufferedImageToMat(BufferedImage bi) {
        // Convert to 3BYTE_BGR if not already
        if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage newImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = newImg.createGraphics();
            g.drawImage(bi, 0, 0, null);
            g.dispose();
            bi = newImg;
        }
        
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    private void processFrame(Mat frame) {
        try {
            // Rotate frame 90 degrees clockwise (Landscape -> Portrait)
            Core.transpose(frame, frame);
            Core.flip(frame, frame, 1);

            Mat hsvImage = new Mat();
            Mat mask1 = new Mat();
            Mat mask2 = new Mat();
            Mat finalMask = new Mat();
            Mat hierarchy = new Mat();

            // 1. Convert to HSV
            Imgproc.cvtColor(frame, hsvImage, Imgproc.COLOR_BGR2HSV);
            
            // Threshold
            if (hMin > hMax) {
                 Core.inRange(hsvImage, new Scalar(hMin, sMin, vMin), new Scalar(180, sMax, vMax), mask1);
                 Core.inRange(hsvImage, new Scalar(0, sMin, vMin), new Scalar(hMax, sMax, vMax), mask2);
                 Core.addWeighted(mask1, 1.0, mask2, 1.0, 0.0, finalMask);
            } else {
                 Core.inRange(hsvImage, new Scalar(hMin, sMin, vMin), new Scalar(hMax, sMax, vMax), finalMask);
            }

            // 2. Find Contours
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(finalMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Point closestCenter = null;
            double maxArea = 0;
            double calculatedDist = -1;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 1000) {
                    float[] radius = new float[1];
                    Point center = new Point();
                    MatOfPoint2f floatContour = new MatOfPoint2f(contour.toArray());
                    Imgproc.minEnclosingCircle(floatContour, center, radius);
                    
                    // Filter: Ignore detections at the bottom 20% of the frame (Robot Body/Sensor)
                    if (center.y > frame.rows() * 0.80) {
                        continue;
                    }

                    if (area > maxArea) {
                        maxArea = area;
                        closestCenter = center;
                        
                        // Calculate Distance
                        double pixelWidth = radius[0] * 2;
                        calculatedDist = (KNOWN_WIDTH * FOCAL_LENGTH) / pixelWidth;
    
                        // Draw on frame for debug
                        Core.circle(frame, center, (int)radius[0], new Scalar(0, 255, 0), 2);
                        Core.putText(frame, String.format("%.1f cm", calculatedDist), new Point(center.x, center.y - 20), 
                            Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 255), 2);
                    }
                }
            }

            // Update volatile state
            this.ballPosition = closestCenter;
            this.ballDistance = calculatedDist;
            this.currentFrame = matToBufferedImage(frame);
            
        } catch (Exception e) {
            System.out.println("Vision Processing Error: " + e.getMessage());
        }
    }

    private BufferedImage matToBufferedImage(Mat m) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}
