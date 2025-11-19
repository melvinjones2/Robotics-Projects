package client.autonomous;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Abstract base class for autonomous tasks that run in a separate thread.
 */

public abstract class AutonomousTask {
    
    private final String taskName;
    private final BufferedWriter out;
    
    private volatile boolean running = false;
    private Thread taskThread = null;
    
    /**
     * Create an autonomous task.
     */
    protected AutonomousTask(String taskName, BufferedWriter out) {
        this.taskName = taskName;
        this.out = out;
    }
    
    /**
     * Start the autonomous task in a separate thread.
     */
    public synchronized boolean start() {
        if (running) {
            log("Task already running: " + taskName);
            return false;
        }
        
        running = true;
        
        try {
            onStart();
        } catch (Exception e) {
            log("Error in onStart(): " + e.getMessage());
            running = false;
            return false;
        }
        
        taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                taskLoop();
            }
        }, taskName + "-thread");
        
        taskThread.start();
        log("Task started: " + taskName);
        
        return true;
    }
    
    /**
     * Stop the autonomous task gracefully.
     * Waits for the task thread to finish (with timeout).
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        
        log("Stopping task: " + taskName);
        running = false;
        
        // Wait for thread to finish
        if (taskThread != null) {
            try {
                taskThread.join(2000); // 2 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("Task stop interrupted: " + taskName);
            }
            taskThread = null;
        }
        
        try {
            onStop();
        } catch (Exception e) {
            log("Error in onStop(): " + e.getMessage());
        }
        
        log("Task stopped: " + taskName);
    }
    
    /**
     * Check if the task is currently running.
     * 
     * @return true if task is running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get the task name.
     * 
     * @return task name
     */
    public String getTaskName() {
        return taskName;
    }
    
    /**
     * Main task loop - runs in separate thread.
     * Calls execute() repeatedly while running flag is true.
     */
    private void taskLoop() {
        log("Task loop starting: " + taskName);
        
        try {
            execute();
        } catch (Exception e) {
            log("Task error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            running = false;
            log("Task loop ending: " + taskName);
        }
    }
    
    protected abstract void execute();
    
    protected void onStart() {
        // Default: do nothing
    }
    
    protected void onStop() {
        // Default: do nothing
    }
    
    protected boolean shouldContinue() {
        return running;
    }
    
    protected void log(String message) {
        if (out != null) {
            try {
                out.write("[" + taskName + "] " + message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                // Logging failed - can't do much about it
                System.err.println("Log error: " + e.getMessage());
            }
        }
    }
    
    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
