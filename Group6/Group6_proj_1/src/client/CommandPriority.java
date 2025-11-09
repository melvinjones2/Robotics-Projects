package client;

/**
 * Command priority levels for the robot control system.
 * Higher priority commands can interrupt and override lower priority commands.
 * 
 * Priority hierarchy (highest to lowest):
 * 1. SAFETY - Asimov's Three Laws enforcement (cannot be overridden)
 * 2. SERVER - Commands from the server/human operator
 * 3. AUTONOMOUS - Client-side autonomous navigation decisions
 * 4. USER - Local user commands (lowest priority)
 */
public enum CommandPriority {
    /**
     * SAFETY priority: Asimov's Three Laws enforcement.
     * - First Law: A robot may not injure a human being or allow a human to come to harm.
     * - Second Law: A robot must obey orders given by humans except where such orders conflict with the First Law.
     * - Third Law: A robot must protect its own existence as long as such protection does not conflict with the First or Second Laws.
     * 
     * Cannot be interrupted or overridden by any other priority.
     */
    SAFETY(4),
    
    /**
     * SERVER priority: Commands issued by the server (human operator).
     * Can interrupt AUTONOMOUS and USER commands.
     * Must still comply with SAFETY (Asimov's Laws).
     */
    SERVER(3),
    
    /**
     * AUTONOMOUS priority: Client-side autonomous navigation.
     * Can interrupt USER commands.
     * Can be overridden by SERVER and SAFETY.
     */
    AUTONOMOUS(2),
    
    /**
     * USER priority: Local user commands.
     * Lowest priority, can be overridden by any other priority.
     */
    USER(1);
    
    private final int level;
    
    CommandPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Check if this priority can interrupt another priority.
     */
    public boolean canInterrupt(CommandPriority other) {
        return this.level > other.level;
    }
    
    /**
     * Check if this priority must yield to another priority.
     */
    public boolean mustYieldTo(CommandPriority other) {
        return this.level < other.level;
    }
}
