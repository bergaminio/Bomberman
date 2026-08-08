package ui;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import common.Action;
import common.Direction;
import game.Game;
import game.GameStatus;
import map.GameMap;
import persistence.HighscoreRepository;
import persistence.MapRepository;
import player.Player;
import service.BombService;
import service.GameService;
import service.MovementService;

// Das lokale Spiel im Fenster, in Echtzeit.
//
// Zwei Taktgeber, bewusst getrennt:
//   - die Spiellogik laeuft in festen Schritten von TICK_MS
//   - gezeichnet wird mit rund 60 Bildern pro Sekunde
//
// Dazwischen interpoliert das GamePanel. Die Logik bleibt dadurch
// rasterbasiert und vorhersagbar, die Bewegung sieht trotzdem fluessig aus.
// Wuerde man die Logik einfach 60-mal pro Sekunde ticken lassen, rasten die
// Figuren mit 60 Feldern pro Sekunde ueber die Karte.
public class SwingGame implements PlayerInput {
    private static final int TICK_MS = 220;
    private static final long TICK_NANOS = TICK_MS * 1_000_000L;
    private static final int FRAME_MS = 16;

    // Bei 220 ms pro Tick sind das rund zwei Sekunden Zuender und gut eine
    // halbe Sekunde Feuer. Auf der Konsole bleiben es 3 und 1 Zug.
    private static final int FUSE_TICKS = 9;
    private static final int EXPLOSION_TICKS = 3;

    private static final int MAP_WIDTH = 13;
    private static final int MAP_HEIGHT = 11;

    private final GameService service = new GameService(
        new MovementService(), new BombService(FUSE_TICKS, EXPLOSION_TICKS));
    private final HighscoreRepository highscores = new HighscoreRepository();
    private final List<PlayerKeys> keys = PlayerKeys.defaults();
    private final Path mapFile;
    private final int playerCount;

    private GameWindow window;
    private Game game;
    private Timer timer;

    // Welche Richtungen gerade gehalten werden, zuletzt gedrueckte zuletzt.
    private List<List<Direction>> held;
    private boolean[] bombRequested;

    private long lastFrameNanos;
    private long accumulator;

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

        window = new GameWindow("Bomberman", this);
        startRound(map);

        for (int i = 0; i < game.getPlayers().size(); i++) {
            window.bindPlayer(i, keys.get(i));
        }
        window.bindCommand(KeyEvent.VK_N, "neustart", this::restart);
        window.bindCommand(KeyEvent.VK_ESCAPE, "beenden", () -> System.exit(0));

        window.open();

        timer = new Timer(FRAME_MS, event -> frame());
        timer.start();
    }

    // Ein Bild. Holt so viele Logikschritte nach, wie seit dem letzten Bild
    // faellig geworden sind, und zeichnet den Rest als Zwischenstand.
    private void frame() {
        long now = System.nanoTime();
        accumulator += now - lastFrameNanos;
        lastFrameNanos = now;

        // Wer das Fenster verschiebt oder den Deckel zuklappt, kommt sonst
        // zu einem Zeitraffer, in dem alle Bomben auf einmal hochgehen.
        accumulator = Math.min(accumulator, TICK_NANOS * 3);

        while (accumulator >= TICK_NANOS && game.getStatus() == GameStatus.RUNNING) {
            accumulator -= TICK_NANOS;
            logicTick();
        }

        if (game.getStatus() == GameStatus.RUNNING) {
            window.setProgress(accumulator / (float) TICK_NANOS);
            window.repaintBoard();
        } else {
            finish();
        }
    }

    // Laeuft auf dem Event-Dispatch-Thread, genau wie die Tastendruecke.
    // Dadurch fasst nur ein einziger Thread den Spielzustand an und es
    // braucht kein synchronized - dasselbe Prinzip wie beim GameServer.
    private void logicTick() {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);

            if (!player.isAlive()) {
                bombRequested[i] = false;
                continue;
            }

            // Bombe zuerst, Bewegung danach: so legt man sie ab und laeuft
            // im selben Schritt weiter, statt einen Takt stehen zu bleiben.
            if (bombRequested[i]) {
                bombRequested[i] = false;
                service.applyAction(game, player, Action.BOMB);
            }

            List<Direction> directions = held.get(i);
            if (!directions.isEmpty()) {
                service.applyAction(game, player,
                    Action.move(directions.get(directions.size() - 1)));
            }
        }

        service.tick(game);
        window.advanceTo(game);
        showStatus();
    }

    @Override
    public void onPressed(int playerIndex, Action action) {
        if (game == null || game.getStatus() != GameStatus.RUNNING) {
            return;
        }

        if (action.getType() == Action.Type.BOMB) {
            bombRequested[playerIndex] = true;
            return;
        }

        if (action.getType() == Action.Type.MOVE) {
            // Erst entfernen, dann anhaengen: die zuletzt gedrueckte
            // Richtung gewinnt, auch wenn eine andere noch gehalten wird.
            List<Direction> directions = held.get(playerIndex);
            directions.remove(action.getDirection());
            directions.add(action.getDirection());
        }
    }

    @Override
    public void onReleased(int playerIndex, Action action) {
        if (action.getType() == Action.Type.MOVE && held != null) {
            held.get(playerIndex).remove(action.getDirection());
        }
    }

    private void restart() {
        GameMap map = loadMap();
        if (map == null) {
            return;
        }

        startRound(map);

        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    private void startRound(GameMap map) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player("Spieler " + (char) ('A' + i), map.getSpawnPositions().get(i)));
        }

        game = new Game(map, players);

        held = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            held.add(new ArrayList<>());
        }
        bombRequested = new boolean[playerCount];

        accumulator = 0;
        lastFrameNanos = System.nanoTime();

        service.start(game);
        window.setGameImmediately(game);
        showStatus();
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

    private void showStatus() {
        window.showStatus("Runde " + game.getRound() + "     " + GameWindow.describePlayers(game));
        window.showHint(GameWindow.describeKeys(keys, game.getPlayers().size()) + "     N: neues Spiel");
    }

    private void finish() {
        timer.stop();

        // Auf den Endstand einrasten, sonst bleibt eine Figur mitten
        // zwischen zwei Feldern stehen.
        window.setProgress(1f);
        window.repaintBoard();

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
