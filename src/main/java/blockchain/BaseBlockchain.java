package blockchain;

import crypto.DigestMethod;
import proto.Types.*;
import utils.DiskUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.max;
import static java.util.Collections.min;

public abstract class BaseBlockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BaseBlockchain.class);
    private final int creatorID;
    private int swapSize = 0;
    private Path swapPath;
    private int maxCacheSize = 100;
    private boolean swapAble = false;
    private final ConcurrentHashMap<Integer, Block> blocks = new ConcurrentHashMap<>();
//    int size = 0;

    public BaseBlockchain(int creatorID, int channel, int maxCacheSize, Path swapPath) {
        this.creatorID = creatorID;
        if (maxCacheSize > 0) {
            this.maxCacheSize = maxCacheSize;
        }
        this.swapPath = swapPath;
        DiskUtils.createStorageDir(swapPath);
        this.swapAble = true;
        createGenesis(channel);
    }

    public BaseBlockchain(int creatorID) {
        this.creatorID = creatorID;
    }

    public BaseBlockchain(BaseBlockchain orig, int start, int end) {
        this.creatorID = orig.creatorID;
        for (int i = start ; i < end ; i++) {
            addBlock(orig.getBlock(i));
        }
    }

    public abstract BaseBlock createNewBLock();

    abstract void createGenesis(int channel);

    // We assume now that all the validated blocks are in memory
    public boolean validateCurrentLeader(int leader, int f) {
        int last =  getHeight();
        for (int i = last ; i > Math.max(0, last - f) ; i--) {
//            System.out.println("---- " + i);
            if (getBlock(i).getHeader().getM().getSender() == leader) return false;
        }
        return true;
    }

    public void setBlocks(List<Block> Nblocks, int start) throws IOException {
        for (int i = start ; i < start + Nblocks.size() ; i++) {
            if (i > getHeight() + 1) {
                addBlock(Nblocks.get(i));
            } else {
                setBlock(i, Nblocks.get((i - start)));
            }

        }
    }

    public void setBlock(int index, Block b) throws IOException {
        if (blocks.keySet().contains(index)) {
            blocks.replace(index, b);
            return;
        }
        DiskUtils.deleteBlockFile(index, swapPath);
        DiskUtils.cutBlock(b, swapPath);
    }

    public boolean validateBlockHash(Block b) {
        int index = b.getHeader().getHeight() - 1;
        Block prev = getBlock(index);
        return validateBlockHash(prev, b);
    }

    static public boolean validateBlockHash(Block prev, Block b) {
        byte[] d = DigestMethod.hash(prev.getHeader().toByteArray());
        return DigestMethod.validate(b.getHeader().getPrev().toByteArray(),
                Objects.requireNonNull(d));
    }

    public void addBlock(Block b) {
        blocks.putIfAbsent(b.getHeader().getHeight(), b);
    }

    public Block getBlock(int index) {
        if (blocks.keySet().contains(index)) {
            return blocks.get(index);
        }
        try {
            return DiskUtils.getBlockFromFile(index, swapPath);
        } catch (IOException e) {
            logger.error("Unable to retrieve block from disk", e);
            return null;
        }
    }

    public List<Block> getBlocks(int start, int end) throws IOException {
        List<Block> ret = new ArrayList<>();
        for (int i = start ; i < end ; i++) {
            ret.add(getBlock(i));
        }
        return ret;
    }

    public int getHeight() {
        return max(blocks.keySet());
    }


    // This should be used in its very specific usage only!!!
    public void removeBlock(int index) throws IOException {
        if (blocks.keySet().contains(index)) {
            blocks.remove(index);
            return;
        }
        DiskUtils.deleteBlockFile(index, swapPath);
    }

    public void writeNextToDisk() {
        if (!swapAble) return;
        if (blocks.size() < maxCacheSize) return;
        try {
            DiskUtils.cutBlock(blocks.get(swapSize), swapPath);
            blocks.remove(swapSize);
            swapSize++;
            } catch (IOException e) {
                logger.error("Unable to remove block", e);
            }

    }

}
