package map;

public class GameMap {
    private int width;
    private int height;
    private Tile[][] tiles;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = new Tile(new common.Position(x, y));
            }
        }
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public Tile getTile(common.Position position) {
        return tiles[position.getY()][position.getX()];
    }
    public boolean isInsideMap(common.Position position) {
        return position.getX() >= 0
        && position.getX() < width
        && position.getY() >= 0
        && position.getY() < height;
    }
}
