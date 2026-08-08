import java.util.List;

import common.Position;
import map.Block;
import map.BlockType;
import map.GameMap;
import map.Tile;
import service.MapValidator;

public class Main {
    public static void main(String[] args) {
        MapValidator validator = new MapValidator();

        System.out.println("=== 1) Standardmap (Seed 42) ===");
        GameMap standard = GameMap.generateStandardMap(13, 11, 42);
        print(standard);
        report(validator, standard);

        System.out.println("=== 2) Map ohne Rand ===");
        GameMap open = new GameMap(9, 9);
        print(open);
        report(validator, open);

        System.out.println("=== 3) Startfeld eingemauert ===");
        GameMap walled = GameMap.generateStandardMap(13, 11, 42);
        walled.getTile(new Position(2, 1)).setBlock(new Block(BlockType.INDESTRUCTIBLE));
        walled.getTile(new Position(1, 2)).setBlock(new Block(BlockType.INDESTRUCTIBLE));
        print(walled);
        report(validator, walled);
    }

    private static void report(MapValidator validator, GameMap map) {
        List<String> problems = validator.findProblems(map);

        if (problems.isEmpty()) {
            System.out.println("Map ist gueltig.\n");
            return;
        }

        System.out.println("Map ist ungueltig, " + problems.size() + " Problem(e):");
        for (String problem : problems) {
            System.out.println("  - " + problem);
        }
        System.out.println();
    }

    // Kommt spaeter nach ui/ConsoleView, hier nur damit man das Ergebnis sieht.
    private static void print(GameMap map) {
        List<Position> spawns = map.getSpawnPositions();

        for (int y = 0; y < map.getHeight(); y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(new Position(x, y));
                line.append(symbolFor(tile, spawns, x, y)).append(' ');
            }

            System.out.println(line.toString());
        }
        System.out.println();
    }

    private static char symbolFor(Tile tile, List<Position> spawns, int x, int y) {
        if (tile.getBlock() != null) {
            return tile.getBlock().isDestroyable() ? 'o' : '#';
        }

        for (int i = 0; i < spawns.size(); i++) {
            if (spawns.get(i).getX() == x && spawns.get(i).getY() == y) {
                return (char) ('1' + i);
            }
        }

        return '.';
    }
}
