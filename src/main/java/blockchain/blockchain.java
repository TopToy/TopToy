package blockchain;

import proto.Block;

import java.util.ArrayList;
import java.util.List;

public abstract class blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockchain.class);
    private List<Block> blocks = new ArrayList<>();
    private int creatorID;

    public blockchain(int creatorID) {
        this.creatorID = creatorID;
        createGenesis();
    }
    abstract block createNewBLock();

    abstract void createGenesis();

    abstract boolean validateBlockData(Block b);

    boolean validateBlockHash(Block b) {
        return b.getPrevHash() == blocks.get(b.getHeight() - 1).hashCode();
    }

    public void addBlock(Block b) {
        logger.info(String.format("[#%d] adds a new block of [height=%d]", creatorID, b.getHeight()));
        blocks.add(b);
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
