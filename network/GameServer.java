package network;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import common.Action;
import game.Game;
import game.GameStatus;
import map.GameMap;
import player.Player;
import service.GameService;

// Der Server ist die einzige Wahrheit. Clients schicken nur, was sie tun
// WOLLEN. Was daraus wird, rechnet ausschliesslich dieser Prozess aus.
//
// Der Spielzustand gehoert genau einem Thread: diesem hier. Die
// ClientHandler-Threads reichen Aktionen ueber eine BlockingQueue herein
// und fassen Game nie selbst an.
public class GameServer {
    public static final int DEFAULT_PORT = 5555;

    private static final int MAP_WIDTH = 13;
    private static final int MAP_HEIGHT = 11;

    private final int port;
    private final int expectedPlayers;
    private final GameService service = new GameService();

    public GameServer(int port, int expectedPlayers) {
        this.port = port;
        this.expectedPlayers = expectedPlayers;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        int players = args.length > 1 ? Integer.parseInt(args[1]) : 2;

        new GameServer(port, players).run();
    }

    public void run() throws IOException, InterruptedException {
        // ServerSocket ohne Adresse bindet auf allen Netzwerkkarten. Damit
        // ist der Server im Hotspot erreichbar, ohne dass man etwas einstellt.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            printAddresses();

            List<ClientHandler> handlers = acceptPlayers(serverSocket);
            GameMap map = GameMap.generateStandardMap(MAP_WIDTH, MAP_HEIGHT);
            Game game = new Game(map, createPlayers(handlers, map));

            welcome(handlers);
            play(handlers, game);
            finish(handlers, game);
        }
    }

    private List<ClientHandler> acceptPlayers(ServerSocket serverSocket) throws IOException {
        List<ClientHandler> handlers = new ArrayList<>();

        System.out.println("Warte auf " + expectedPlayers + " Spieler ...");

        while (handlers.size() < expectedPlayers) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);

            String name = handler.awaitJoin();
            handlers.add(handler);

            System.out.println("  " + name + " ist da (" + handlers.size()
                + "/" + expectedPlayers + ")");

            // Erst jetzt den Lesethread starten. Vorher haette er das
            // JOIN weggelesen, auf das awaitJoin() wartet.
            new Thread(handler, "client-" + handlers.size()).start();
        }

        return handlers;
    }

    private List<Player> createPlayers(List<ClientHandler> handlers, GameMap map) {
        List<Player> players = new ArrayList<>();

        for (int i = 0; i < handlers.size(); i++) {
            players.add(new Player(handlers.get(i).getPlayerName(), map.getSpawnPositions().get(i)));
        }

        return players;
    }

    private void welcome(List<ClientHandler> handlers) {
        for (int i = 0; i < handlers.size(); i++) {
            handlers.get(i).send(Message.of(Message.Type.WELCOME,
                String.valueOf(i), String.valueOf(handlers.size())));
        }
    }

    private void play(List<ClientHandler> handlers, Game game) throws InterruptedException {
        service.start(game);

        while (game.getStatus() == GameStatus.RUNNING) {
            broadcastState(handlers, game);
            askEveryone(handlers, game);
            collectActions(handlers, game);

            service.tick(game);
        }
    }

    // Erst alle fragen, dann alle Antworten einsammeln. So tippen die
    // Spieler gleichzeitig statt nacheinander zu warten.
    private void askEveryone(List<ClientHandler> handlers, Game game) {
        for (int i = 0; i < handlers.size(); i++) {
            if (isPlaying(handlers, game, i)) {
                handlers.get(i).send(Message.of(Message.Type.YOUR_TURN));
            }
        }
    }

    private void collectActions(List<ClientHandler> handlers, Game game) throws InterruptedException {
        for (int i = 0; i < handlers.size(); i++) {
            if (!isPlaying(handlers, game, i)) {
                continue;
            }

            Player player = game.getPlayers().get(i);
            Action action = Message.decodeAction(handlers.get(i).takeAction());

            if (action == null) {
                // Aufgegeben oder Verbindung verloren: scheidet aus.
                player.die();
                broadcast(handlers, Message.of(Message.Type.INFO,
                    player.getName() + " hat aufgegeben."));
                continue;
            }

            service.applyAction(game, player, action);
        }
    }

    private boolean isPlaying(List<ClientHandler> handlers, Game game, int index) {
        return game.getPlayers().get(index).isAlive() && handlers.get(index).isConnected();
    }

    private void finish(List<ClientHandler> handlers, Game game) {
        broadcastState(handlers, game);

        Player winner = game.getWinner();
        String result = winner != null ? winner.getName() : "";

        broadcast(handlers, Message.of(Message.Type.GAME_OVER, result));
        System.out.println("Spiel beendet nach " + game.getRound() + " Runden. Sieger: "
            + (winner != null ? winner.getName() : "niemand"));

        for (ClientHandler handler : handlers) {
            handler.close();
        }
    }

    private void broadcastState(List<ClientHandler> handlers, Game game) {
        List<String> lines = GameStateCodec.encode(game);

        for (ClientHandler handler : handlers) {
            handler.sendLines(lines);
        }
    }

    private void broadcast(List<ClientHandler> handlers, Message message) {
        for (ClientHandler handler : handlers) {
            handler.send(message);
        }
    }

    private void printAddresses() {
        System.out.println("=== BOMBERMAN SERVER ===");
        System.out.println("Port " + port);
        System.out.println("Auf demselben PC verbinden mit:  127.0.0.1");

        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }

                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address) {
                        System.out.println("Im selben WLAN/Hotspot mit:      "
                            + address.getHostAddress() + "   (" + nic.getDisplayName() + ")");
                    }
                }
            }
        } catch (SocketException cause) {
            // Adressen anzeigen ist reiner Komfort, der Server laeuft trotzdem.
            System.out.println("(Adressen konnten nicht ermittelt werden: " + cause.getMessage() + ")");
        }

        System.out.println();
    }
}
