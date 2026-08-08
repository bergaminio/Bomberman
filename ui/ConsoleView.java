package ui;

import java.util.Scanner;

import bomb.Bomb;
import bomb.Explosion;
import common.Action;
import common.Direction;
import common.Position;
import game.Game;
import game.GameStatus;
import map.GameMap;
import map.Tile;
import player.Player;

// Zeigt den Spielzustand an und liest Eingaben.
// Veraendert nie etwas am Spiel: hier gibt es keinen Aufruf, der ein
// Game, einen Player oder eine Bomb aendert. Alles laeuft ueber Getter.
public class ConsoleView {
    private final Scanner scanner;

    public ConsoleView() {
        this(new Scanner(System.in));
    }

    public ConsoleView(Scanner scanner) {
        this.scanner = scanner;
    }

    public void render(Game game) {
        GameMap map = game.getMap();

        System.out.println();
        for (int y = 0; y < map.getHeight(); y++) {
            StringBuilder line = new StringBuilder();

            for (int x = 0; x < map.getWidth(); x++) {
                line.append(cellFor(game, new Position(x, y)));
            }

            System.out.println(line.toString());
        }

        System.out.println("Runde " + game.getRound() + "   " + statusLine(game));
    }

    public void showLegend() {
        System.out.println("Zeichen:  #  Mauer      o  Block      *  Feuer      .  Boden");
        System.out.println("          A  Spieler    x  Toter      3  Zuender    A3 Spieler auf Bombe");
        System.out.println("Steuerung: w a s d = gehen, b = Bombe legen, x = warten, q = beenden");
    }

    // Liefert null, wenn der Spieler abbrechen will oder die Eingabe endet.
    public Action askForAction(Player player, char letter) {
        while (true) {
            System.out.print(letter + " " + player.getName() + " > ");

            if (!scanner.hasNextLine()) {
                System.out.println();
                return null;
            }

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("q")) {
                return null;
            }

            Action action = parseAction(input);
            if (action != null) {
                return action;
            }

            System.out.println("  \"" + input + "\" kenne ich nicht. Erlaubt: w a s d b x q");
        }
    }

    public String askForName(String prompt, String fallback) {
        System.out.print(prompt + " [" + fallback + "]: ");

        if (!scanner.hasNextLine()) {
            System.out.println(fallback);
            return fallback;
        }

        String input = scanner.nextLine().trim();
        return input.isEmpty() ? fallback : input;
    }

    public int askForNumber(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");

            if (!scanner.hasNextLine()) {
                System.out.println(min);
                return min;
            }

            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Faellt unten in dieselbe Fehlermeldung wie eine Zahl
                // ausserhalb des Bereichs.
            }

            System.out.println("  Bitte eine ganze Zahl zwischen " + min + " und " + max + ".");
        }
    }

    public void showMessage(String message) {
        System.out.println("  " + message);
    }

    public void showResult(Game game) {
        System.out.println();

        if (game.getStatus() != GameStatus.FINISHED) {
            System.out.println("=== Abgebrochen ===");
            return;
        }

        Player winner = game.getWinner();
        if (winner == null) {
            System.out.println("=== Unentschieden, niemand hat ueberlebt ===");
            return;
        }

        System.out.println("=== " + winner.getName() + " gewinnt nach "
            + game.getRound() + " Runden ===");
    }

    public void close() {
        scanner.close();
    }

    private Action parseAction(String input) {
        switch (input) {
            case "w": return Action.move(Direction.UP);
            case "s": return Action.move(Direction.DOWN);
            case "a": return Action.move(Direction.LEFT);
            case "d": return Action.move(Direction.RIGHT);
            case "b": return Action.BOMB;
            case "x": return Action.PASS;
            default:  return null;
        }
    }

    // Jede Zelle ist zwei Zeichen breit. Das zweite Zeichen ist normalerweise
    // ein Leerzeichen, zeigt aber den Zuender, wenn ein Spieler auf einer
    // Bombe steht. Sonst waere die Bombe unter ihm unsichtbar.
    private String cellFor(Game game, Position position) {
        for (Explosion explosion : game.getExplosions()) {
            if (explosion.covers(position)) {
                return "* ";
            }
        }

        Bomb bomb = bombAt(game, position);

        for (Player player : game.getPlayers()) {
            if (player.getPosition().equals(position)) {
                char who = player.isAlive() ? letterFor(game, player) : 'x';
                return "" + who + (bomb == null ? ' ' : fuseDigit(bomb));
            }
        }

        if (bomb != null) {
            return "" + fuseDigit(bomb) + ' ';
        }

        Tile tile = game.getMap().getTile(position);
        if (tile.getBlock() != null) {
            return tile.getBlock().isDestroyable() ? "o " : "# ";
        }

        return ". ";
    }

    private Bomb bombAt(Game game, Position position) {
        for (Bomb bomb : game.getBombs()) {
            if (bomb.getPosition().equals(position)) {
                return bomb;
            }
        }
        return null;
    }

    // indexOf() vergleicht hier mit Objektidentitaet, weil Player kein
    // equals() hat. Genau richtig: gemeint ist dieser eine Spieler.
    public char letterFor(Game game, Player player) {
        return (char) ('A' + game.getPlayers().indexOf(player));
    }

    private char fuseDigit(Bomb bomb) {
        return (char) ('0' + Math.min(9, bomb.getFuseTicks()));
    }

    private String statusLine(Game game) {
        StringBuilder line = new StringBuilder();

        for (Player player : game.getPlayers()) {
            if (line.length() > 0) {
                line.append("   ");
            }

            line.append(letterFor(game, player)).append(' ').append(player.getName());

            if (player.isAlive()) {
                line.append(" ").append(player.getPosition());
                line.append(" [").append(player.getActiveBombs());
                line.append("/").append(player.getBombCapacity()).append("]");
            } else {
                line.append(" tot");
            }
        }

        return line.toString();
    }
}
