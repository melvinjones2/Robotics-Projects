package server.mapping;

import java.util.ArrayList;
import java.util.List;

public class LineMapManager {
    
    public static class LineSegment {
        public final float x1, y1, x2, y2;
        
        public LineSegment(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
        
        @Override
        public String toString() {
            return String.format("(%.1f,%.1f)-(%.1f,%.1f)", x1, y1, x2, y2);
        }
    }
    
    private final float width;
    private final float height;
    private final List<LineSegment> lines;
    
    public LineMapManager() {
        this(100.0f, 100.0f);
    }
    
    public LineMapManager(float width, float height) {
        this.width = width;
        this.height = height;
        this.lines = new ArrayList<>();
    }
    
    public void addLine(float x1, float y1, float x2, float y2) {
        lines.add(new LineSegment(x1, y1, x2, y2));
    }
    
    public void clearLines() {
        lines.clear();
    }
    
    public int getLineCount() {
        return lines.size();
    }
    
    public List<LineSegment> getLines() {
        return new ArrayList<>(lines);
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
    
    public void loadDefaultTestMap() {
        clearLines();
        addLine(0.0f, 0.0f, 100.0f, 0.0f);
        addLine(100.0f, 0.0f, 100.0f, 100.0f);
        addLine(100.0f, 100.0f, 0.0f, 100.0f);
        addLine(0.0f, 100.0f, 0.0f, 0.0f);
        addLine(20.0f, 0.0f, 20.0f, 20.0f);
        addLine(20.0f, 20.0f, 50.0f, 20.0f);
        addLine(50.0f, 20.0f, 50.0f, 70.0f);
        addLine(50.0f, 50.0f, 65.0f, 50.0f);
        addLine(0.0f, 70.0f, 25.0f, 70.0f);
    }
    
    @Override
    public String toString() {
        return String.format("LineMap: %d lines, bounds=(0,0 to %.1f,%.1f)", lines.size(), width, height);
    }
}
