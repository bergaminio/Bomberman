package map;

import common.Position;

public class Tile {
    private Position position;
    private TileType type;
    private Block block;


    public Tile(Position position) {   
        this.position = position;
        this.type = TileType.GROUND;
        this.block = null;
    }
    public Tile(Position position, Block block) {   
        this.position = position;
        this.type = TileType.BLOCK;
        this.block = block; 
    }
    public Position getPosition() {
        return position;
    }

    public TileType getType() {
        return type;
    }
    public Block getBlock(){
        return block;
    }

    public boolean isWalkable() {
        return type == TileType.GROUND;
    }

    public void setBlock(Block block) {
        this.block = block;
        this.type = TileType.BLOCK;
    }

    // Gibt zurueck, ob wirklich etwas zerstoert wurde. Das braucht spaeter
    // die Explosion, um zu wissen, wo sie stoppen muss.
    public boolean destroyBlock() {
        if (block == null || !block.isDestroyable()) {
            return false;
        }

        this.block = null;
        this.type = TileType.GROUND;
        return true;
    }
}