package player;

import common.Position;

public class Player {
    private String name;
    private Position position;
    private PlayerStatus status;

    public Player(String name, Position position) {
        this.name = name;
        this.position = position;
        this.status = PlayerStatus.ALIVE;
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
}
