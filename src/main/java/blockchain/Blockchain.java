package blockchain;

import blockchain.genesis.GenCreator;
import utils.DiskUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static java.lang.String.format;
import static java.util.Collections.max;
import proto.Types.*;

public class Blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Blockchain.class);
    private final int creatorID;
    private int swapSize = 0;
    private Path swapPath;
    private int maxCacheSize = 100;
    private boolean swapAble = false;
    private final ConcurrentHashMap<Integer, Block> blocks = new ConcurrentHashMap<>();
    private Queue<Future> finishedTasks = new LinkedList<>();

//    int size = 0;

    public Blockchain(int creatorID,  int maxCacheSize, GenCreator gcreator, Path swapPath) {
        this.creatorID = creatorID;
        if (maxCacheSize > 0) {
            this.maxCacheSize = maxCacheSize;
        }
        this.swapPath = swapPath;
        DiskUtils.createStorageDir(swapPath);
        this.swapAble = true;
        addBlock(gcreator.createGenesisBlock());
    }

    public Blockchain(int creatorID) {
        this.creatorID = creatorID;
    }

    public Blockchain(Blockchain orig, int start, int end) {
        this.creatorID = orig.creatorID;
        for (int i = start ; i < end ; i++) {
            addBlock(orig.getBlock(i));
        }
    }

//    public abstract BaseBlock createNewBLock();
//
//    abstract void createGenesis(int channel);

    // We assume now that all the validated blocks are in memory
    public boolean validateCurrentLeader(int leader, int f) {
        int last =  getHeight();
        for (int i = last ; i > Math.max(0, last - f) ; i--) {
            if (getBlock(i).getHeader().getM().getSender() == leader) return false;
        }
        return true;
    }

    public void setBlocks(List<Block> Nblocks, int start) throws IOException {
        for (int i = start ; i < start + Nblocks.size() ; i++) {
            if (i > getHeight()) {
                addBlock(Nblocks.get(i - start));
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
        if (b.getHeader().getHeight() == 0) return true;
        int index = b.getHeader().getHeight() - 1;
        if (getHeight() < index) return false;
        Block prev = getBlock(index);
        return Utils.validateBlockHash(prev, b);
    }

    public boolean isValid() {
        for (int i = 1 ; i < getHeight() + 1 ; i++) {
            if (!validateBlockHash(getBlock(i - 1))) {
                System.out.println(String.format("Invalid Blockchain!! [%d -> %d]", i-1, i));
                return false;
            }
        }
        return true;
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

    public List<Block> getBlocks(int start, int end) {
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
//        logger.debug("BD(-1)");
        if (!swapAble) return;
//        logger.debug(format("BD(0), [%d ; %d]", blocks.size(), maxCacheSize));
        if (blocks.size() < maxCacheSize) return;
//        logger.debug("BD(1)");
        int currBlockIndex = swapSize;
        finishedTasks.add(DiskUtils.cutBlockAsync(blocks.get(currBlockIndex), swapPath));
        swapSize++;
        assert finishedTasks.peek() != null;
        boolean task_done = finishedTasks.peek().isDone();
//        logger.debug("BD(2)");
        while (task_done) {
//            logger.debug("BD(3)");
            blocks.remove(currBlockIndex);
            finishedTasks.remove();
            if (finishedTasks.isEmpty()) break;
            task_done = finishedTasks.peek().isDone();
        }
    }

    public boolean contains(int height) {
        return height <= getHeight();
    }

}