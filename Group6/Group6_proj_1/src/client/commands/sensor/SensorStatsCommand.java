package client.commands.sensor;

import client.network.CommandHandler;
import client.sensor.data.SensorDataStore;
import client.sensor.data.SensorDataStore.SensorStats;
import client.commands.BaseCommand;
import client.commands.CommandParser;

/**
 * Command to get statistics for a specific sensor.
 * Usage: SENSOR_STATS <sensor_name> [window_size]
 * Example: SENSOR_STATS ultrasonic 10
 */
public class SensorStatsCommand extends BaseCommand {
    
    private final SensorDataStore dataStore;
    
    public SensorStatsCommand(SensorDataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    @Override
    public void execute(String[] args, CommandHandler context) {
        if (dataStore == null) {
            error(context, "Sensor data store not available");
            return;
        }
        
        if (args.length < 2) {
            usage(context, "SENSOR_STATS <sensor_name> [window_size]");
            context.say("Available sensors: " + String.join(", ", dataStore.getSensorNames()), false);
            return;
        }
        
        String sensorName = args[1].toLowerCase();
        int windowSize = 10; // Default
        
        if (args.length > 2) {
            try {
                windowSize = CommandParser.parseInt(args[2], "window_size");
            } catch (IllegalArgumentException e) {
                error(context, e.getMessage());
                return;
            }
        }
        
        SensorStats stats = dataStore.getStats(sensorName, windowSize);
        
        if (stats == null) {
            error(context, "No data for sensor: " + sensorName);
            return;
        }
        
        String message = stats.toString();
        feedback(context, message);
        context.sendLog(message);
    }
}
