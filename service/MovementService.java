package service;

import common.Direction;
import common.Position;
import map.GameMap;
import player.Player;

public class MovementService {

    // Liefert true, wenn der Zug geklappt hat. Bei false steht der Spieler
    // noch am selben Ort, der Aufrufer kann das melden.
    public boolean tryMove(Player player, Direction direction, GameMap map) {
        if (!player.isAlive()) {
            return false;
        }

        Position target = player.getPosition().move(direction);

        // Reihenfolge ist wichtig: erst Grenze pruefen, dann das Tile holen.
        // Umgekehrt wuerde getTile() bei einem Feld ausserhalb abstuerzen.
        if (!map.isInsideMap(target)) {
            return false;
        }

        if (!map.getTile(target).isWalkable()) {
            return false;
        }

        player.setPosition(target);
        return true;
    }
}
