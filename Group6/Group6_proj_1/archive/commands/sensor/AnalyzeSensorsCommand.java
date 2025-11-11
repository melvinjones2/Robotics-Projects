package client.commands.sensor;

import client.commands.BaseCommand;
import client.network.CommandHandler;
import client.sensor.data.SensorAnalyzer;
import client.sensor.data.SensorDataStore;

/**
 * Command to get sensor analysis summary.
 * Usage: ANALYZE
 */
public class AnalyzeSensorsCommand extends BaseCommand {
    
    private final SensorDataStore dataStore;
    private final SensorAnalyzer analyzer;
    
    public AnalyzeSensorsCommand(SensorDataStore dataStore) {
        this.dataStore = dataStore;
        this.analyzer = new SensorAnalyzer(dataStore);
    }
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (dataStore == null) {
            error(context, "Sensor data store not available");
            return;
        }
        
        String summary = analyzer.getSensorSummary();
        
        // Send to server
        String[] lines = summary.split("\n");
        for (String line : lines) {
            context.sendLog(line);
        }
        
        feedback(context, "Sensor analysis sent");
    }
}
