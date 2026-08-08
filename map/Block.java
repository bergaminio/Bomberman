package map;

public class Block {
    private BlockType type;

    public Block(BlockType type) {
        this.type = type;
    }
    public BlockType getType() {
        return type;
    }
    public boolean isDestroyable() {
        return type == BlockType.DESTROYABLE;
    }
}