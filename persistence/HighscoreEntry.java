package persistence;

// Ein Eintrag der Bestenliste. Unveraenderlich: ein zusaetzlicher Sieg
// erzeugt einen neuen Eintrag statt den alten zu aendern.
public class HighscoreEntry {
    private final String name;
    private final int wins;

    public HighscoreEntry(String name, int wins) {
        this.name = name;
        this.wins = wins;
    }

    public String getName() {
        return name;
    }

    public int getWins() {
        return wins;
    }

    public HighscoreEntry withOneMoreWin() {
        return new HighscoreEntry(name, wins + 1);
    }

    @Override
    public String toString() {
        return name + " " + wins;
    }
}
