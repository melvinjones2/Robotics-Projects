package client.commands;

/**
 * Metadata for a command including name, aliases, category, and help text.
 * Used by CommandRegistry for self-documenting commands.
 */
public class CommandMetadata {
    private final String name;
    private final String[] aliases;
    private final String category;
    private final String helpText;
    private final ICommand handler;
    
    public CommandMetadata(String name, ICommand handler, String category, String helpText, String... aliases) {
        this.name = name;
        this.handler = handler;
        this.category = category;
        this.helpText = helpText;
        this.aliases = aliases;
    }
    
    public String getName() { return name; }
    public String[] getAliases() { return aliases; }
    public String getCategory() { return category; }
    public String getHelpText() { return helpText; }
    public ICommand getHandler() { return handler; }
    
    /**
     * Check if this command matches the given key (name or alias).
     */
    public boolean matches(String key) {
        if (name.equalsIgnoreCase(key)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (aliases.length > 0) {
            sb.append(" (");
            for (int i = 0; i < aliases.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(aliases[i]);
            }
            sb.append(")");
        }
        sb.append(" - ").append(helpText);
        return sb.toString();
    }
}
