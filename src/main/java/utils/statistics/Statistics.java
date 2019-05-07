package utils.statistics;

import blockchain.data.BCS;
import config.Config;
import das.ms.BFD;
import org.h2.mvstore.ConcurrentArrayList;
import proto.Types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

public class Statistics {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Statistics.class);
    static ExecutorService worker = Executors.newSingleThreadExecutor();
    static private long start;
    static private long stop;

    static private int h1 = 0;
    static private int h2 = 0;

    static private AtomicInteger all = new AtomicInteger(0);
    static private AtomicInteger pos = new AtomicInteger(0);
    static private AtomicInteger opt = new AtomicInteger(0);
    static private AtomicInteger neg = new AtomicInteger(0);
    static private AtomicInteger tmo = new AtomicInteger(0);
    static private AtomicInteger actTmo = new AtomicInteger(0);
    static private AtomicInteger maxTmo = new AtomicInteger(0);
    static private AtomicInteger syncs = new AtomicInteger(0);
    static private AtomicLong negTime = new AtomicLong(0);
//    static private HashMap<Integer[], Long> dlt = new HashMap<>();


    static private AtomicBoolean active = new AtomicBoolean(false);

    static int txCount = 0;
    static int nob = 0;
    static int neb = 0;
    static int txSize = 0;
    static int txInBlock = 0;

    static double acBP2T = 0;
    static double acBP2D = 0;
    static double acBP2DL = 0;

    static double acHP2T = 0;
    static double acHP2D = 0;
    static double acHT2D = 0;
    static double acHP2DL = 0;
    static double acHD2DL = 0;

    static public void updateAll() {
        if (!active.get()) return;
        all.incrementAndGet();
    }

    static public void updateOpt() {
        if (!active.get()) return;
        opt.incrementAndGet();
    }

    static public void updatePos() {
        if (!active.get()) return;
        pos.incrementAndGet();
    }

    static public void updateNeg() {
        if (!active.get()) return;
        neg.incrementAndGet();
    }

    static public void updateSyncs() {
        if (!active.get()) return;
        syncs.incrementAndGet();
    }

    static public void updateNegTime(long time) {
        if (!active.get()) return;
        negTime.addAndGet(time);
    }

    static public void activate() {
        if (active.get()) return;
        logger.info("Start statistics");
        active.set(true);
        h1 = BCS.height();
        start = System.currentTimeMillis();
        worker.submit(() -> {
            try {
                collectReasults();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        });
    }

    static public void deactivate() {
        if (!active.get()) return;
        logger.info("stop statistics");
        active.set(false);
        h2 = BCS.height();
        stop = System.currentTimeMillis();
        worker.shutdownNow();
    }

    static public void updateTmo(int newTmo) {
        if (!active.get()) return;
        tmo.addAndGet(newTmo);
    }

    static public void updateActTmo(int newActTmo) {
        if (!active.get()) return;
        tmo.addAndGet(newActTmo);
    }

    static public void updateMaxTmo(int newMaxTmo) {
        if (!active.get()) return;
        maxTmo.set(max(maxTmo.get(), newMaxTmo));
    }

    public static int getNeg() {
        return neg.get();
    }

    public static int getH1() {
        return h1;
    }

    public static int getAll() {
        return all.get();
    }

    public static int getH2() {
        return h2;
    }

    public static int getOpt() {
        return opt.get();
    }

    public static int getPos() {
        return pos.get();
    }

    public static long getActTmo() {
        return actTmo.get();
    }

    public static long getStart() {
        return start;
    }

    public static long getStop() {
        return stop;
    }

    public static long getTmo() {
        return tmo.get();
    }

    public static long getMaxTmo() {
        return maxTmo.get();
    }

    public static int getSyncs() {
        return syncs.get();
    }

    public static long getNegTime() {
        return negTime.get();
    }

    public static int getTxCount() {
        return txCount;
    }

    public static double getAcBP2D() {
        return acBP2D;
    }

    public static double getAcBP2T() {
        return acBP2T;
    }

    public static double getAcHP2D() {
        return acHP2D;
    }

    public static double getAcHP2T() {
        return acHP2T;
    }

    public static double getAcHT2D() {
        return acHT2D;
    }

    public static int getNeb() {
        return neb;
    }

    public static int getNob() {
        return nob;
    }

    public static int getTxInBlock() {
        return txInBlock;
    }

//    public static int getTxSize() {
//        return txSize;
//    }

    public static double getAcBP2DL() {
        return acBP2DL;
    }

    public static double getAcHD2DL() {
        return acHD2DL;
    }

    public static double getAcHP2DL() {
        return acHP2DL;
    }

    public static boolean isActive() {
        return active.get();
    }

    static void collectForBlock(BCStat b, int h, int worker) {
        long curr = System.currentTimeMillis();
        nob++;
        if (b.txCount == 0) {
            neb++;
        }
        txCount += b.txCount;
//        for (Types.Transaction t : b.getDataList()) {
//            txSize += t.getData().size();
//        }
        acBP2T += b.hst.getTentativeTime() - b.bst
                .getProposeTime();
        acBP2D += b.hst.getDefiniteTime() - b.bst
                .getProposeTime();

        acBP2DL += curr - b.bst.getProposeTime();

        acHP2T += b.hst.getTentativeTime() - b.hst
                .getProposeTime();
        acHP2D += b.hst.getDefiniteTime() - b.hst
                .getProposeTime();
        acHT2D += b.hst.getDefiniteTime() - b.hst
                .getTentativeTime();
        acHP2DL += curr - b.hst.getProposeTime();
        acHD2DL += curr - b.hst.getDefiniteTime();
//        dlt.put(new Integer[]{h, worker}, curr);
    }

    static void collectReasults() throws InterruptedException {
        int workers = Config.getC();
        int h = getH1();
        while (active.get()) {
            for (int i = 0 ; i < workers ; i++) {
                BCStat b = BCS.bGetBCStat(i, h);
                if (b == null) continue;
                collectForBlock(b, h, i);
            }
            h++;
        }
    }

//    static public long getDltByHeight(int height, int w) {
//        Integer[] key = new Integer[]{height, w};
//        if (!dlt.containsKey(key)) return -1;
//        return dlt.get(new Integer[]{height, w});
//
//    }

}
