import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import common.Action;
import game.Game;
import game.GameStatus;
import map.GameMap;
import persistence.HighscoreEntry;
import persistence.HighscoreRepository;
import persistence.MapRepository;
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
        HighscoreRepository highscores = new HighscoreRepository();

        System.out.println("=== BOMBERMAN ===\n");
        showHighscores(view, highscores);
        view.showLegend();
        System.out.println();

        GameMap map = loadOrGenerateMap(view, args);
        if (map == null) {
            view.close();
            return;
        }

        Game game = setUpPlayers(view, map);
        service.start(game);
        playUntilFinished(view, service, game);

        view.render(game);
        view.showResult(game);
        recordWinner(view, highscores, game);
        view.close();
    }

    private static void showHighscores(ConsoleView view, HighscoreRepository highscores) {
        try {
            List<HighscoreEntry> entries = highscores.load();

            if (entries.isEmpty()) {
                System.out.println("Bestenliste: noch keine Siege aufgezeichnet.\n");
                return;
            }

            System.out.println("Bestenliste:");
            for (HighscoreEntry entry : entries) {
                System.out.printf("  %-16s %d%n", entry.getName(), entry.getWins());
            }
            System.out.println();

        } catch (IOException | IllegalArgumentException cause) {
            // Eine kaputte Bestenliste darf niemanden am Spielen hindern.
            view.showMessage("Bestenliste nicht lesbar (" + cause.getMessage() + "), starte trotzdem.");
            System.out.println();
        }
    }

    private static GameMap loadOrGenerateMap(ConsoleView view, String[] args) {
        if (args.length == 0) {
            return generateMap(view);
        }

        Path file = Path.of(args[0]);

        // Zwei Fehlerarten, zwei Meldungen: die Datei war nicht lesbar,
        // oder sie war lesbar und ihr Inhalt taugt nicht.
        try {
            GameMap map = new MapRepository().load(file);
            view.showMessage("Map geladen aus " + file);
            return map;

        } catch (IOException cause) {
            view.showMessage("Datei nicht lesbar: " + cause.getMessage());
            return null;

        } catch (IllegalArgumentException cause) {
            view.showMessage("Map unbrauchbar: " + cause.getMessage());
            return null;
        }
    }

    private static GameMap generateMap(ConsoleView view) {
        GameMap map = GameMap.generateStandardMap(MAP_WIDTH, MAP_HEIGHT);
        List<String> problems = new MapValidator().findProblems(map);

        if (problems.isEmpty()) {
            return map;
        }

        view.showMessage("Die erzeugte Map ist kaputt:");
        for (String problem : problems) {
            view.showMessage("  " + problem);
        }
        return null;
    }

    private static Game setUpPlayers(ConsoleView view, GameMap map) {
        int count = view.askForNumber("Wie viele Spieler", 2, 4);
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            char letter = (char) ('A' + i);
            String name = view.askForName("Name fuer " + letter, "Spieler " + letter);
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

    private static void recordWinner(ConsoleView view, HighscoreRepository highscores, Game game) {
        Player winner = game.getWinner();

        if (winner == null) {
            return;
        }

        try {
            highscores.addWin(winner.getName());
            view.showMessage("Sieg fuer " + winner.getName() + " gespeichert.");
        } catch (IOException | IllegalArgumentException cause) {
            view.showMessage("Sieg konnte nicht gespeichert werden: " + cause.getMessage());
        }
    }
}
