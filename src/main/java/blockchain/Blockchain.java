package blockchain;

import blockchain.genesis.GenCreator;
import utils.DiskUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static java.lang.String.format;

import proto.Types.*;
import utils.statistics.BCStat;
import utils.statistics.Statistics;

public class Blockchain {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Blockchain.class);
    private final int creatorID;
    private int swapSize = 0;
    private Path swapPath;
    private int maxCacheSize = 100;
    private boolean swapAble = false;
    private final ConcurrentHashMap<Integer, Block> blocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, BCStat> bsts = new ConcurrentHashMap<>();

    private Queue<Future> finishedTasks = new LinkedList<>();
    private Queue<Integer> tasksIdx = new LinkedList<>();

//    int size = 0;

    Blockchain(int creatorID, int maxCacheSize, GenCreator gcreator, Path swapPath) {
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

    // We assume now that all the validated blocks are in memory
    public boolean validateCurrentLeader(int leader, int f) {
        int last =  lastIndex();
        for (int i = last ; i > Math.max(0, last - f) ; i--) {
            if (getBlock(i).getHeader().getBid().getPid() == leader) return false;
        }
        return true;
    }

    public void setBlocks(List<Block> Nblocks, int start)  {
        for (int i = start ; i < start + Nblocks.size() ; i++) {
            if (i > lastIndex()) {
                addBlock(Nblocks.get(i - start));
            } else {
                setBlock(i, Nblocks.get((i - start)));
            }

        }
    }

    public void setBlock(int index, Block b) {
        if (Statistics.isActive()) {
            BCStat st = new BCStat();
            st.bst = b.getBst();
            st.hst = b.getHeader().getHst();
            st.txCount = b.getDataCount();
            st.pid = b.getId().getPid();
            bsts.putIfAbsent(b.getHeader().getHeight(), st);
        }

        if (blocks.keySet().contains(index)) {
            blocks.replace(index, b);
            return;
        }
        try {
            DiskUtils.deleteBlockFile(index, swapPath);
            DiskUtils.cutBlock(b, swapPath);
        } catch (IOException e) {
            logger.error(e);
        }


    }

    public boolean validateBlockHash(Block b) {
        if (b.getHeader().getHeight() == 0) return true;
        int index = b.getHeader().getHeight() - 1;
        if (lastIndex() < index) return false;
        Block prev = getBlock(index);
        return Utils.validateBlockHash(prev, b);
    }

    public boolean isValid() {
        for (int i = 1 ; i < lastIndex() + 1 ; i++) {
            if (!validateBlockHash(getBlock(i - 1))) {
                logger.info(String.format("Invalid Blockchain!! [%d -> %d]", i-1, i));
                return false;
            }
        }
        return true;
    }

    public void addBlock(Block b) {
        blocks.putIfAbsent(b.getHeader().getHeight(), b);
    }

    public BCStat getBlockSts(int index) {
        BCStat st = bsts.get(index);
        bsts.remove(index);
        return st;
    }
    public Block getBlock(int index) {
        if (blocks.isEmpty()) return null;
        if (index > lastIndex()) return null;
        if (blocks.keySet().contains(index)) {
            return blocks.get(index);
        }
        try {
            return DiskUtils.getBlockFromFile(index, swapPath);
        } catch (IOException e) {
            logger.error(format("Unable to retrieve block from disk [index=%d ; height=%d]", index, lastIndex()), e);
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

    public int lastIndex() {
        return Collections.max(blocks.keySet(), Comparator.comparingInt(integer -> integer));
//        int li = 0;
//        for (Map.Entry<Integer, Block> e: blocks.entrySet()) {
//            li = Math.max(li, e.getKey());
//        }
//        return  li;
//        return max(blocks.keys());
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
        int currBlockIndex = swapSize;
        try {
            DiskUtils.cutBlock(blocks.get(currBlockIndex), swapPath);
        } catch (IOException e) {
            logger.error(e);
        }
        swapSize++;
        blocks.remove(currBlockIndex);
    }

    public void writeNextToDiskAsync() {
        if (!swapAble) return;
        if (blocks.size() < maxCacheSize) return;
        int currBlockIndex = swapSize;
        tasksIdx.add(currBlockIndex);
        finishedTasks.add(DiskUtils.cutBlockAsync(blocks.get(currBlockIndex), swapPath));
        swapSize++;
        assert finishedTasks.peek() != null;
        boolean task_done = finishedTasks.peek().isDone();
        while (task_done) {
            blocks.remove(tasksIdx.remove());
            finishedTasks.remove();
            if (finishedTasks.isEmpty()) break;
            task_done = finishedTasks.peek().isDone();
        }
    }

//    void gcPhase() {
//        if (blocks.size() > 10 * maxCacheSize) {
//            long start = System.currentTimeMillis();
//            while (finishedTasks.size() > 0) {
//                // TODO: Inspect this code...
//                finishedTasks.poll().get();
//
//            }
//            logger.info(format("Running synchronized writes to block (in order to prevent memory leaks." +
//                    " Took about [%d] ms", System.currentTimeMillis() - start));
//            System.out.println(format("Running synchronized writes to block (in order to prevent memory leaks." +
//                    " Took about [%d] ms", System.currentTimeMillis() - start));
//        }
//    }

    public boolean contains(int height) {
        return height <= lastIndex();
    }

}
