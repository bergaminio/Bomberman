import java.util.ArrayList;
import java.util.List;

import common.Action;
import game.Game;
import game.GameStatus;
import map.GameMap;
import player.Player;
import service.GameService;
import service.MapValidator;
import ui.ConsoleView;

public class Main {
    private static final int MAP_WIDTH = 13;
    private static final int MAP_HEIGHT = 11;

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        GameService service = new GameService();

        System.out.println("=== BOMBERMAN ===\n");
        view.showLegend();
        System.out.println();

        Game game = setUpGame(view);
        if (game == null) {
            view.close();
            return;
        }

        service.start(game);
        playUntilFinished(view, service, game);

        view.render(game);
        view.showResult(game);
        view.close();
    }

    private static Game setUpGame(ConsoleView view) {
        GameMap map = GameMap.generateStandardMap(MAP_WIDTH, MAP_HEIGHT);

        // Die Standardmap ist per Konstruktion gueltig. Trotzdem einmal
        // pruefen, damit ein Fehler in der Generierung sofort auffaellt
        // statt mitten im Spiel.
        MapValidator validator = new MapValidator();
        List<String> problems = validator.findProblems(map);

        if (!problems.isEmpty()) {
            view.showMessage("Die erzeugte Map ist kaputt:");
            for (String problem : problems) {
                view.showMessage("  " + problem);
            }
            return null;
        }

        int count = view.askForNumber("Wie viele Spieler", 2, 4);
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            char letter = (char) ('A' + i);
            String fallback = "Spieler " + letter;
            String name = view.askForName("Name fuer " + letter, fallback);
            players.add(new Player(name, map.getSpawnPositions().get(i)));
        }

        return new Game(map, players);
    }

    private static void playUntilFinished(ConsoleView view, GameService service, Game game) {
        while (game.getStatus() == GameStatus.RUNNING) {
            view.render(game);

            if (!collectActions(view, service, game)) {
                return;
            }

            service.tick(game);
        }
    }

    // Jeder lebende Spieler zieht einmal. Liefert false, wenn jemand
    // abbrechen will.
    private static boolean collectActions(ConsoleView view, GameService service, Game game) {
        for (Player player : game.getPlayers()) {
            if (!player.isAlive()) {
                continue;
            }

            Action action = view.askForAction(player, view.letterFor(game, player));
            if (action == null) {
                return false;
            }

            if (!service.applyAction(game, player, action)) {
                view.showMessage("Geht nicht, " + player.getName() + " bleibt stehen.");
            }
        }

        return true;
    }
}
