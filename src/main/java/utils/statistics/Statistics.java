package utils.statistics;

import blockchain.data.BCS;
import das.ms.BFD;
import org.h2.mvstore.ConcurrentArrayList;
import proto.Types;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

public class Statistics {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Statistics.class);

    // Note that due to late nodes this list may grow infinitely. Currently we assume that this is an uncommon case.
    static ConcurrentHashMap<Types.BlockID, BlockHeaderStatistics> headerStats;
    static ConcurrentHashMap<Types.BlockID, BlockStatistics> blockStats;

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



    static private boolean active = false;
    static private int txCount = 0;
    static private long blocksP2T = 0;
    static private long blocksP2D = 0;
    static private long blocksP2DL = 0;

    static private long headersP2T = 0;
    static private long headersT2D = 0;
    static private long headersP2D = 0;
    static private long headersD2DL = 0;
    static private long headersP2DL = 0;
    static private int tentatives = 0;
    static private int definites = 0;
    static private long nob;

    static final private Object lock = new Object();
    static public void updateAll() {
        if (!active) return;
        all.incrementAndGet();
    }

    static public void updateOpt() {
        if (!active) return;
        opt.incrementAndGet();
    }

    static public void updatePos() {
        if (!active) return;
        pos.incrementAndGet();
    }

    static public void updateNeg() {
        if (!active) return;
        neg.incrementAndGet();
    }

    static public void updateSyncs() {
        if (!active) return;
        syncs.incrementAndGet();
    }

    static public void updateNegTime(long time) {
        if (!active) return;
        negTime.addAndGet(time);
    }

    static public void updateTentative(Types.BlockID bid) {
        if (!active) return;
        if (!blockStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.getHeaderStatistics().updateTentative();
            return b;
        });
    }

    static public void updateDefinite(Types.BlockID bid) {
        if (!active) return;
        if (!blockStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.getHeaderStatistics().updateDefinite();
            return b;
        });
    }

    static public void activate() {
        active = true;
        h1 = BCS.height();
        start = System.currentTimeMillis();
    }

    static public void deactivate() {
        active = false;
        h2 = BCS.height();
        stop = System.currentTimeMillis();
        registerStats();
        headerStats.values().removeIf(h -> true);
        blockStats.values().removeIf(b -> true);
    }

    static public void addHeaderStat(Types.BlockID bid) {
        if (!active) return;
        BlockHeaderStatistics st = new BlockHeaderStatistics();
        headerStats.putIfAbsent(bid, st);
    }

    static public void updateHeaderPT(Types.BlockID bid, long pt) {
        if (!active) return;
        if (!headerStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        headerStats.computeIfPresent(bid, (k, h) -> {
            h.setProposedTime(pt);
            return h;
        });
    }

    static public void updateHeaderSender(Types.BlockID bid, int sender) {
        if (!active) return;
        if (!headerStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        headerStats.computeIfPresent(bid, (k, h) -> {
            h.setSender(sender);
            return h;
        });
    }

    static public void updateHeaderWorker(Types.BlockID bid, int worker) {
        if (!active) return;
        if (!headerStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        headerStats.computeIfPresent(bid, (k, h) -> {
            h.setWorker(worker);
            return h;
        });
    }

    static public void updateTmo(int newTmo) {
        if (!active) return;
        tmo.addAndGet(newTmo);
    }

    static public void updateActTmo(int newActTmo) {
        if (!active) return;
        tmo.addAndGet(newActTmo);
    }

    static public void updateMaxTmo(int newMaxTmo) {
        if (!active) return;
        maxTmo.set(max(maxTmo.get(), newMaxTmo));
    }

    static public void updateHeaderHeight(Types.BlockID bid, int height) {
        if (!active) return;
        if (!headerStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        headerStats.computeIfPresent(bid, (k, h) -> {
            h.setHeight(height);
            return h;
        });
    }

    static public void addBlockStat(Types.BlockID bid) {
        if (!active) return;
        BlockStatistics st = new BlockStatistics().setBid(bid);
        blockStats.putIfAbsent(bid, st);
    }

    static public void updateBlockStatSize(Types.BlockID bid, int size) {
        if (!active) return;
        if (!blockStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.setDataSize(size);
            return b;
        });
    }

    static public void updateBlockStatPT(Types.BlockID bid, long pt) {
        if (!active) return;
        if (!blockStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.setProposedTime(pt);
            return b;
        });
    }

    static public void updateHeaderTT(Types.BlockID bid, long tt) {
        if (!blockStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.getHeaderStatistics().setTentativeTime(tt);
            return b;
        });
    }

    static public void updateHeaderDT(Types.BlockID bid, long dt) {
        if (!blockStats.containsKey(bid)) {
            logger.error("Invalid BID");
        }
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.getHeaderStatistics().setPermanentTime(dt);
            return b;
        });
    }

