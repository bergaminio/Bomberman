package player;

import common.Position;

public class Player {
    private static final int DEFAULT_BOMB_CAPACITY = 1;
    private static final int DEFAULT_BLAST_RADIUS = 1;

    private String name;
    private Position position;
    private PlayerStatus status;

    private int bombCapacity;
    private int blastRadius;
    private int activeBombs;

    public Player(String name, Position position) {
        this.name = name;
        this.position = position;
        this.status = PlayerStatus.ALIVE;
        this.bombCapacity = DEFAULT_BOMB_CAPACITY;
        this.blastRadius = DEFAULT_BLAST_RADIUS;
        this.activeBombs = 0;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void die() {
        this.status = PlayerStatus.DEAD;
    }

    public boolean isAlive() {
        return status == PlayerStatus.ALIVE;
    }

    public int getBombCapacity() {
        return bombCapacity;
    }

    public int getBlastRadius() {
        return blastRadius;
    }

    public int getActiveBombs() {
        return activeBombs;
    }

    public boolean canPlaceBomb() {
        return isAlive() && activeBombs < bombCapacity;
    }

    // Wird beim Legen hochgezaehlt und beim Explodieren wieder runter.
    // So begrenzt sich der Spieler selbst, ohne dass jemand mitzaehlen muss.
    public void bombPlaced() {
        activeBombs++;
    }

    public void bombExploded() {
        if (activeBombs > 0) {
            activeBombs--;
        }
    }
}
