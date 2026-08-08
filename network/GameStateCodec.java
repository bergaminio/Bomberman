package network;

import java.util.ArrayList;
import java.util.List;

import bomb.Bomb;
import bomb.Explosion;
import common.Position;
import game.Game;
import game.GameStatus;
import map.Block;
import map.BlockType;
import map.GameMap;
import map.Tile;
import player.Player;

// Verpackt einen Spielzustand in Textzeilen und wieder zurueck.
// Mehrzeilig mit Endmarke statt alles in eine Zeile zu quetschen: das
// Format bleibt lesbar, und man kann sich per telnet anschauen, was
// wirklich ueber die Leitung geht.
public class GameStateCodec {
    public static final String START = "STATE";
    public static final String END = "END";

    private static final char GROUND = '.';
    private static final char DESTROYABLE = 'o';
    private static final char INDESTRUCTIBLE = '#';

    public static List<String> encode(Game game) {
        List<String> lines = new ArrayList<>();
        GameMap map = game.getMap();

        lines.add(START);
        lines.add("SIZE " + map.getWidth() + " " + map.getHeight());

        for (int y = 0; y < map.getHeight(); y++) {
            StringBuilder row = new StringBuilder();

            for (int x = 0; x < map.getWidth(); x++) {
                row.append(symbolFor(map.getTile(new Position(x, y))));
            }

            lines.add("ROW " + row);
        }

        // Der Name steht bewusst zuletzt, damit er Leerzeichen enthalten darf.
        for (Player player : game.getPlayers()) {
            lines.add("PLAYER " + player.getPosition().getX()
                + " " + player.getPosition().getY()
                + " " + (player.isAlive() ? "ALIVE" : "DEAD")
                + " " + player.getActiveBombs()
                + " " + player.getBombCapacity()
                + " " + player.getBlastRadius()
                + " " + player.getName());
        }

        for (Bomb bomb : game.getBombs()) {
            lines.add("BOMB " + bomb.getPosition().getX()
                + " " + bomb.getPosition().getY()
                + " " + bomb.getFuseTicks()
                + " " + bomb.getBlastRadius()
                + " " + game.getPlayers().indexOf(bomb.getOwner()));
        }

        for (Explosion explosion : game.getExplosions()) {
            StringBuilder line = new StringBuilder("FIRE " + explosion.getRemainingTicks());

            for (Position position : explosion.getAffectedPositions()) {
                line.append(' ').append(position.getX()).append(',').append(position.getY());
            }

            lines.add(line.toString());
        }

        lines.add("META " + game.getRound() + " " + game.getStatus());
        lines.add(END);

        return lines;
    }

    public static Game decode(List<String> lines) {
        GameMap map = null;
        int row = 0;

        List<Player> players = new ArrayList<>();
        List<String[]> bombLines = new ArrayList<>();
        List<String[]> fireLines = new ArrayList<>();

        int round = 0;
        GameStatus status = GameStatus.RUNNING;

        for (String line : lines) {
            String[] parts = line.split(" ");

            switch (parts[0]) {
                case "SIZE":
                    map = new GameMap(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    break;

                case "ROW":
                    applyRow(map, row++, parts[1]);
                    break;

                case "PLAYER":
                    players.add(decodePlayer(line));
                    break;

                case "BOMB":
                    bombLines.add(parts);
                    break;

                case "FIRE":
                    fireLines.add(parts);
                    break;

                case "META":
                    round = Integer.parseInt(parts[1]);
                    status = GameStatus.valueOf(parts[2]);
                    break;

                default:
                    // START, END und alles Unbekannte einfach ueberspringen.
                    break;
            }
        }

        if (map == null) {
            throw new IllegalArgumentException("Zustand ohne SIZE-Zeile empfangen.");
        }

        Game game = new Game(map, players);
        game.setRound(round);
        game.setStatus(status);

        for (String[] parts : bombLines) {
            Position position = new Position(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            Player owner = players.get(Integer.parseInt(parts[5]));
            game.getBombs().add(new Bomb(owner, position,
                Integer.parseInt(parts[4]), Integer.parseInt(parts[3])));
        }

        for (String[] parts : fireLines) {
            List<Position> affected = new ArrayList<>();

            for (int i = 2; i < parts.length; i++) {
                String[] xy = parts[i].split(",");
                affected.add(new Position(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }

            game.getExplosions().add(new Explosion(affected, Integer.parseInt(parts[1])));
        }

        return game;
    }

    private static Player decodePlayer(String line) {
        // Sieben Teile: PLAYER x y status aktiv kapazitaet radius, dann der
        // Rest als Name. Das Limit sorgt dafuer, dass Namen mit Leerzeichen
        // nicht zerfallen.
        String[] parts = line.split(" ", 8);

        Position position = new Position(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        Player player = new Player(parts.length > 7 ? parts[7] : "?", position);

        int activeBombs = Integer.parseInt(parts[4]);
        for (int i = 0; i < activeBombs; i++) {
            player.bombPlaced();
        }

        if (parts[3].equals("DEAD")) {
            player.die();
        }

        return player;
    }

    private static void applyRow(GameMap map, int y, String row) {
        for (int x = 0; x < row.length(); x++) {
            Tile tile = map.getTile(new Position(x, y));

            if (row.charAt(x) == DESTROYABLE) {
                tile.setBlock(new Block(BlockType.DESTROYABLE));
            } else if (row.charAt(x) == INDESTRUCTIBLE) {
                tile.setBlock(new Block(BlockType.INDESTRUCTIBLE));
            }
        }
    }

    private static char symbolFor(Tile tile) {
        if (tile.getBlock() == null) {
            return GROUND;
        }

        return tile.getBlock().isDestroyable() ? DESTROYABLE : INDESTRUCTIBLE;
    }
}
