package blockchain;

import proto.Block;

import java.util.ArrayList;
import java.util.List;

public abstract class blockchain {
    private List<Block> blocks = new ArrayList<>();
    private int creatorID;

    public blockchain(int creatorID) {
        this.creatorID = creatorID;
        createGenesis();
    }
    private boolean validateBlock(Block b) {
        return (b.getPrevHash() == blocks.get(b.getHeight() - 1).hashCode());
    }

    abstract abstractBlock createNewBLock();

    abstract void createGenesis();

    public void addBlock(abstractBlock b) {
        blocks.add(b.construct());
    }

    public Block getBlock(int index) {
        return blocks.get(index);
    }

    public List<Block> getBlocks(int start, int end) {
        return blocks.subList(start, end);
    }

    public int getHeight() {
        return blocks.size();
    }

    public int getCreatorID() {
        return creatorID;
    }
}
