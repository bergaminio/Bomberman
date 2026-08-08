package map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import common.Position;

public class GameMap {
    private static final double DESTROYABLE_CHANCE = 0.75;

    private int width;
    private int height;
    private Tile[][] tiles;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = new Tile(new Position(x, y));
            }
        }
    }

    public static GameMap generateStandardMap(int width, int height) {
        return generateStandardMap(width, height, new Random());
    }

    // Gleicher Seed = gleiche Map. Praktisch zum Testen und Debuggen.
    public static GameMap generateStandardMap(int width, int height, long seed) {
        return generateStandardMap(width, height, new Random(seed));
    }

    private static GameMap generateStandardMap(int width, int height, Random random) {
        GameMap map = new GameMap(width, height);
        List<Position> spawns = map.getSpawnPositions();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.tiles[y][x];

                if (map.isBorder(x, y) || isPillar(x, y)) {
                    tile.setBlock(new Block(BlockType.INDESTRUCTIBLE));
                    continue;
                }

                if (isSpawnSafe(spawns, x, y)) {
                    continue;
                }

                if (random.nextDouble() < DESTROYABLE_CHANCE) {
                    tile.setBlock(new Block(BlockType.DESTROYABLE));
                }
            }
        }

        return map;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(Position position) {
        return tiles[position.getY()][position.getX()];
    }

    public boolean isInsideMap(Position position) {
        return position.getX() >= 0
        && position.getX() < width
        && position.getY() >= 0
        && position.getY() < height;
    }

    // Die vier Ecken innerhalb des Randes, im Uhrzeigersinn ab links oben.
    public List<Position> getSpawnPositions() {
        List<Position> spawns = new ArrayList<>();
        spawns.add(new Position(1, 1));
        spawns.add(new Position(width - 2, 1));
        spawns.add(new Position(width - 2, height - 2));
        spawns.add(new Position(1, height - 2));
        return spawns;
    }

    private boolean isBorder(int x, int y) {
        return x == 0 || y == 0 || x == width - 1 || y == height - 1;
    }

    // Die festen Saeulen des klassischen Bomberman-Rasters.
    private static boolean isPillar(int x, int y) {
        return x % 2 == 0 && y % 2 == 0;
    }

    // Startfeld plus direkte Nachbarn bleiben leer, sonst startet jemand eingemauert.
    private static boolean isSpawnSafe(List<Position> spawns, int x, int y) {
        for (Position spawn : spawns) {
            int distance = Math.abs(spawn.getX() - x) + Math.abs(spawn.getY() - y);
            if (distance <= 1) {
                return true;
            }
        }
        return false;
    }
}
