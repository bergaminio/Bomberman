package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import common.Action;
import game.Game;
import player.Player;
import ui.ConsoleView;

// Der Client rechnet nichts aus. Er zeigt an, was der Server schickt, und
// schickt zurueck, was der Spieler tippen will. Alle Regeln laufen auf dem
// Server - sonst koennten zwei Clients zu verschiedenen Ergebnissen kommen.
public class GameClient {
    private final String host;
    private final int port;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : GameServer.DEFAULT_PORT;

        new GameClient(host, port).run();
    }

    public void run() throws IOException {
        ConsoleView view = new ConsoleView();

        System.out.println("=== BOMBERMAN CLIENT ===");
        view.showLegend();
        System.out.println();

        String name = view.askForName("Dein Name", "Spieler");

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("Verbunden mit " + host + ":" + port + ", warte auf Mitspieler ...");
            writer.println(Message.of(Message.Type.JOIN, name).format());

            listen(view, reader, writer);

        } catch (IOException cause) {
            view.showMessage("Verbindung fehlgeschlagen: " + cause.getMessage());
            view.showMessage("Laeuft der Server, und stimmen Adresse und Port?");
        } finally {
            view.close();
        }
    }

    private void listen(ConsoleView view, BufferedReader reader, PrintWriter writer) throws IOException {
        Game game = null;
        int myIndex = 0;
        String line;

        while ((line = reader.readLine()) != null) {

            // Ein Zustand kommt mehrzeilig und endet mit einer Endmarke.
            if (line.equals(GameStateCodec.START)) {
                game = GameStateCodec.readFrom(reader);
                view.render(game);
                continue;
            }

            Message message = Message.parse(line);

            switch (message.getType()) {
                case WELCOME:
                    myIndex = Integer.parseInt(message.getPart(0));
                    view.showMessage("Du bist Spieler " + (char) ('A' + myIndex)
                        + " von " + message.getPart(1) + ".");
                    break;

                case YOUR_TURN:
                    sendAction(view, writer, game, myIndex);
                    break;

                case INFO:
                    view.showMessage(message.getPart(0));
                    break;

                case GAME_OVER:
                    showResult(view, message.getPart(0));
                    return;

                default:
                    view.showMessage("Unerwartete Nachricht: " + line);
                    break;
            }
        }

        view.showMessage("Der Server hat die Verbindung beendet.");
    }

    private void sendAction(ConsoleView view, PrintWriter writer, Game game, int myIndex) {
        Player me = game.getPlayers().get(myIndex);
        Action action = view.askForAction(me, (char) ('A' + myIndex));

        writer.println(Message.of(Message.Type.ACTION, Message.encodeAction(action)).format());

        if (action == null) {
            view.showMessage("Du hast aufgegeben.");
        }
    }

    private void showResult(ConsoleView view, String winner) {
        System.out.println();

        if (winner.isEmpty()) {
            System.out.println("=== Unentschieden, niemand hat ueberlebt ===");
        } else {
            System.out.println("=== " + winner + " gewinnt ===");
        }
    }
}
