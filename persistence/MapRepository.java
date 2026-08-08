package persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import common.Position;
import map.Block;
import map.BlockType;
import map.GameMap;
import map.Tile;
import service.MapValidator;

// Laedt und speichert Maps als Textdatei.
// Zwei Fehlerarten, bewusst getrennt gehalten:
//   IOException            - die Datei liess sich nicht lesen oder schreiben
//   IllegalArgumentException - die Datei war lesbar, ihr Inhalt ist Unsinn
public class MapRepository {
    private static final char GROUND = '.';
    private static final char DESTROYABLE = 'o';
    private static final char INDESTRUCTIBLE = '#';

    private final MapValidator validator;

    public MapRepository() {
        this(new MapValidator());
    }

    // Der Validator wird hereingereicht statt selbst erzeugt. Dadurch ist
    // sichtbar, wovon diese Klasse abhaengt, und man kann sie austauschen.
    public MapRepository(MapValidator validator) {
        this.validator = validator;
    }

    public GameMap load(Path file) throws IOException {
        List<String> lines = readLines(file);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Die Datei enthaelt keine Map.");
        }

        int height = lines.size();
        int width = lines.get(0).length();
        GameMap map = new GameMap(width, height);

        for (int y = 0; y < height; y++) {
            String line = lines.get(y);

            if (line.length() != width) {
                throw new IllegalArgumentException("Zeile " + (y + 1) + " ist "
                    + line.length() + " Zeichen lang, erwartet waren " + width + ".");
            }

            for (int x = 0; x < width; x++) {
                applySymbol(map, x, y, line.charAt(x));
            }
        }

        // Eine Map von der Platte ist per Hand geschrieben und darum
        // verdaechtig. Der Validator aus Schritt 1 prueft sie, bevor sie
        // ins Spiel geht.
        List<String> problems = validator.findProblems(map);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Map ist nicht spielbar: "
                + String.join(" ", problems));
        }

        return map;
    }

    public void save(GameMap map, Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (int y = 0; y < map.getHeight(); y++) {
                StringBuilder line = new StringBuilder();

                for (int x = 0; x < map.getWidth(); x++) {
                    line.append(symbolFor(map.getTile(new Position(x, y))));
                }

                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    private List<String> readLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Leerzeilen am Ende der Datei sind kein Kartenteil.
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }

        return lines;
    }

    private void applySymbol(GameMap map, int x, int y, char symbol) {
        Tile tile = map.getTile(new Position(x, y));

        switch (symbol) {
            case GROUND:
                break;
            case DESTROYABLE:
                tile.setBlock(new Block(BlockType.DESTROYABLE));
                break;
            case INDESTRUCTIBLE:
                tile.setBlock(new Block(BlockType.INDESTRUCTIBLE));
                break;
            default:
                throw new IllegalArgumentException("Unbekanntes Zeichen '" + symbol
                    + "' bei " + x + "/" + y + ". Erlaubt sind "
                    + GROUND + " " + DESTROYABLE + " " + INDESTRUCTIBLE + ".");
        }
    }

    private char symbolFor(Tile tile) {
        if (tile.getBlock() == null) {
            return GROUND;
        }

        return tile.getBlock().isDestroyable() ? DESTROYABLE : INDESTRUCTIBLE;
    }
}
