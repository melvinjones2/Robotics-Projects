package server.mapping;

/**
 * Grid-based occupancy map: UNKNOWN (0), FREE (1), OCCUPIED (2).
 */
public class GridMapManager {
    
    public static final byte UNKNOWN = 0;
    public static final byte FREE = 1;
    public static final byte OCCUPIED = 2;
    
    private final int gridWidth;   // Number of cells horizontally
    private final int gridHeight;  // Number of cells vertically
    private final float cellSize;
    private final byte[][] grid;
    
    public GridMapManager(float worldWidth, float worldHeight, float cellSize) {
        this.cellSize = cellSize;
        this.gridWidth = (int) Math.ceil(worldWidth / cellSize);
        this.gridHeight = (int) Math.ceil(worldHeight / cellSize);
        this.grid = new byte[gridHeight][gridWidth];
        clear();
    }
    
    public GridMapManager() {
        this(100.0f, 100.0f, 10.0f);
    }
    
    public void clear() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                grid[y][x] = UNKNOWN;
            }
        }
    }
    
    public void setCellWorld(float worldX, float worldY, byte state) {
        int gridX = (int) (worldX / cellSize);
        int gridY = (int) (worldY / cellSize);
        setCell(gridX, gridY, state);
    }
    
    public void setCell(int gridX, int gridY, byte state) {
        if (isValidCell(gridX, gridY)) {
            grid[gridY][gridX] = state;
        }
    }
    
    public byte getCell(int gridX, int gridY) {
        if (isValidCell(gridX, gridY)) {
            return grid[gridY][gridX];
        }
        return UNKNOWN;
    }
    
    public boolean isValidCell(int gridX, int gridY) {
        return gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight;
    }
    
    public int getGridWidth() {
        return gridWidth;
    }
    
    public int getGridHeight() {
        return gridHeight;
    }
    
    public float getCellSize() {
        return cellSize;
    }
    
    public int countCells(byte state) {
        int count = 0;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (grid[y][x] == state) {
                    count++;
                }
            }
        }
        return count;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("GridMap %dx%d cells (%.1fcm each):\n", 
            gridWidth, gridHeight, cellSize));
        sb.append(String.format("Unknown: %d, Free: %d, Occupied: %d\n",
            countCells(UNKNOWN), countCells(FREE), countCells(OCCUPIED)));
        return sb.toString();
    }
    
    public String toASCII() {
        StringBuilder sb = new StringBuilder();
        for (int y = gridHeight - 1; y >= 0; y--) {  // Top to bottom
            for (int x = 0; x < gridWidth; x++) {
                switch (grid[y][x]) {
                    case UNKNOWN:
                        sb.append('?');
                        break;
                    case FREE:
                        sb.append('.');
                        break;
                    case OCCUPIED:
                        sb.append('#');
                        break;
                    default:
                        sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
