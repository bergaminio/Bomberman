package bomb;

import common.Position;
import player.Player;

// Eine Bombe ist eine Entitaet, keine Wertklasse: zwei Bomben mit denselben
// Werten sind trotzdem zwei verschiedene Bomben. Darum bewusst kein equals().
public class Bomb {
    public static final int DEFAULT_FUSE_TICKS = 3;

    private final Player owner;
    private final Position position;
    private final int blastRadius;
    private int fuseTicks;

    public Bomb(Player owner, Position position) {
        this(owner, position, owner.getBlastRadius(), DEFAULT_FUSE_TICKS);
    }

    public Bomb(Player owner, Position position, int blastRadius, int fuseTicks) {
        this.owner = owner;
        this.position = position;
        this.blastRadius = blastRadius;
        this.fuseTicks = fuseTicks;
    }

    public Player getOwner() {
        return owner;
    }

    public Position getPosition() {
        return position;
    }

    public int getBlastRadius() {
        return blastRadius;
    }

    public int getFuseTicks() {
        return fuseTicks;
    }

    public void tick() {
        if (fuseTicks > 0) {
            fuseTicks--;
        }
    }

    public boolean isReadyToExplode() {
        return fuseTicks <= 0;
    }

    // Fuer die Kettenreaktion: eine Bombe im Feuer wartet nicht auf ihren Zuender.
    public void detonateNow() {
        this.fuseTicks = 0;
    }
}
