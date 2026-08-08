package service;

import bomb.Explosion;
import common.Action;
import common.Direction;
import common.Position;
import game.Game;
import game.GameStatus;
import player.Player;

// Aendert den Spielzustand. Game selbst haelt ihn nur.
public class GameService {
    private final MovementService movementService;
    private final BombService bombService;

    public GameService() {
        this(new MovementService(), new BombService());
    }

    public GameService(MovementService movementService, BombService bombService) {
        this.movementService = movementService;
        this.bombService = bombService;
    }

    public void start(Game game) {
        if (game.getStatus() == GameStatus.WAITING) {
            game.setStatus(GameStatus.RUNNING);
        }
    }

    // Die Aktion eines einzelnen Spielers. Aendert nur ihn, nie die Uhr.
    // Liefert false, wenn die Aktion nicht ausgefuehrt werden konnte.
    public boolean applyAction(Game game, Player player, Action action) {
        if (game.getStatus() != GameStatus.RUNNING || !player.isAlive()) {
            return false;
        }

        switch (action.getType()) {
            case MOVE:
                return tryMove(game, player, action.getDirection());
            case BOMB:
                return bombService.placeBomb(player, game.getBombs()) != null;
            default:
                return true;
        }
    }

    // Ein Tick der Spielwelt, nachdem alle Spieler gezogen haben.
    public void tick(Game game) {
        if (game.getStatus() != GameStatus.RUNNING) {
            return;
        }

        // Erst das Feuer vom letzten Tick loeschen, dann das neue zuenden.
        // Sonst wuerde eine Explosion zwei Runden lang toeten.
        bombService.tickExplosions(game.getExplosions());
        game.getExplosions().addAll(bombService.tickBombs(game.getBombs(), game.getMap()));

        killPlayersInExplosions(game);
        game.nextRound();
        updateStatus(game);
    }

    // Die Bombenpruefung sitzt hier und nicht im MovementService, weil der
    // die Bombenliste nicht kennen soll. Er weiss nur ueber die Map Bescheid.
    private boolean tryMove(Game game, Player player, Direction direction) {
        Position target = player.getPosition().move(direction);

        if (bombService.findBombAt(game.getBombs(), target) != null) {
            return false;
        }

        return movementService.tryMove(player, direction, game.getMap());
    }

    private void killPlayersInExplosions(Game game) {
        for (Player player : game.getPlayers()) {
            if (!player.isAlive()) {
                continue;
            }

            for (Explosion explosion : game.getExplosions()) {
                if (explosion.covers(player.getPosition())) {
                    player.die();
                    break;
                }
            }
        }
    }

    private void updateStatus(Game game) {
        int alive = game.getAlivePlayers().size();

        // Allein zum Ueben: das Spiel endet erst, wenn man sich selbst sprengt.
        if (game.getPlayers().size() == 1) {
            if (alive == 0) {
                game.setStatus(GameStatus.FINISHED);
            }
            return;
        }

        if (alive <= 1) {
            game.setStatus(GameStatus.FINISHED);
        }
    }
}
