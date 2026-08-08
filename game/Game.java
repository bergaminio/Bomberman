package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bomb.Bomb;
import bomb.Explosion;
import map.GameMap;
import player.Player;

// Haelt den Spielzustand und beantwortet Fragen darueber. Veraendert wird
// er von aussen durch den GameService, nicht hier drin.
public class Game {
    private final GameMap map;
    private final List<Player> players;
    private final List<Bomb> bombs;
    private final List<Explosion> explosions;

    private GameStatus status;
    private int round;

    public Game(GameMap map, List<Player> players) {
        this.map = map;
        this.players = new ArrayList<>(players);
        this.bombs = new ArrayList<>();
        this.explosions = new ArrayList<>();
        this.status = GameStatus.WAITING;
        this.round = 0;
    }

    public GameMap getMap() {
        return map;
    }

    // Die Spielerliste steht beim Start fest, darum nur lesbar.
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    // Bomben und Explosionen kommen und gehen staendig. Der BombService
    // arbeitet direkt auf diesen Listen.
    public List<Bomb> getBombs() {
        return bombs;
    }

    public List<Explosion> getExplosions() {
        return explosions;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getRound() {
        return round;
    }

    public void nextRound() {
        setRound(round + 1);
    }

    // Wird beim Wiederherstellen eines Zustands gebraucht, etwa wenn der
    // Client den Stand vom Server empfaengt.
    public void setRound(int round) {
        this.round = round;
    }

    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Player player : players) {
            if (player.isAlive()) {
                alive.add(player);
            }
        }
        return alive;
    }

    // null heisst: noch nicht entschieden oder unentschieden.
    public Player getWinner() {
        if (status != GameStatus.FINISHED) {
            return null;
        }

        List<Player> alive = getAlivePlayers();
        return alive.size() == 1 ? alive.get(0) : null;
    }
}
