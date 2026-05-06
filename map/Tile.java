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
}