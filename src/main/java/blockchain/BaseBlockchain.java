package blockchain;

import crypto.DigestMethod;

import proto.Types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class BaseBlockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BaseBlockchain.class);
    private final List<Block> blocks = new ArrayList<>();
    private final int creatorID;

    public BaseBlockchain(int creatorID, int channel) {
        this.creatorID = creatorID;
        createGenesis(channel);
    }

    public BaseBlockchain(BaseBlockchain orig, int start, int end) {
        this.creatorID = orig.creatorID;
        this.blocks.addAll(orig.getBlocks(start, end));
    }

    public abstract BaseBlock createNewBLock();

    abstract void createGenesis(int channel);

    public boolean validateCurrentLeader(int leader, int f) {
        if (blocks.size() >= f && blocks.subList(blocks.size() - f, blocks.size()).
                stream().
                map(bl -> bl.getHeader().getM().getSender()).
                collect(Collectors.toList()).
                contains(leader)) {
            logger.debug(format("[#%d] leader is in the last f proposers", creatorID));
            return false;
        }
        return true;
    }
    public boolean validateBlockCreator(Block b, int f) {
        if (blocks.size() >= f && blocks.subList(blocks.size() - f, blocks.size()).
        stream().
        map(bl -> bl.getHeader().getM().getSender()).
        collect(Collectors.toList()).
        contains(b.getHeader().getM().getSender())) {
            logger.debug(format("[#%d] invalid BaseBlock", creatorID));
            return false;
        }
        return true;
    }

//    public abstract boolean validateBlockData(Block b);

    public void setBlocks(List<Block> Nblocks, int start) {
        for (int i = start ; i < start + Nblocks.size() ; i++) {
            if (blocks.size() <= i) {
                blocks.add(Nblocks.get(i - start));
            } else {
                blocks.set(i, Nblocks.get(i - start));
            }

        }
    }

    public void setBlock(int index, Block b) {
        synchronized (blocks) {
            blocks.set(index, b);
        }
    }
    public boolean validateBlockHash(Block b) {
        byte[] d = DigestMethod.hash(blocks.get(b.getHeader().getHeight() - 1).getHeader().toByteArray());
        return DigestMethod.validate(b.getHeader().getPrev().toByteArray(),
                Objects.requireNonNull(d));
    }

    public void addBlock(Block b) {
        synchronized (blocks) {
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

    public List<Block> getBlocksCopy(int start, int end) {
        synchronized (blocks) {
            return new ArrayList<Block>(blocks.subList(start, end));
        }
    }

    public int getHeight() {
        synchronized (blocks) {
            return blocks.size() - 1;
        }
    }

    public void removeBlock(int index) {

        synchronized (blocks) {
            blocks.remove(index);
        }
    }

}
