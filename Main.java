import common.Direction;
import common.Position;

public class Main {
    public static void main(String[] args) {
        Position position = new Position(4, 2);
        Position newPosition = position.move(Direction.RIGHT);

        System.out.println(newPosition.getX());
        System.out.println(newPosition.getY());
    }
}
    