//    static public BlockHeaderStatistics getBlockHeaderStat(Types.BlockID bid) {
//        if (!active) return null;
//        return headerStats.get(bid);
//    }
//
//    static public BlockStatistics getBlockStat(Types.BlockID bid) {
//        if (!active) return null;
//        return blockStats.get(bid);
//    }

    static public void adjustHeaderStatAndBlockStat(Types.BlockID bid) {
        if (!active) return;
        blockStats.computeIfPresent(bid, (k, b) -> {
            b.setHeaderStatistics(headerStats.get(bid));
            headerStats.remove(bid);
            return b;
        });
    }

    static public void registerStats() {
        if (!active) return;
        synchronized (lock) {
            int h = BCS.height();
            long time = System.currentTimeMillis();
            for (BlockStatistics bs : blockStats.values()) {
                if (bs.getHeaderStatistics() != null && bs.getHeaderStatistics().getHeight() <= h) {
                    BlockHeaderStatistics bhs = bs.getHeaderStatistics();

                    if (bs.getDataSize() == 0) {
                        nob++;
                    }

                    txCount += bs.getDataSize();
                    blocksP2T += max(0, bhs.getTentativeTime() - bs.getProposedTime());
                    blocksP2D += max(0, bhs.getPermanentTime() - bs.getProposedTime());
                    blocksP2DL += max(0, time - bs.getProposedTime());

                    headersP2T += max(0, bhs.getTentativeTime() - bhs.getProposedTime());
                    headersP2D += max(0, bhs.getPermanentTime() - bhs.getProposedTime());
                    headersT2D += max(0, bhs.getPermanentTime() - bhs.getTentativeTime());
                    headersD2DL += max(0, time - bhs.getPermanentTime());
                    headersP2DL += max(0, time - bhs.getProposedTime());

                    if (bhs.getDefinite()) {
                        definites++;
                    }

                    if (bhs.getTentative()) {
                        tentatives++;
                    }

                }
            }

            blockStats.values().removeIf(bs -> bs.getHeaderStatistics() != null
                    && bs.getHeaderStatistics().getHeight() <= h);
        }

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

    public static int getTxCount() {
        return txCount;
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

    public static long getBlocksP2D() {
        return blocksP2D;
    }

    public static long getBlocksP2DL() {
        return blocksP2DL;
    }

    public static long getBlocksP2T() {
        return blocksP2T;
    }

    public static long getHeadersD2DL() {
        return headersD2DL;
    }

    public static long getHeadersP2D() {
        return headersP2D;
    }

    public static long getHeadersP2DL() {
        return headersP2DL;
    }

    public static long getHeadersP2T() {
        return headersP2T;
    }

    public static long getHeadersT2D() {
        return headersT2D;
    }

    public static long getNob() {
        return nob;
    }

    public static long getTmo() {
        return tmo.get();
    }

    public static long getMaxTmo() {
        return maxTmo.get();
    }

    public static int getTentatives() {
        return tentatives;
    }

    public static int getDeinites() {
        return definites;
    }

    public static int getSyncs() {
        return syncs.get();
    }

    public static long getNegTime() {
        return negTime.get();
    }


    //
//
//    private int txCount;
//    private int totalDec;
//    private int optimisticDec;
//    private int syncEvents;
//    private int pos;
//    private int neg;
//    private long headerTotalTD;
//    private long headersTotalPD;
//    private int totalT;
//    private int totalP;
//    private long totalNegDecTime;
//    private int avgTmo;
//    private int avgActTmo;
//    private int maxTmo;
//    private int suspected;
//
//    static private Statistics[] sworkers;
//    static private Queue<Integer[]>[] wTxs;
//    static private long start;
//    static private long stop;
//    static private int workers;
//    static private boolean active = false;
//    static private int nob[];
//    static private int neb = 0;
//    static private int h1 = 0;
//    static private int h2 = 0;
//
//    public Statistics() {
//        headerStats = new ConcurrentHashMap<>();
//        txCount = 0;
//        totalDec = 0;
//        optimisticDec = 0;
//        syncEvents = 0;
//        pos = 0;
//        neg = 0;
//        headerTotalTD = 0;
//        headersTotalPD = 0;
//        totalT = 0;
//        totalP = 0;
//        totalNegDecTime = 0;
//        avgTmo = 0;
//        avgActTmo = 0;
//        maxTmo = 0;
//        suspected = 0;
//    }
//
//    public Statistics(int workers) {
//        Statistics.start = 0;
//        Statistics.stop = 0;
//        Statistics.workers = workers;
//        Statistics.sworkers = new Statistics[workers];
//        Statistics.wTxs = new Queue[workers];
//        nob = new int[workers];
//        for (int i = 0 ; i < workers ; i++) {
//            Statistics.sworkers[i] = new Statistics();
//            Statistics.wTxs[i] = new LinkedList<>();
//        }
//    }

//    static public void updateHeaderProposed(Types.BlockHeader h, int worker) {
//
//        sworkers[worker].headerStats
//                .putIfAbsent(h, Types.HeaderStatistics.newBuilder().setProposed(System.currentTimeMillis()).build());
//    }
//
//    static public void updateHeaderTD(Types.BlockHeader h, int worker) {
//        if (!active) return;
//        sworkers[worker].headerStats
//                .computeIfPresent(h, (k, v1) -> v1.toBuilder().setTD(System.currentTimeMillis() - v1.getProposed()).build());
//
//    }
//
//    static public void updateHeaderPD(Types.BlockHeader h, int worker) {
//        if (!active) return;
//        sworkers[worker].headerStats
//                .computeIfPresent(h, (k, v1) -> v1.toBuilder().setPD(System.currentTimeMillis() - v1.getProposed()).build());
//    }
//
//    static public void updateHeaderStatus(Types.BlockHeader h, int worker) {
//        if (!active) return;
//        if (!sworkers[worker].headerStats.containsKey(h)) return;
//        sworkers[worker].headerTotalTD += sworkers[worker].headerStats.get(h).getTD();
//        sworkers[worker].headersTotalPD += sworkers[worker].headerStats.get(h).getPD();
//        sworkers[worker].headerStats.remove(h);
//    }
//    static public void activate() {
//        active = true;
//        h1 = BCS.height();
//        start = System.currentTimeMillis();
//    }
//
//    static public void deactivate() {
//        active = false;
//        h2 = BCS.height();
//        stop = System.currentTimeMillis();
//    }
//
//    static public void updateTxCount(int worker, int height, int count) {
//        if (!active) return;
//        wTxs[worker].add(new Integer[]{height, count});
//    }
//
//    static public void updateTotalDec(int worker) {
//        if (!active) return;
//        sworkers[worker].totalDec++;
//    }
//
//    static public void updateOptimisitcDec(int worker) {
//        if (!active) return;
//        sworkers[worker].optimisticDec++;
//    }
//
//    static public void updateSyncEvents(int worker) {
//        if (!active) return;
//        sworkers[worker].syncEvents++;
//    }
//
//    static public void updatePosDec(int worker) {
//        if (!active) return;
//        sworkers[worker].pos++;
//    }
//
//    static public void updateNegDec(int worker) {
//        if (!active) return;
//        sworkers[worker].neg++;
//    }
//
//    static public void updateT(int worker) {
//        if (!active) return;
//        sworkers[worker].totalT++;
//    }
//
//    static public void updateP(int worker) {
//        if (!active) return;
//        sworkers[worker].totalP++;
//    }
//
//    static public void updateNegTime(int worker, long time) {
//        if (!active) return;
//        sworkers[worker].totalNegDecTime += time;
//    }
//
//    static public void updateActTmo(int worker, int tmo) {
//        if (!active) return;
//        sworkers[worker].avgActTmo += tmo;
//
//    }
//
//    static public void updateMaxTmo(int worker, int tmo) {
//        if (!active) return;
//        sworkers[worker].maxTmo = max(sworkers[worker].maxTmo, tmo);
//
//    }
//
//    static public void updateAvgTmo(int worker, int tmo) {
//        if (!active) return;
//        sworkers[worker].avgTmo += tmo;
//    }
//
//    static public void updateSuspected(int worker) {
//        if (!active) return;
//        sworkers[worker].suspected += BFD.size(worker);
//    }
//
////    static public void updateNob(int w) {
////        if (!active) return;
////        Statistics.nob[w]++;
////    }
//
//    static public void updateNeb() {
//        if (!active) return;
//        Statistics.neb++;
//    }
//
//    static public long getStart() {
//        return start;
//    }
//
//    static public long getStop() {
//        return stop;
//    }
//
//
//    static public Statistics getStatistics() {
//        Statistics sts = new Statistics();
//        int maxT = 0;
//        for (int i = 0 ; i < workers ; i++) {
//            sts.optimisticDec += sworkers[i].optimisticDec;
//            sts.totalDec += sworkers[i].totalDec;
//            sts.pos += sworkers[i].pos;
//            sts.neg += sworkers[i].neg;
//            sts.syncEvents += sworkers[i].syncEvents;
//            sts.headerTotalTD += sworkers[i].headerTotalTD;
//            sts.headersTotalPD += sworkers[i].headersTotalPD;
//            sts.totalT += sworkers[i].totalT;
//            sts.totalP += sworkers[i].totalP;
//            sts.totalNegDecTime += sworkers[i].totalNegDecTime;
//            sts.avgTmo += sworkers[i].avgTmo;
//            sts.avgActTmo += sworkers[i].avgActTmo;
//            sts.suspected += BFD.size(i);
//            maxT = max(maxT, sworkers[i].maxTmo);
//        }
//        sts.suspected /= workers;
//        sts.maxTmo = maxT;
//        sts.txCount = getAllTxs();
//
//        return sts;
//    }
//
////    static int getAllTxs() {
////        int count = 0;
////        int minS = wTxs[0].size();
////
////        for (int i = 1 ; i < workers ; i++) {
////            minS = min(minS, wTxs[i].size());
////        }
//////        logger.info(format("Statistics [min=%d]", minS));
////        for (int i = 0 ; i < workers ; i++) {
////            for (int j = 0 ; j < minS ; j++) {
////                count += wTxs[i].remove();
////            }
////        }
////        return count;
////    }
//
//    static int getAllTxs() {
//        int count = 0;
//        for (int i = 0 ; i < workers ; i++) {
//            for (Integer[] data : wTxs[i]) {
//                if (data[0] >= h1 && data[0] <= h2) {
//                    count+= data[1];
//                }
//            }
//        }
//        return count;
//    }
//
//    public int getTxCount() {
//        return txCount;
//    }
//
//    public int getOptimisticDec() {
//        return optimisticDec;
//    }
//
//    public int getTotalDec() {
//        return totalDec;
//
//    }
//
//    public int getPosDec() {
//        return pos;
//    }
//
//    public int getNegDec() {
//        return neg;
//    }
//
//    public int getSyncEnvents() {
//        return syncEvents;
//    }
//
//    public long getHeadersTotalTD() {
//        return headerTotalTD;
//    }
//
//    public long getHeadersTtoalPD() {
//        return headersTotalPD;
//    }
//
//    public int getTotalT() {
//        return totalT;
//    }
//
//    public int getTotalP() {
//        return totalP;
//    }
//
//    public long getTotalNegTime() {
//        return totalNegDecTime;
//    }
//
//    public int getAvgTmo() {
//        return avgTmo;
//    }
//
//    public int getAvgActTmo() {
//        return avgActTmo;
//    }
//
//    public int getMaxTmo() {
//        return maxTmo;
//    }
//
//    public int getSuspected() {
//        return suspected;
//    }
//
//    public int getNob() {
////        int n = nob[0];
////        for (int i = 1 ; i < workers ; i++) {
////            n = min(n, nob[i]);
////        }
////        return n * workers;
//        return (h2 - h1) * workers;
//
//    }
//
////    public int getNeb() {
////        return neb;
////    }
}
