package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Ein Thread pro verbundenem Spieler.
//
// Er tut genau zwei Dinge: Zeilen vom Socket lesen und Aktionen in eine
// Warteschlange legen. Den Spielzustand fasst er nie an - der gehoert
// allein dem Thread des GameServers. Dadurch braucht es rund um Game,
// Player und die Bombenliste keine einzige Sperre.
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final BlockingQueue<String> actions = new LinkedBlockingQueue<>();

    // volatile, weil der Serverthread dieses Feld liest, waehrend dieser
    // Thread es schreibt.
    private volatile boolean connected = true;

    private String playerName = "Spieler";

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    // Wird vor dem Start des Threads aufgerufen, solange noch niemand spielt.
    public String awaitJoin() throws IOException {
        String line = reader.readLine();

        if (line == null) {
            throw new IOException("Verbindung wurde vor dem Beitritt geschlossen.");
        }

        Message message = Message.parse(line);

        if (message.getType() != Message.Type.JOIN) {
            throw new IOException("Erwartet wurde JOIN, empfangen: " + line);
        }

        playerName = message.getPart(0).isBlank() ? "Spieler" : message.getPart(0);
        return playerName;
    }

    @Override
    public void run() {
        try {
            String line;

            while ((line = reader.readLine()) != null) {
                Message message = Message.parse(line);

                if (message.getType() == Message.Type.ACTION) {
                    actions.put(message.getPart(0));
                }
            }
        } catch (IOException cause) {
            // Verbindung weg. Faellt unten in dieselbe Aufraeumarbeit.
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
        } finally {
            connected = false;

            // Ohne das wartet der Serverthread ewig in takeAction(), wenn
            // dieser Spieler mitten in der Runde die Verbindung verliert.
            actions.offer("QUIT");

            close();
        }
    }

    public String takeAction() throws InterruptedException {
        return actions.take();
    }

    // synchronized, weil der Serverthread broadcastet und hier theoretisch
    // zwei Sendungen ineinanderlaufen koennten.
    public synchronized void send(Message message) {
        writer.println(message.format());
    }

    public synchronized void sendLines(List<String> lines) {
        for (String line : lines) {
            writer.println(line);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Beim Schliessen ist ein Fehler nicht mehr zu gebrauchen.
        }
    }
}
