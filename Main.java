import java.util.ArrayList;
import java.util.List;

import bomb.Bomb;
import bomb.Explosion;
import common.Direction;
import common.Position;
import map.Block;
import map.BlockType;
import map.GameMap;
import map.Tile;
import player.Player;
import service.BombService;
import service.MovementService;

public class Main {
    public static void main(String[] args) {
        showCountdownAndEscape();
        showBombCapacity();
        showBlastStops();
        showChainReaction();
    }

    private static void showCountdownAndEscape() {
        System.out.println("=== 1) Bombe legen, weglaufen, Zuender ablaufen lassen ===");

        GameMap map = room(11, 7);
        BombService bombs = new BombService();
        MovementService movement = new MovementService();

        List<Bomb> activeBombs = new ArrayList<>();
        List<Explosion> explosions = new ArrayList<>();
        Player player = new Player("Michael", new Position(1, 1));

        bombs.placeBomb(player, activeBombs);
        System.out.println("Bombe gelegt, Zuender " + activeBombs.get(0).getFuseTicks()
            + ", Konto " + player.getActiveBombs() + "/" + player.getBombCapacity());
        print(map, player, activeBombs, explosions);

        for (int tick = 1; tick <= 3; tick++) {
            explosions.addAll(bombs.tickBombs(activeBombs, map));
            movement.tryMove(player, Direction.RIGHT, map);

            System.out.println("nach Tick " + tick + ":");
            print(map, player, activeBombs, explosions);
        }

        System.out.println("Konto nach der Explosion: " + player.getActiveBombs()
            + "/" + player.getBombCapacity());

        bombs.tickExplosions(explosions);
        System.out.println("Nach einem weiteren Tick sind noch " + explosions.size()
            + " Explosionen uebrig.\n");
    }

    private static void showBombCapacity() {
        System.out.println("=== 2) Kapazitaet: ein Spieler, eine Bombe ===");

        GameMap map = room(11, 7);
        BombService bombs = new BombService();
        MovementService movement = new MovementService();

        List<Bomb> activeBombs = new ArrayList<>();
        Player player = new Player("Michael", new Position(1, 1));

        System.out.println("1. Bombe: " + describe(bombs.placeBomb(player, activeBombs)));
        movement.tryMove(player, Direction.RIGHT, map);
        System.out.println("2. Bombe: " + describe(bombs.placeBomb(player, activeBombs))
            + "  (Kapazitaet erschoepft)");

        movement.tryMove(player, Direction.LEFT, map);
        System.out.println("3. Versuch auf dem Feld der 1. Bombe: "
            + describe(bombs.placeBomb(player, activeBombs)) + "\n");
    }

    private static void showBlastStops() {
        System.out.println("=== 3) Wo der Feuerstrahl endet (Radius 3) ===");

        GameMap map = room(11, 7);
        map.getTile(new Position(8, 3)).setBlock(new Block(BlockType.INDESTRUCTIBLE));
        map.getTile(new Position(3, 3)).setBlock(new Block(BlockType.DESTROYABLE));

        BombService bombs = new BombService();
        List<Bomb> activeBombs = new ArrayList<>();
        Player player = new Player("Michael", new Position(1, 5));

        Bomb bomb = new Bomb(player, new Position(5, 3), 3, 1);
        activeBombs.add(bomb);
        player.bombPlaced();

        System.out.println("vorher:");
        print(map, player, activeBombs, new ArrayList<>());

        List<Explosion> explosions = bombs.tickBombs(activeBombs, map);
        System.out.println("nachher:");
        print(map, player, activeBombs, explosions);

        System.out.println("links  bei 3/3 vom zerstoerbaren Block geschluckt, er ist jetzt weg");
        System.out.println("rechts bei 8/3 vor der Mauer gestoppt, 8/3 selbst bleibt heil");
        System.out.println("oben und unten vom Rand gestoppt");
        System.out.println("getroffene Felder: " + explosions.get(0).getAffectedPositions().size() + "\n");
    }

    private static void showChainReaction() {
        System.out.println("=== 4) Kettenreaktion ===");

        GameMap map = room(11, 7);
        BombService bombs = new BombService();
        List<Bomb> activeBombs = new ArrayList<>();
        Player player = new Player("Michael", new Position(1, 5));

        Bomb first = new Bomb(player, new Position(2, 3), 3, 1);
        Bomb second = new Bomb(player, new Position(5, 3), 2, 3);
        activeBombs.add(first);
        activeBombs.add(second);
        player.bombPlaced();
        player.bombPlaced();

        System.out.println("Bombe A auf 2/3 mit Zuender 1, Bombe B auf 5/3 mit Zuender 3");
        print(map, player, activeBombs, new ArrayList<>());

        List<Explosion> explosions = bombs.tickBombs(activeBombs, map);

        System.out.println("nach einem Tick: A war faellig, ihr Feuer hat B mitgerissen");
        print(map, player, activeBombs, explosions);
        System.out.println("Explosionen in diesem Tick: " + explosions.size()
            + ", B hatte Zuender " + second.getFuseTicks() + " statt 2\n");
    }

    private static String describe(Bomb bomb) {
        return bomb == null ? "abgelehnt" : "gelegt auf " + bomb.getPosition();
    }

    // Leerer Raum mit geschlossenem Rand, damit die Demos uebersichtlich bleiben.
    private static GameMap room(int width, int height) {
        GameMap map = new GameMap(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    map.getTile(new Position(x, y)).setBlock(new Block(BlockType.INDESTRUCTIBLE));
                }
            }
        }

        return map;
    }

    // Kommt spaeter nach ui/ConsoleView, hier nur damit man das Ergebnis sieht.
    private static void print(GameMap map, Player player, List<Bomb> bombs, List<Explosion> explosions) {
        for (int y = 0; y < map.getHeight(); y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < map.getWidth(); x++) {
                line.append(symbolFor(map, new Position(x, y), player, bombs, explosions)).append(' ');
            }

            System.out.println(line.toString());
        }
        System.out.println();
    }

    private static char symbolFor(GameMap map, Position position, Player player,
                                  List<Bomb> bombs, List<Explosion> explosions) {
        for (Explosion explosion : explosions) {
            if (explosion.covers(position)) {
                return '*';
            }
        }

        if (player.getPosition().equals(position)) {
            return player.isAlive() ? 'P' : 'x';
        }

        for (Bomb bomb : bombs) {
            if (bomb.getPosition().equals(position)) {
                return (char) ('0' + bomb.getFuseTicks());
            }
        }

        Tile tile = map.getTile(position);
        if (tile.getBlock() != null) {
            return tile.getBlock().isDestroyable() ? 'o' : '#';
        }

        return '.';
    }
}
