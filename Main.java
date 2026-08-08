import java.util.List;

import bomb.Bomb;
import bomb.Explosion;
import common.Action;
import common.Direction;
import common.Position;
import game.Game;
import map.Block;
import map.BlockType;
import map.GameMap;
import map.Tile;
import player.Player;
import service.GameService;

public class Main {
    public static void main(String[] args) {
        showScriptedGame();
        showBombBlocksTile();
    }

    private static void showScriptedGame() {
        System.out.println("=== Eine komplette Partie, Zug fuer Zug ===\n");

        GameMap map = room(11, 7);
        Player anna = new Player("Anna", new Position(1, 1));
        Player ben = new Player("Ben", new Position(1, 4));

        Game game = new Game(map, List.of(anna, ben));
        GameService service = new GameService();

        System.out.println("Status vor start(): " + game.getStatus());
        service.start(game);
        System.out.println("Status nach start(): " + game.getStatus() + "\n");
        print(game);

        playRound(service, game, anna, Action.BOMB, ben, Action.move(Direction.UP));
        playRound(service, game, anna, Action.move(Direction.RIGHT), ben, Action.move(Direction.UP));
        playRound(service, game, anna, Action.move(Direction.RIGHT), ben, Action.PASS);

        Player winner = game.getWinner();
        System.out.println("Gewonnen hat: " + (winner == null ? "niemand" : winner.getName()));
        System.out.println("Annas Bombenkonto ist wieder bei " + anna.getActiveBombs()
            + "/" + anna.getBombCapacity() + "\n");

        System.out.println("Ein Zug nach Spielende wird abgelehnt: "
            + service.applyAction(game, anna, Action.move(Direction.RIGHT)) + "\n");
    }

    private static void showBombBlocksTile() {
        System.out.println("=== Auf die eigene Bombe kommt man nicht zurueck ===");

        GameMap map = room(11, 7);
        Player anna = new Player("Anna", new Position(1, 1));

        Game game = new Game(map, List.of(anna));
        GameService service = new GameService();
        service.start(game);

        service.applyAction(game, anna, Action.BOMB);
        System.out.println("Bombe auf 1/1 gelegt, Anna steht noch drauf.");

        boolean away = service.applyAction(game, anna, Action.move(Direction.RIGHT));
        System.out.println("Wegtreten nach rechts: " + away + "   -> Anna auf " + anna.getPosition());

        boolean back = service.applyAction(game, anna, Action.move(Direction.LEFT));
        System.out.println("Zurueck auf die Bombe:  " + back + "  -> Anna auf " + anna.getPosition());
        System.out.println();
        print(game);
    }

    private static void playRound(GameService service, Game game,
                                  Player first, Action firstAction,
                                  Player second, Action secondAction) {
        System.out.println("Runde " + (game.getRound() + 1) + ": "
            + first.getName() + " " + firstAction + " | "
            + second.getName() + " " + secondAction);

        service.applyAction(game, first, firstAction);
        service.applyAction(game, second, secondAction);
        service.tick(game);

        print(game);
    }

    // Leerer Raum mit geschlossenem Rand, damit die Demo uebersichtlich bleibt.
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

    // Kommt in Schritt 5 nach ui/ConsoleView.
    private static void print(Game game) {
        GameMap map = game.getMap();

        for (int y = 0; y < map.getHeight(); y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < map.getWidth(); x++) {
                line.append(symbolFor(game, new Position(x, y))).append(' ');
            }

            System.out.println(line.toString());
        }

        StringBuilder status = new StringBuilder();
        status.append("Runde ").append(game.getRound());
        status.append(", Status ").append(game.getStatus());

        for (Player player : game.getPlayers()) {
            status.append(" | ").append(player.getName()).append(' ');
            status.append(player.isAlive() ? player.getPosition().toString() : "tot");
        }

        System.out.println(status.toString());
        System.out.println();
    }

    private static char symbolFor(Game game, Position position) {
        for (Explosion explosion : game.getExplosions()) {
            if (explosion.covers(position)) {
                return '*';
            }
        }

        for (Player player : game.getPlayers()) {
            if (player.getPosition().equals(position)) {
                return player.isAlive() ? player.getName().charAt(0) : 'x';
            }
        }

        for (Bomb bomb : game.getBombs()) {
            if (bomb.getPosition().equals(position)) {
                return (char) ('0' + bomb.getFuseTicks());
            }
        }

        Tile tile = game.getMap().getTile(position);
        if (tile.getBlock() != null) {
            return tile.getBlock().isDestroyable() ? 'o' : '#';
        }

        return '.';
    }
}
