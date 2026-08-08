package common;

// Was ein Spieler in einer Runde tun kann.
// Bei MOVE steht in direction wohin, bei BOMB und PASS ist direction null.
public class Action {
    public enum Type {
        MOVE,
        BOMB,
        PASS
    }

    // BOMB und PASS tragen keine Daten, darum reicht je eine gemeinsame
    // Instanz. Erlaubt ist das nur, weil Action unveraenderlich ist.
    public static final Action BOMB = new Action(Type.BOMB, null);
    public static final Action PASS = new Action(Type.PASS, null);

    private final Type type;
    private final Direction direction;

    private Action(Type type, Direction direction) {
        this.type = type;
        this.direction = direction;
    }

    public static Action move(Direction direction) {
        return new Action(Type.MOVE, direction);
    }

    public Type getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return type == Type.MOVE ? "MOVE " + direction : type.toString();
    }
}
