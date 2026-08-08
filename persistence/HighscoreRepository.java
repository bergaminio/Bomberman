package persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Kapselt den Dateizugriff auf die Bestenliste. Der Rest des Programms
// weiss nicht, dass es eine Datei ist, und schon gar nicht in welchem Format.
public class HighscoreRepository {
    private static final String DEFAULT_FILE = "highscores.txt";
    private static final String SEPARATOR = ";";

    private final Path file;

    public HighscoreRepository() {
        this(Path.of(DEFAULT_FILE));
    }

    public HighscoreRepository(Path file) {
        this.file = file;
    }

    // Sortiert: meiste Siege zuerst, bei Gleichstand alphabetisch.
    public List<HighscoreEntry> load() throws IOException {
        List<HighscoreEntry> entries = new ArrayList<>();

        // Noch nie gespielt ist kein Fehler, sondern eine leere Liste.
        if (!Files.exists(file)) {
            return entries;
        }

        // try-with-resources: der Reader wird geschlossen, auch wenn im
        // Block eine Exception fliegt. Ohne das bliebe die Datei offen.
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int number = 0;

            while ((line = reader.readLine()) != null) {
                number++;

                if (line.isBlank()) {
                    continue;
                }

                entries.add(parseLine(line, number));
            }
        }

        entries.sort((left, right) -> {
            int byWins = Integer.compare(right.getWins(), left.getWins());
            return byWins != 0 ? byWins : left.getName().compareTo(right.getName());
        });

        return entries;
    }

    public void save(List<HighscoreEntry> entries) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (HighscoreEntry entry : entries) {
                writer.write(entry.getName() + SEPARATOR + entry.getWins());
                writer.newLine();
            }
        }
    }

    public void addWin(String playerName) throws IOException {
        List<HighscoreEntry> entries = load();

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getName().equals(playerName)) {
                entries.set(i, entries.get(i).withOneMoreWin());
                save(entries);
                return;
            }
        }

        entries.add(new HighscoreEntry(playerName, 1));
        save(entries);
    }

    // lastIndexOf statt indexOf: so darf ein Spielername das Trennzeichen
    // enthalten, nur die Zahl dahinter muss sauber sein.
    private HighscoreEntry parseLine(String line, int number) {
        int separator = line.lastIndexOf(SEPARATOR);

        if (separator < 1) {
            throw new IllegalArgumentException(
                "Zeile " + number + " hat kein \"" + SEPARATOR + "\": " + line);
        }

        String name = line.substring(0, separator);
        String winsText = line.substring(separator + SEPARATOR.length()).trim();

        try {
            return new HighscoreEntry(name, Integer.parseInt(winsText));
        } catch (NumberFormatException cause) {
            throw new IllegalArgumentException(
                "Zeile " + number + " hat keine Zahl hinter dem Trennzeichen: " + winsText, cause);
        }
    }
}
