package common;

import java.util.Objects;

// Unveraenderlich: x und y sind final, move() liefert immer ein neues Objekt.
// Dadurch koennen sich mehrere Spieler gefahrlos dieselbe Position teilen.
public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Position move(Direction direction) {
        if (direction == Direction.UP) {
            return new Position(x, y - 1);
        }

        if (direction == Direction.DOWN) {
            return new Position(x, y + 1);
        }

        if (direction == Direction.LEFT) {
            return new Position(x - 1, y);
        }

        if (direction == Direction.RIGHT) {
            return new Position(x + 1, y);
        }

        return this;
    }

    // Zwei Positionen sind gleich, wenn x und y gleich sind, nicht wenn es
    // dasselbe Objekt ist. Ohne das findet contains() in einer Liste nichts.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Position)) {
            return false;
        }

        Position that = (Position) other;
        return this.x == that.x && this.y == that.y;
    }

    // Muss zu equals() passen: gleiche Objekte, gleicher Hash.
    // Sonst funktionieren HashSet und HashMap nicht richtig.
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return x + "/" + y;
    }
}