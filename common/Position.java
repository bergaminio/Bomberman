package common;

public class Position {
    private int x;
    private int y;

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
}