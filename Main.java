  import common.Direction;
  import common.Position;
  import player.Player;

  public class Main {
      public static void main(String[] args) {
          Position position = new Position(4, 2);
          Position newPosition = position.move(Direction.RIGHT);

          Player player = new Player("Michael", newPosition);

          System.out.println(player.getName());
          System.out.println(player.getPosition().getX());
          System.out.println(player.getPosition().getY());
          System.out.println(player.getStatus());

          player.die();

          System.out.println(player.getStatus());
      }
  }