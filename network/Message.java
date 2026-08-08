package network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import common.Action;
import common.Direction;

// Der Umschlag: eine Zeile, ein Typ, beliebig viele Textteile, getrennt
// durch |. Was inhaltlich drinsteht, geht Message nichts an - den
// Spielzustand kodiert GameStateCodec.
public class Message {
    public enum Type {
        JOIN,
        WELCOME,
        YOUR_TURN,
        ACTION,
        INFO,
        GAME_OVER,
        ERROR
    }

    private static final String SEPARATOR = "|";

    private final Type type;
    private final List<String> parts;

    private Message(Type type, List<String> parts) {
        this.type = type;
        this.parts = parts;
    }

    public static Message of(Type type, String... parts) {
        List<String> clean = new ArrayList<>();

        // Ein | im Text wuerde die Zeile beim Empfaenger zerreissen.
        // Das Protokoll darf keine Zeichen durchlassen, die es selbst braucht.
        for (String part : parts) {
            clean.add(part.replace(SEPARATOR, "/"));
        }

        return new Message(type, clean);
    }

    public static Message parse(String line) {
        String[] pieces = line.split("\\" + SEPARATOR, -1);

        Type type;
        try {
            type = Type.valueOf(pieces[0]);
        } catch (IllegalArgumentException cause) {
            return new Message(Type.ERROR, List.of("Unbekannter Typ: " + pieces[0]));
        }

        List<String> parts = new ArrayList<>(Arrays.asList(pieces).subList(1, pieces.length));
        return new Message(type, parts);
    }

    public String format() {
        StringBuilder line = new StringBuilder(type.name());

        for (String part : parts) {
            line.append(SEPARATOR).append(part);
        }

        return line.toString();
    }

    public Type getType() {
        return type;
    }

    public String getPart(int index) {
        return index < parts.size() ? parts.get(index) : "";
    }

    // Das Netz spricht eine andere Sprache als die Tastatur. Der Spieler
    // tippt "w", ueber die Leitung geht "MOVE UP". So kann man die
    // Steuerung aendern, ohne das Protokoll anzufassen, und umgekehrt.
    public static String encodeAction(Action action) {
        if (action == null) {
            return "QUIT";
        }

        if (action.getType() == Action.Type.MOVE) {
            return "MOVE " + action.getDirection();
        }

        return action.getType().name();
    }

    // null heisst: der Spieler gibt auf oder hat Unsinn geschickt.
    public static Action decodeAction(String text) {
        if (text.startsWith("MOVE ")) {
            try {
                return Action.move(Direction.valueOf(text.substring("MOVE ".length())));
            } catch (IllegalArgumentException cause) {
                return null;
            }
        }

        if (text.equals("BOMB")) {
            return Action.BOMB;
        }

        if (text.equals("PASS")) {
            return Action.PASS;
        }

        return null;
    }
}
