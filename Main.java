import java.nio.file.Path;

import network.GameClient;
import network.GameServer;
import network.SwingClient;
import ui.ConsoleGame;
import ui.SwingGame;

// Ein Einstiegspunkt fuer alle Spielarten. Ohne Argument startet das
// Fenster, weil das die Variante ist, die man normalerweise will.
public class Main {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase() : "gui";

        switch (mode) {
            case "gui":
                SwingGame.start(numberOr(args, 1, 2), pathOr(args, 2));
                break;

            case "konsole":
                ConsoleGame.start(pathOr(args, 1));
                break;

            case "server":
                new GameServer(numberOr(args, 1, GameServer.DEFAULT_PORT),
                    numberOr(args, 2, 2)).run();
                break;

            case "client":
                SwingClient.start(textOr(args, 1, "127.0.0.1"),
                    numberOr(args, 2, GameServer.DEFAULT_PORT),
                    args.length > 3 ? args[3] : null);
                break;

            case "client-konsole":
                new GameClient(textOr(args, 1, "127.0.0.1"),
                    numberOr(args, 2, GameServer.DEFAULT_PORT)).run();
                break;

            default:
                showUsage(mode);
                break;
        }
    }

    private static void showUsage(String unknown) {
        System.out.println("Unbekannter Modus: " + unknown);
        System.out.println();
        System.out.println("  gui [spieler] [mapdatei]     Fenster, Echtzeit, 2-4 Spieler (Standard)");
        System.out.println("  konsole [mapdatei]           Konsole, rundenbasiert");
        System.out.println("  server [port] [spieler]      Server fuer das Netzwerkspiel");
        System.out.println("  client [host] [port]         Netzwerkspiel im Fenster");
        System.out.println("  client-konsole [host] [port] Netzwerkspiel auf der Konsole");
        System.out.println();
        System.out.println("Beispiele:");
        System.out.println("  .\\run.ps1");
        System.out.println("  .\\run.ps1 -ProgramArgs gui,3");
        System.out.println("  .\\run.ps1 -ProgramArgs konsole,maps\\arena.txt");
        System.out.println("  .\\run.ps1 -ProgramArgs server,5555,2");
        System.out.println("  .\\run.ps1 -ProgramArgs client,127.0.0.1,5555");
    }

    private static String textOr(String[] args, int index, String fallback) {
        return index < args.length ? args[index] : fallback;
    }

    private static int numberOr(String[] args, int index, int fallback) {
        if (index >= args.length) {
            return fallback;
        }

        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException cause) {
            System.out.println("\"" + args[index] + "\" ist keine Zahl, nehme " + fallback + ".");
            return fallback;
        }
    }

    private static Path pathOr(String[] args, int index) {
        return index < args.length ? Path.of(args[index]) : null;
    }
}
