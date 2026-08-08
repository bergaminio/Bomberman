import java.util.HashSet;
import java.util.List;
import java.util.Set;

import common.Direction;
import common.Position;
import map.GameMap;
import map.Tile;
import player.Player;
import service.MapValidator;
import service.MovementService;

public class Main {
    public static void main(String[] args) {
        showPositionIsAValue();
        showSharedPositionIsSafe();
        showMovement();
        showOutsideMap();
    }

    private static void showPositionIsAValue() {
        System.out.println("=== 1) Position ist ein Wert, kein Objekt-Identitaetsvergleich ===");

        Position a = new Position(3, 4);
        Position b = new Position(3, 4);

        System.out.println("a = " + a + ", b = " + b);
        System.out.println("a == b       -> " + (a == b) + "   (zwei verschiedene Objekte)");
        System.out.println("a.equals(b)  -> " + a.equals(b) + "    (gleicher Wert)");

        Set<Position> visited = new HashSet<>();
        visited.add(a);
        visited.add(b);
        System.out.println("HashSet mit a und b enthaelt " + visited.size() + " Eintrag");
        System.out.println("(ohne hashCode() waeren es 2)\n");
    }

    private static void showSharedPositionIsSafe() {
        System.out.println("=== 2) Zwei Spieler teilen sich dieselbe Position-Instanz ===");

        GameMap map = GameMap.generateStandardMap(13, 11, 42);
        MovementService movement = new MovementService();

        Position shared = new Position(1, 1);
        Player anna = new Player("Anna", shared);
        Player ben = new Player("Ben", shared);

        System.out.println("vorher:  Anna " + anna.getPosition() + ", Ben " + ben.getPosition());
        movement.tryMove(anna, Direction.RIGHT, map);
        System.out.println("nachher: Anna " + anna.getPosition() + ", Ben " + ben.getPosition());
        System.out.println("Ben steht noch da, weil move() ein neues Objekt liefert\n");
    }

    private static void showMovement() {
        System.out.println("=== 3) Bewegung auf der Standardmap ===");

        GameMap map = GameMap.generateStandardMap(13, 11, 42);
        MovementService movement = new MovementService();
        Player player = new Player("Michael", map.getSpawnPositions().get(0));

        print(map, player);
        System.out.println("Bomben: " + player.getActiveBombs() + "/" + player.getBombCapacity()
            + ", Radius " + player.getBlastRadius() + "\n");

        walk(movement, player, map, Direction.RIGHT);
        walk(movement, player, map, Direction.RIGHT);
        walk(movement, player, map, Direction.DOWN);
        walk(movement, player, map, Direction.UP);
        walk(movement, player, map, Direction.LEFT);
        walk(movement, player, map, Direction.DOWN);

        System.out.println();
        print(map, player);

        System.out.println("Und als Toter (UP waere frei):");
        player.die();
        walk(movement, player, map, Direction.UP);
        System.out.println();
    }

    private static void showOutsideMap() {
        System.out.println("=== 4) Map ohne Rand: die Grenzpruefung faengt es ab ===");

        GameMap open = new GameMap(9, 9);
        MovementService movement = new MovementService();
        Player player = new Player("Michael", new Position(0, 0));

        walk(movement, player, open, Direction.LEFT);
        walk(movement, player, open, Direction.UP);
        walk(movement, player, open, Direction.RIGHT);
        System.out.println();
    }

    private static void walk(MovementService movement, Player player, GameMap map, Direction direction) {
        Position before = player.getPosition();
        boolean moved = movement.tryMove(player, direction, map);

        String result = moved
            ? "ok         " + before + " -> " + player.getPosition()
            : "blockiert  bleibt auf " + player.getPosition() + reasonFor(map, before, direction, player);

        System.out.printf("%-6s %s%n", direction, result);
    }

    // Gleiche Reihenfolge wie in tryMove(), sonst zeigt die Begruendung
    // einen Grund an, der gar nicht der ausschlaggebende war.
    private static String reasonFor(GameMap map, Position from, Direction direction, Player player) {
        if (!player.isAlive()) {
            return "  (Spieler ist tot)";
        }

        Position target = from.move(direction);

        if (!map.isInsideMap(target)) {
            return "  (ausserhalb der Map)";
        }

        Tile tile = map.getTile(target);
        if (!tile.isWalkable()) {
            return tile.getBlock().isDestroyable() ? "  (zerstoerbarer Block)" : "  (Mauer)";
        }

        return "  (unbekannt)";
    }

    // Kommt spaeter nach ui/ConsoleView, hier nur damit man das Ergebnis sieht.
    private static void print(GameMap map, Player player) {
        List<Position> spawns = map.getSpawnPositions();

        for (int y = 0; y < map.getHeight(); y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < map.getWidth(); x++) {
                Position position = new Position(x, y);
                line.append(symbolFor(map.getTile(position), spawns, position, player)).append(' ');
            }

            System.out.println(line.toString());
        }
    }

    private static char symbolFor(Tile tile, List<Position> spawns, Position position, Player player) {
        if (player != null && player.getPosition().equals(position)) {
            return player.isAlive() ? 'P' : 'x';
        }

        if (tile.getBlock() != null) {
            return tile.getBlock().isDestroyable() ? 'o' : '#';
        }

        if (spawns.contains(position)) {
            return (char) ('1' + spawns.indexOf(position));
        }

        return '.';
    }
}
