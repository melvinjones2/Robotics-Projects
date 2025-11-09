package client;

import client.SensorAnalyzer.NavigationSuggestion;

/**
 * Command to get navigation suggestion based on current sensor data.
 * Usage: NAV_SUGGEST
 */
public class NavigationSuggestCommand extends BaseCommand {
    
    private final SensorAnalyzer analyzer;
    
    public NavigationSuggestCommand(SensorDataStore dataStore) {
        this.analyzer = new SensorAnalyzer(dataStore);
    }
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        NavigationSuggestion suggestion = analyzer.suggestNavigation();
        
        String message = String.format("Navigation: %s - %s", 
            suggestion.action, suggestion.reason);
        
        feedback(context, message);
        context.sendLog(message);
    }
}
