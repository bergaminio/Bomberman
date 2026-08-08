package service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import common.Direction;
import common.Position;
import map.GameMap;
import map.Tile;

public class MapValidator {

    public boolean isValid(GameMap map) {
        return findProblems(map).isEmpty();
    }

    public List<String> findProblems(GameMap map) {
        List<String> problems = new ArrayList<>();

        if (map.getWidth() < 5 || map.getHeight() < 5) {
            problems.add("Map ist zu klein, mindestens 5x5 wird gebraucht.");
            return problems;
        }

        checkBorderIsClosed(map, problems);
        checkSpawnsAreFree(map, problems);
        checkSpawnsAreConnected(map, problems);

        return problems;
    }

    private void checkBorderIsClosed(GameMap map, List<String> problems) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                boolean isBorder = x == 0 || y == 0
                    || x == map.getWidth() - 1
                    || y == map.getHeight() - 1;

                if (isBorder && map.getTile(new Position(x, y)).isWalkable()) {
                    problems.add("Rand ist offen bei " + x + "/" + y + ".");
                }
            }
        }
    }

    private void checkSpawnsAreFree(GameMap map, List<String> problems) {
        for (Position spawn : map.getSpawnPositions()) {
            if (!map.getTile(spawn).isWalkable()) {
                problems.add("Startfeld " + spawn.getX() + "/" + spawn.getY() + " ist verbaut.");
            }
        }
    }

    // Flood Fill vom ersten Startfeld aus: alle anderen Startfelder muessen
    // erreichbar sein, sonst koennen sich die Spieler nie treffen.
    private void checkSpawnsAreConnected(GameMap map, List<String> problems) {
        List<Position> spawns = map.getSpawnPositions();
        boolean[][] reached = new boolean[map.getHeight()][map.getWidth()];

        Position start = spawns.get(0);
        Queue<Position> todo = new ArrayDeque<>();
        todo.add(start);
        reached[start.getY()][start.getX()] = true;

        while (!todo.isEmpty()) {
            Position current = todo.remove();

            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);

                if (!map.isInsideMap(next)) {
                    continue;
                }
                if (reached[next.getY()][next.getX()]) {
                    continue;
                }
                if (!canBeReached(map.getTile(next))) {
                    continue;
                }

                reached[next.getY()][next.getX()] = true;
                todo.add(next);
            }
        }

        for (Position spawn : spawns) {
            if (!reached[spawn.getY()][spawn.getX()]) {
                problems.add("Startfeld " + spawn.getX() + "/" + spawn.getY()
                    + " ist von den anderen abgeschnitten.");
            }
        }
    }

    // Zerstoerbare Bloecke zaehlen als erreichbar, man kann sie ja wegsprengen.
    // Nur unzerstoerbare Bloecke trennen die Map wirklich.
    private boolean canBeReached(Tile tile) {
        if (tile.isWalkable()) {
            return true;
        }
        return tile.getBlock() != null && tile.getBlock().isDestroyable();
    }
}
