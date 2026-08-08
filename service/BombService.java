package service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import bomb.Bomb;
import bomb.Explosion;
import common.Direction;
import common.Position;
import map.GameMap;
import map.Tile;
import player.Player;

public class BombService {
    private final int fuseTicks;
    private final int explosionTicks;

    // Standardwerte passen zum rundenbasierten Spiel: drei Zuege Zeit zum
    // Fliehen, ein Zug Feuer.
    public BombService() {
        this(Bomb.DEFAULT_FUSE_TICKS, Explosion.DEFAULT_DURATION_TICKS);
    }

    // Im Fenster laeuft ein Tick nur rund 200 ms. Dort braucht es viel mehr
    // Ticks fuer dieselbe gefuehlte Zeit, darum sind beide Werte einstellbar.
    public BombService(int fuseTicks, int explosionTicks) {
        this.fuseTicks = fuseTicks;
        this.explosionTicks = explosionTicks;
    }

    // Liefert die gelegte Bombe oder null, wenn es nicht ging.
    public Bomb placeBomb(Player player, List<Bomb> bombs) {
        if (!player.canPlaceBomb()) {
            return null;
        }
        if (findBombAt(bombs, player.getPosition()) != null) {
            return null;
        }

        Bomb bomb = new Bomb(player, player.getPosition(), player.getBlastRadius(), fuseTicks);
        bombs.add(bomb);
        player.bombPlaced();
        return bomb;
    }

    // Ein Tick: erst alle Zuender runterzaehlen, dann alle faelligen Bomben
    // gemeinsam zuenden. Getrennt, damit zwei gleichzeitig faellige Bomben
    // auch wirklich gleichzeitig hochgehen.
    public List<Explosion> tickBombs(List<Bomb> bombs, GameMap map) {
        for (Bomb bomb : bombs) {
            bomb.tick();
        }

        return detonateReadyBombs(bombs, map);
    }

    public void tickExplosions(List<Explosion> explosions) {
        for (Explosion explosion : explosions) {
            explosion.tick();
        }

        explosions.removeIf(explosion -> explosion.isFinished());
    }

    public Bomb findBombAt(List<Bomb> bombs, Position position) {
        for (Bomb bomb : bombs) {
            if (bomb.getPosition().equals(position)) {
                return bomb;
            }
        }
        return null;
    }

    // Kettenreaktion mit Warteschlange statt Rekursion: eine Bombe im Feuer
    // wird hinten angehaengt und in einem der naechsten Durchlaeufe bearbeitet.
    private List<Explosion> detonateReadyBombs(List<Bomb> bombs, GameMap map) {
        Queue<Bomb> pending = new ArrayDeque<>();
        for (Bomb bomb : bombs) {
            if (bomb.isReadyToExplode()) {
                pending.add(bomb);
            }
        }

        List<Explosion> explosions = new ArrayList<>();
        Set<Bomb> alreadyDetonated = new HashSet<>();

        while (!pending.isEmpty()) {
            Bomb bomb = pending.remove();

            // Zwei Feuerstrahlen koennen dieselbe Bombe treffen. Ohne diese
            // Sperre wuerde sie zweimal explodieren und der Besitzer wuerde
            // seinen Bombenzaehler zweimal senken.
            if (!alreadyDetonated.add(bomb)) {
                continue;
            }

            List<Position> affected = calculateBlast(bomb, map);
            explosions.add(new Explosion(affected, explosionTicks));

            bombs.remove(bomb);
            bomb.getOwner().bombExploded();

            for (Position position : affected) {
                Bomb hit = findBombAt(bombs, position);
                if (hit != null) {
                    hit.detonateNow();
                    pending.add(hit);
                }
            }
        }

        return explosions;
    }

    private List<Position> calculateBlast(Bomb bomb, GameMap map) {
        List<Position> affected = new ArrayList<>();
        affected.add(bomb.getPosition());

        for (Direction direction : Direction.values()) {
            Position current = bomb.getPosition();

            for (int step = 0; step < bomb.getBlastRadius(); step++) {
                current = current.move(direction);

                if (!map.isInsideMap(current)) {
                    break;
                }

                Tile tile = map.getTile(current);

                // Unzerstoerbare Mauer: der Strahl endet davor, das Feld
                // selbst wird nicht getroffen.
                if (!tile.isWalkable() && !tile.getBlock().isDestroyable()) {
                    break;
                }

                affected.add(current);

                // Zerstoerbarer Block: wird zerstoert und schluckt den Strahl.
                // Genau dafuer gibt destroyBlock() einen boolean zurueck.
                if (tile.destroyBlock()) {
                    break;
                }
            }
        }

        return affected;
    }
}
