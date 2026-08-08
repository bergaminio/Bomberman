package network;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import common.Action;
import game.Game;
import ui.GameWindow;
import ui.PlayerInput;
import ui.PlayerKeys;

// Der Netzwerk-Client im Fenster.
//
// Zwei Threads, klar getrennt:
//   - ein Netzwerkthread liest den Socket und faellt nie ins Zeichnen,
//     sondern reicht ueber GameWindow.showState() an den EDT weiter
//   - der Event-Dispatch-Thread nimmt Tasten entgegen und schickt sie los
//
// Anders als das lokale Spiel bleibt das hier rundenbasiert: der Server
// wartet auf einen Zug pro Spieler. Ueber einen Hotspot ist das sogar von
// Vorteil, weil kein Lag-Ausgleich noetig ist.
public class SwingClient implements PlayerInput {
    private final String host;
    private final int port;
    private final String name;

    private GameWindow window;

    // volatile: der Netzwerkthread schreibt, der EDT liest.
    private volatile PrintWriter writer;
    private volatile boolean myTurn;

    private Game game;
    private int myIndex;

    public SwingClient(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : GameServer.DEFAULT_PORT;

        start(host, port, args.length > 2 ? args[2] : null);
    }

    // Ohne Namen fragt ein Dialog danach. Mit Namen startet der Client
    // ohne Rueckfrage, das ist beim Testen und bei mehreren Fenstern
    // hintereinander praktisch.
    public static void start(String host, int port, String givenName) {
        SwingUtilities.invokeLater(() -> {
            String name = givenName;

            if (name == null) {
                name = JOptionPane.showInputDialog(null,
                    "Dein Name:", "Bomberman", JOptionPane.QUESTION_MESSAGE);

                if (name == null) {
                    System.exit(0);
                }
            }

            new SwingClient(host, port, name.isBlank() ? "Spieler" : name).run();
        });
    }

    private void run() {
        window = new GameWindow("Bomberman - " + host + ":" + port, this);

        // Nur ein Spieler sitzt hier. Welche Farbe er im Spiel hat, sagt
        // der Server; getippt wird immer mit W A S D.
        window.bindPlayer(0, PlayerKeys.wasd());
        window.bindCommand(KeyEvent.VK_ESCAPE, "beenden", () -> System.exit(0));

        window.open();
        window.showStatus("Verbinde mit " + host + ":" + port + " ...");
        window.showHint("Warte auf Mitspieler.");

        new Thread(this::connect, "netzwerk").start();
    }

    private void connect() {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter socketWriter = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            writer = socketWriter;
            socketWriter.println(Message.of(Message.Type.JOIN, name).format());

            listen(reader);

        } catch (IOException cause) {
            window.showStatus("Verbindung fehlgeschlagen: " + cause.getMessage());
            window.showHint("Laeuft der Server, und stimmen Adresse und Port?");
        } finally {
            writer = null;
        }
    }

    private void listen(BufferedReader reader) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {

            if (line.equals(GameStateCodec.START)) {
                game = GameStateCodec.readFrom(reader);
                draw();
                continue;
            }

            Message message = Message.parse(line);

            switch (message.getType()) {
                case WELCOME:
                    myIndex = Integer.parseInt(message.getPart(0));
                    window.showHint("Du bist Spieler " + (char) ('A' + myIndex)
                        + " von " + message.getPart(1) + ".");
                    break;

                case YOUR_TURN:
                    myTurn = true;
                    window.showHint("Du bist dran:  W A S D bewegen, Leertaste Bombe, X warten.");
                    break;

                case INFO:
                    window.showHint(message.getPart(0));
                    break;

                case GAME_OVER:
                    showResult(message.getPart(0));
                    return;

                default:
                    break;
            }
        }

        window.showStatus("Der Server hat die Verbindung beendet.");
    }

    // Laeuft auf dem EDT. Der Schreibvorgang ist ein kurzer Schreibbefehl
    // in einen lokalen Socket-Puffer, das darf der EDT machen.
    @Override
    public void onAction(int playerIndex, Action action) {
        PrintWriter target = writer;

        if (!myTurn || target == null) {
            return;
        }

        myTurn = false;
        target.println(Message.of(Message.Type.ACTION, Message.encodeAction(action)).format());
        window.showHint("Zug abgeschickt, warte auf die anderen ...");
    }

    private void draw() {
        window.showState(game,
            "Runde " + game.getRound() + "     " + GameWindow.describePlayers(game),
            myTurn ? "Du bist dran." : "Warte ...");
    }

    private void showResult(String winner) {
        if (winner.isEmpty()) {
            window.showStatus("Unentschieden - niemand hat ueberlebt.");
        } else {
            window.showStatus(winner + " gewinnt!");
        }

        window.showHint("Esc zum Beenden.");
    }
}
