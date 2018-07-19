package blockchain;

import crypto.DigestMethod;
import proto.Block;
import proto.Crypto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockchain.class);
    private final List<Block> blocks = new ArrayList<>();
    private final int creatorID;

    public blockchain(int creatorID, String digestMethodName) {
        this.creatorID = creatorID;
        createGenesis();
    }
    abstract block createNewBLock();

    abstract void createGenesis();

    abstract boolean validateBlockData(Block b);

    boolean validateBlockHash(Block b) {
//        logger.info(String.format("[height=%d, sender=%d] [prev=%s]", b.getHeader().getHeight(),
//                b.getHeader().getCreatorID(), Arrays.toString(b.getHeader().getPrev().getDigest().toByteArray())));
//        logger.info(String.format("[height=%d, sender=%d] [prev2=%s]", b.getHeader().getHeight(),
//                b.getHeader().getCreatorID(), Arrays.toString(Objects.requireNonNull(
//                        DigestMethod.hash(blocks.get(b.getHeader().getHeight() - 1).getHeader())).getDigest().toByteArray())));
        Crypto.Digest d = DigestMethod.hash(blocks.get(b.getHeader().getHeight() - 1).getHeader());
        return DigestMethod.validate(b.getHeader().getPrev(),
                Objects.requireNonNull(d)); // TODO: Note that currentky we hash the whole block
    }

    public void addBlock(Block b) {
        synchronized (blocks) {
            logger.info(String.format("[#%d] adds a new block of [height=%d]", creatorID, b.getHeader().getHeight()));
            blocks.add(b);
        }
    }

    public Block getBlock(int index) {
        synchronized (blocks) {
            return blocks.get(index);
        }
    }

    public List<Block> getBlocks(int start, int end) {
        synchronized (blocks) {
            return blocks.subList(start, end);
        }
    }

    public int getHeight() {
        synchronized (blocks) {
            return blocks.size() - 1;
        }
    }

    public int getCreatorID() {
        return creatorID;
    }

}
