package client.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for all commands.
 * Commands register themselves with metadata including aliases and help text.
 * Provides command lookup, help generation, and category organization.
 */
public class CommandRegistry {
    private final Map<String, ICommand> commandMap;
    private final List<CommandMetadata> metadata;
    
    public CommandRegistry() {
        this.commandMap = new HashMap<>();
        this.metadata = new ArrayList<>();
    }
    
    /**
     * Register a command with metadata.
     * Automatically registers all aliases.
     */
    public void register(CommandMetadata meta) {
        // Register primary name
        commandMap.put(meta.getName().toUpperCase(), meta.getHandler());
        
        // Register all aliases
        for (String alias : meta.getAliases()) {
            commandMap.put(alias.toUpperCase(), meta.getHandler());
        }
        
        // Store metadata for help/introspection
        metadata.add(meta);
    }
    
    /**
     * Get command handler by name or alias.
     */
    public ICommand getCommand(String key) {
        return commandMap.get(key.toUpperCase());
    }
    
    /**
     * Get all registered command metadata.
     */
    public List<CommandMetadata> getAllMetadata() {
        return new ArrayList<>(metadata);
    }
    
    /**
     * Get commands by category.
     */
    public List<CommandMetadata> getByCategory(String category) {
        List<CommandMetadata> result = new ArrayList<>();
        for (CommandMetadata meta : metadata) {
            if (meta.getCategory().equalsIgnoreCase(category)) {
                result.add(meta);
            }
        }
        return result;
    }
    
    /**
     * Get help text for all commands, organized by category.
     */
    public String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Commands:\n");
        
        // Group by category
        Map<String, List<CommandMetadata>> byCategory = new HashMap<>();
        for (CommandMetadata meta : metadata) {
            String cat = meta.getCategory();
            if (!byCategory.containsKey(cat)) {
                byCategory.put(cat, new ArrayList<CommandMetadata>());
            }
            byCategory.get(cat).add(meta);
        }
        
        // Output each category
        for (Map.Entry<String, List<CommandMetadata>> entry : byCategory.entrySet()) {
            sb.append("\n[").append(entry.getKey()).append("]\n");
            for (CommandMetadata meta : entry.getValue()) {
                sb.append("  ").append(meta.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Check if a command is registered.
     */
    public boolean hasCommand(String key) {
        return commandMap.containsKey(key.toUpperCase());
    }
    
    /**
     * Get total number of registered commands (not including aliases).
     */
    public int getCommandCount() {
        return metadata.size();
    }
}
