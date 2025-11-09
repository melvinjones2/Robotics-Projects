package server;

/**
 * Log levels for filtering and prioritizing log messages.
 */
public enum LogLevel {
    DEBUG(0, "DEBUG"),
    INFO(1, "INFO"),
    WARN(2, "WARN"),
    ERROR(3, "ERROR");
    
    private final int priority;
    private final String label;
    
    LogLevel(int priority, String label) {
        this.priority = priority;
        this.label = label;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String getLabel() {
        return label;
    }
    
    public boolean isEnabled(LogLevel minLevel) {
        return this.priority >= minLevel.priority;
    }
}
