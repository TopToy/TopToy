package blockchain;

import crypto.DigestMethod;

import proto.Types.*;
import utils.DiskUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class BaseBlockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BaseBlockchain.class);
    private final List<Block> blocks = new ArrayList<>();
    private final int creatorID;
    private int swapSize = 0;
    private Path swapPath;

    public BaseBlockchain(int creatorID, int channel) {
        this.creatorID = creatorID;
        createGenesis(channel);
    }

    public BaseBlockchain(int creatorID) {
        this.creatorID = creatorID;
    }

    public BaseBlockchain(BaseBlockchain orig, int start, int end) throws IOException {
        this.creatorID = orig.creatorID;
        this.blocks.addAll(orig.getBlocks(start, end));
    }

    public abstract BaseBlock createNewBLock();

    abstract void createGenesis(int channel);

    // Here we assume that the last f blocks are always in memory
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
    // Here we assume that the last f blocks are always in memory
    public boolean validateBlockCreator(Block b, int f) {
        if (blocks.size() >= f && blocks.subList(blocks.size() - f, blocks.size()).
        stream().
        map(bl -> bl.getHeader().getM().getSender()).
        collect(Collectors.toList()).
        contains(b.getHeader().getM().getSender())) {
            logger.debug(format("[#%d] invalid block", creatorID));
            return false;
        }
        return true;
    }

    public void setBlocks(List<Block> Nblocks, int start) throws IOException {
        for (int i = start ; i < start + Nblocks.size() ; i++) {
            if (i > getHeight() + 1) {
                synchronized (blocks) {
                    blocks.addAll(Nblocks.subList(i, start + Nblocks.size()));
                    return;
                }
            } else {
                setBlock(i, Nblocks.get(i - start));
            }

        }
    }

    public void setBlock(int index, Block b) throws IOException {
        synchronized (blocks) {
            if (index < swapSize) {
                DiskUtils.deleteBlockFile(index, swapPath);
                DiskUtils.cutBlock(b, swapPath);
                return;
            }
            if (index >= blocks.size()) {
                blocks.add(b);
                return;
            }
            blocks.set(index, b);
        }
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
        synchronized (blocks) {
            blocks.add(b);
        }
    }

    public Block getBlock(int index) {
        // performance alert!!
        synchronized (blocks) {
            if (index < swapSize) {
                try {
                    return DiskUtils.getBlockFromFile(index, swapPath);
                } catch (IOException e) {
                    logger.error("Unable to retrieve block from disk", e);
                    return null;
                }
            }
            return blocks.get(index);
        }
    }

    public List<Block> getBlocks(int start, int end) throws IOException {
        // performance alert!!
        synchronized (blocks) {
            if (start < swapSize) {
                List<Block> ret = new ArrayList<>();
                for (int i = start; i < swapSize || i < end; i++) {
                    ret.add(DiskUtils.getBlockFromFile(i, swapPath));
                }
                if (swapSize < end) {
                    ret.addAll(new ArrayList<>(blocks.subList(0, end)));
                }
                return ret;
            } else {
                return new ArrayList<>(blocks.subList(start, end));
            }
        }
    }

//    public List<Block> getBlocksCopy(int start, int end) {
//        synchronized (blocks) {
//            return new ArrayList<Block>(blocks.subList(start, end));
//        }
//    }

    public int getHeight() {
        synchronized (blocks) {
            return blocks.size() + swapSize - 1;
        }
    }

    public void removeBlock(int index) throws IOException {
        synchronized (blocks) {
            if (index < swapSize) {
                DiskUtils.deleteBlockFile(index, swapPath);
                swapSize--;
                return;
            }
            blocks.remove(index - swapSize);
        }
    }

    public void writeNextToDisk() throws IOException {
        synchronized (blocks) {
            DiskUtils.cutBlock(blocks.get(0), swapPath);
            blocks.remove(0);
            swapSize++;
        }
    }

}
