package blockchain.data;

import blockchain.Blockchain;
import blockchain.Utils;
import proto.types.block.*;
import utils.statistics.BCStat;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static app.JToy.bftSMaRtSettings;
import static blockchain.Utils.createBlockchain;
import static com.google.common.primitives.Ints.min;
import static java.lang.Integer.max;

public class BCS {
    static private Blockchain[] bcs;
    private final static Object newBlockNotifier = new Object();
    private static int n;
    private static int f;
    private static int workers;

    public BCS(int id, int n, int f, int workers) {
        BCS.n = n;
        BCS.f = f;
        BCS.workers = workers;
        bcs = new Blockchain[workers];
        for (int i = 0 ; i < workers ; i++) {
            bcs[i] = createBlockchain(Utils.BCT.SGC, id, max(f + 2, 10), // TODO: check the cache size
                    Paths.get("blocks", String.valueOf(i)));
        }
    }

    public static boolean validateBlockHash(int w, Block b) {
        return bcs[w].validateBlockHash(b);
    }

    public static boolean validateCurrentLeader(int w, int currLeader, int currF) {
        return bcs[w].validateCurrentLeader(currLeader, currF);
    }

    public static void addBlock(int w, Block b) {
        bcs[w].addBlock(b);
    }

    public static void removeBlock(int w, int h) throws IOException {
        bcs[w].removeBlock(h);
    }

    public static void setBlocks(int w, List<Block> blocks, int start) {
        bcs[w].setBlocks(blocks, start);
    }

    public static void notifyOnNewDefiniteBlock() {
        synchronized (newBlockNotifier) {
            newBlockNotifier.notifyAll();
        }
    }

    public static Block nbGetBlock(int w, int h) {
        return bcs[w].getBlock(h);
    }

    public static void setBlock(int w, int h, Block b) {
        bcs[w].setBlock(h, b);
    }

    public static boolean contains(int w, int h) {
        return bcs[w].contains(h);
    }

    public static List<Block> getBlocks(int w, int low, int high) {
        return bcs[w].getBlocks(low, high);
    }

    public static BCStat bGetBCStat(int w, int h) throws InterruptedException {
        synchronized (newBlockNotifier) {
            while (height() < h) {
                newBlockNotifier.wait();
            }
            return bcs[w].getBlockSts(h);
        }
    }

    public static Block bGetBlock(int w, int h) throws InterruptedException {
        synchronized (newBlockNotifier) {
            while (height() < h) {
                newBlockNotifier.wait();
            }
            return bcs[w].getBlock(h);
        }
    }

    public static int lastIndex(int w) {
        return bcs[w].lastIndex();
    }


    public static int height(int w) {
        if (bftSMaRtSettings) return lastIndex(w);
        return max(0, lastIndex(w) - (f + 2));
    }

    public static int height() {
        int mh = height(0);
        for (int i = 0 ; i < workers ; i++) {
            mh = min(mh, height(i));
        }
        return mh;
    }

    public static void writeNextToDiskAsync(int w) {
        bcs[w].writeNextToDiskAsync();
    }

    public static boolean isValid(int w) {
        return bcs[w].isValid();
    }

    public static boolean isValid() {
        for (int i = 0 ; i < workers ; i++) {
            if (!isValid(i)) return false;
        }
        return true;
    }
}
