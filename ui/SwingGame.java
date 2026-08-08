package ui;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import common.Action;
import game.Game;
import game.GameStatus;
import map.GameMap;
import persistence.HighscoreRepository;
import persistence.MapRepository;
import player.Player;
import service.GameService;

// Das lokale Spiel im Fenster, in Echtzeit.
//
// Ein Timer ticht die Welt weiter, statt auf eine Eingabe zu warten. Genau
// das ging auf der Konsole nicht: Scanner.nextLine() blockiert, bis jemand
// Enter drueckt. Tastenereignisse blockieren nicht - deshalb faellt hier
// der Grund fuers Rundenmodell weg.
//
// GameService und Game bleiben unveraendert. Der Timer ruft dieselben
// Methoden auf wie vorher die Konsolenschleife.
public class SwingGame implements PlayerInput {
    private static final int TICK_MS = 450;
    private static final int MAP_WIDTH = 13;
    private static final int MAP_HEIGHT = 11;

    private final GameService service = new GameService();
    private final HighscoreRepository highscores = new HighscoreRepository();
    private final List<PlayerKeys> keys = PlayerKeys.defaults();
    private final Path mapFile;
    private final int playerCount;

    private GameWindow window;
    private Game game;
    private Timer timer;

    // Was jeder Spieler seit dem letzten Tick zuletzt gedrueckt hat.
    // Nur der letzte Tastendruck zaehlt, sonst staut sich Eingabe auf.
    private Action[] pending;

    public SwingGame(int playerCount, Path mapFile) {
        this.playerCount = playerCount;
        this.mapFile = mapFile;
    }

    public static void start(int playerCount, Path mapFile) {
        // Swing-Komponenten gehoeren dem Event-Dispatch-Thread. Auch das
        // Erzeugen des Fensters muss dort passieren, nicht in main().
        SwingUtilities.invokeLater(() -> new SwingGame(playerCount, mapFile).run());
    }

    private void run() {
        GameMap map = loadMap();
        if (map == null) {
            return;
        }

        game = newGame(map);
        pending = new Action[game.getPlayers().size()];

        window = new GameWindow("Bomberman", this);
        for (int i = 0; i < game.getPlayers().size(); i++) {
            window.bindPlayer(i, keys.get(i));
        }
        window.bindCommand(KeyEvent.VK_N, "neustart", this::restart);
        window.bindCommand(KeyEvent.VK_ESCAPE, "beenden", () -> System.exit(0));

        window.open();

        service.start(game);
        draw();

        timer = new Timer(TICK_MS, event -> tick());
        timer.start();
    }

    // Laeuft auf dem Event-Dispatch-Thread, genau wie die Tastendruecke.
    // Dadurch fasst nur ein einziger Thread den Spielzustand an und es
    // braucht kein synchronized - dasselbe Prinzip wie beim GameServer.
    private void tick() {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);

            Action action = pending[i];
            pending[i] = null;

            if (action != null && player.isAlive()) {
                service.applyAction(game, player, action);
            }
        }

        service.tick(game);
        draw();

        if (game.getStatus() == GameStatus.FINISHED) {
            timer.stop();
            finish();
        }
    }

    @Override
    public void onAction(int playerIndex, Action action) {
        if (game != null && game.getStatus() == GameStatus.RUNNING) {
            pending[playerIndex] = action;
        }
    }

    private void restart() {
        if (timer != null) {
            timer.stop();
        }

        GameMap map = loadMap();
        if (map == null) {
            return;
        }

        game = newGame(map);
        pending = new Action[game.getPlayers().size()];

        service.start(game);
        draw();
        timer.start();
    }

    private Game newGame(GameMap map) {
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            players.add(new Player("Spieler " + (char) ('A' + i), map.getSpawnPositions().get(i)));
        }

        return new Game(map, players);
    }

    private GameMap loadMap() {
        if (mapFile == null) {
            return GameMap.generateStandardMap(MAP_WIDTH, MAP_HEIGHT);
        }

        try {
            return new MapRepository().load(mapFile);
        } catch (IOException | IllegalArgumentException cause) {
            javax.swing.JOptionPane.showMessageDialog(window,
                "Map konnte nicht geladen werden:\n" + cause.getMessage(),
                "Bomberman", javax.swing.JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void draw() {
        window.showState(game,
            "Runde " + game.getRound() + "     " + GameWindow.describePlayers(game),
            GameWindow.describeKeys(keys, game.getPlayers().size()) + "     N: neues Spiel");
    }

    private void finish() {
        Player winner = game.getWinner();

        if (winner == null) {
            window.showStatus("Unentschieden - niemand hat ueberlebt.");
        } else {
            window.showStatus(winner.getName() + " gewinnt nach " + game.getRound() + " Runden!");
            saveWin(winner);
        }

        window.showHint("N druecken fuer ein neues Spiel, Esc zum Beenden.");
    }

    private void saveWin(Player winner) {
        try {
            highscores.addWin(winner.getName());
        } catch (IOException | IllegalArgumentException cause) {
            window.showHint("Sieg konnte nicht gespeichert werden: " + cause.getMessage());
        }
    }
}
