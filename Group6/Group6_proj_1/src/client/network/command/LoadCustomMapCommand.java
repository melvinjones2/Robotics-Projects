package client.network.command;

import common.ParsedCommand;
import java.io.IOException;

/**
 * Loads the custom map layout (46" x 56.5") with tape markers and goal zones.
 * Syntax: LOADCUSTOMMAP
 */
public class LoadCustomMapCommand implements ICommand {

    public LoadCustomMapCommand(ParsedCommand parsedCmd) {
    }

    @Override
    public String getName() {
        return "LOADCUSTOMMAP";
    }

    @Override
    public void execute(CommandContext context) throws IOException {
        sendReply(context, "========== LOADING CUSTOM MAP ==========");
        
        float inchToCm = 2.54f;
        float mapWidth = 46.0f * inchToCm;
        float mapHeight = 56.5f * inchToCm;
        
        sendReply(context, String.format("Map dimensions: %.1f x %.1f cm (46in x 56.5in)", mapWidth, mapHeight));
        
        // Map boundaries
        sendReply(context, String.format("CUSTOMMAP_DATA: boundary width=%.1f height=%.1f", mapWidth, mapHeight));
        sendReply(context, String.format("CUSTOMMAP_LINE: 0.0 0.0 %.1f 0.0", mapWidth));
        sendReply(context, String.format("CUSTOMMAP_LINE: %.1f 0.0 %.1f %.1f", mapWidth, mapWidth, mapHeight));
        sendReply(context, String.format("CUSTOMMAP_LINE: %.1f %.1f 0.0 %.1f", mapWidth, mapHeight, mapHeight));
        sendReply(context, String.format("CUSTOMMAP_LINE: 0.0 %.1f 0.0 0.0", mapHeight));
        
        // Center divider
        float centerX = mapWidth / 2.0f;
        sendReply(context, String.format("CUSTOMMAP_LINE: %.1f 0.0 %.1f %.1f", centerX, centerX, mapHeight));
        
        // Tape line positions (6 inches apart)
        float endSpace = 10.25f * inchToCm;
        float tapeSpacing = 6.0f * inchToCm;
        
        float h1 = endSpace;
        float h2 = h1 + tapeSpacing;
        float h3 = h2 + tapeSpacing;
        float h4 = h3 + tapeSpacing;
        float h5 = h4 + tapeSpacing;
        float h6 = h5 + tapeSpacing;
        
        sendReply(context, String.format("CUSTOMMAP_TAPE: 0.0 %.1f %.1f %.1f", h1, mapWidth, h1));
        sendReply(context, String.format("CUSTOMMAP_TAPE: 0.0 %.1f %.1f %.1f", h2, mapWidth, h2));
        sendReply(context, String.format("CUSTOMMAP_TAPE: 0.0 %.1f %.1f %.1f", h3, mapWidth, h3));
        sendReply(context, String.format("CUSTOMMAP_TAPE: 0.0 %.1f %.1f %.1f", h4, mapWidth, h4));
        sendReply(context, String.format("CUSTOMMAP_TAPE: 0.0 %.1f %.1f %.1f", h5, mapWidth, h5));
        sendReply(context, String.format("CUSTOMMAP_TAPE: 0.0 %.1f %.1f %.1f", h6, mapWidth, h6));
        
        // Goal zones (6" x 6" squares)
        float goalSize = 6.0f * inchToCm;
        
        // Goal 1: Top-left, 1' from left edge, 2'6" from top edge
        float goal1_x = 12.0f * inchToCm;
        float goal1_y = mapHeight - (2.5f * 12.0f * inchToCm) - goalSize;
        float goal1_centerX = goal1_x + (goalSize / 2.0f);
        float goal1_centerY = goal1_y + (goalSize / 2.0f);
        sendReply(context, String.format("CUSTOMMAP_GOAL: %.1f %.1f %.1f goal1", 
            goal1_centerX, goal1_centerY, goalSize));
        
        // Goal 2: Bottom-right, 1' from right edge, 2'6" from bottom edge
        float goal2_x = mapWidth - 12.0f * inchToCm - goalSize;
        float goal2_y = 2.5f * 12.0f * inchToCm;
        float goal2_centerX = goal2_x + (goalSize / 2.0f);
        float goal2_centerY = goal2_y + (goalSize / 2.0f);
        sendReply(context, String.format("CUSTOMMAP_GOAL: %.1f %.1f %.1f goal2", 
            goal2_centerX, goal2_centerY, goalSize));
        
        sendReply(context, "========== CUSTOM MAP LOADED ==========");
        sendReply(context, String.format("Goal 1: (%.1f, %.1f)", goal1_centerX, goal1_centerY));
        sendReply(context, String.format("Goal 2: (%.1f, %.1f)", goal2_centerX, goal2_centerY));
        sendReply(context, String.format("Example: NAVLINEMAP %.1f %.1f %.1f %.1f", 
            goal1_centerX, goal1_centerY, goal2_centerX, goal2_centerY));
    }

    private void sendReply(CommandContext context, String message) throws IOException {
        context.getOut().write(message);
        context.getOut().newLine();
        context.getOut().flush();
    }
}
