package utils;

import das.ms.BFD;
import proto.Types;

import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;

public class Statistics {
    // Note that due to late nodes this list may grow infinitely. Currently we assume that this is an uncommon case.
    private ConcurrentHashMap<Types.BlockHeader, Types.HeaderStatistics> headerStats;

    private int txCount;
    private int totalDec;
    private int optimisticDec;
    private int syncEvents;
    private int pos;
    private int neg;
    private long headerTotalTD;
    private long headersTotalPD;
    private int totalT;
    private int totalP;
    private long totalNegDecTime;
    private int avgTmo;
    private int avgActTmo;
    private int maxTmo;
    private int suspected;

    static private Statistics[] sworkers;
    static private long start;
    static private long stop;
    static private int workers;

    public Statistics() {
        headerStats = new ConcurrentHashMap<>();
        txCount = 0;
        totalDec = 0;
        optimisticDec = 0;
        syncEvents = 0;
        pos = 0;
        neg = 0;
        headerTotalTD = 0;
        headersTotalPD = 0;
        totalT = 0;
        totalP = 0;
        totalNegDecTime = 0;
        avgTmo = 0;
        avgActTmo = 0;
        maxTmo = 0;
        suspected = 0;
    }

    public Statistics(int workers) {
        Statistics.start = 0;
        Statistics.stop = 0;
        Statistics.workers = workers;
        Statistics.sworkers = new Statistics[workers];
        for (int i = 0 ; i < workers ; i++) {
            Statistics.sworkers[i] = new Statistics();
        }
    }

    static public void updateHeaderProposed(Types.BlockHeader h, int worker) {
        sworkers[worker].headerStats
                .putIfAbsent(h, Types.HeaderStatistics.newBuilder().setProposed(System.currentTimeMillis()).build());
    }

    static public void updateHeaderTD(Types.BlockHeader h, int worker) {
        sworkers[worker].headerStats
                .computeIfPresent(h, (k, v1) -> v1.toBuilder().setTD(System.currentTimeMillis() - v1.getProposed()).build());

    }

    static public void updateHeaderPD(Types.BlockHeader h, int worker) {
        sworkers[worker].headerStats
                .computeIfPresent(h, (k, v1) -> v1.toBuilder().setPD(System.currentTimeMillis() - v1.getProposed()).build());
    }

    static public void updateHeaderStatus(Types.BlockHeader h, int worker) {
        if (!sworkers[worker].headerStats.containsKey(h)) return;
        sworkers[worker].headerTotalTD += sworkers[worker].headerStats.get(h).getTD();
        sworkers[worker].headersTotalPD += sworkers[worker].headerStats.get(h).getPD();
        sworkers[worker].headerStats.remove(h);
    }

    static public void updateStart() {
        start = System.currentTimeMillis();
    }

    static public void updateStop() {
        stop = System.currentTimeMillis();
    }

    static public void updateTxCount(int worker, int count) {
        sworkers[worker].txCount += count;
    }

    static public void updateTotalDec(int worker) {
        sworkers[worker].totalDec++;
    }

    static public void updateOptimisitcDec(int worker) {
        sworkers[worker].optimisticDec++;
    }

    static public void updateSyncEvents(int worker) {
        sworkers[worker].syncEvents++;
    }

    static public void updatePosDec(int worker) {
        sworkers[worker].pos++;
    }

    static public void updateNegDec(int worker) {
        sworkers[worker].neg++;
    }

    static public void updateT(int worker) {
        sworkers[worker].totalT++;
    }

    static public void updateP(int worker) {
        sworkers[worker].totalP++;
    }

    static public void updateNegTime(int worker, long time) {
        sworkers[worker].totalNegDecTime += time;
    }

    static public void updateActTmo(int worker, int tmo) {
        sworkers[worker].avgActTmo += tmo;

    }

    static public void updateMaxTmo(int worker, int tmo) {
        sworkers[worker].maxTmo = max(sworkers[worker].maxTmo, tmo);

    }

    static public void updateAvgTmo(int worker, int tmo) {
        sworkers[worker].avgTmo += tmo;
    }

    static public void updateSuspected(int worker) {
        sworkers[worker].suspected += BFD.size(worker);
    }

    static public long getStart() {
        return start;
    }

    static public long getStop() {
        return stop;
    }

    static public Statistics getStatistics() {
        Statistics sts = new Statistics();
        int maxT = 0;
        for (int i = 0 ; i < workers ; i++) {
            sts.txCount += sworkers[i].txCount; //.getAndAdd(sworkers[i].txCount.get());
            sts.optimisticDec += sworkers[i].optimisticDec; //.getAndAdd(sworkers[i].optimisticDec.get());
            sts.totalDec += sworkers[i].totalDec; //.getAndAdd(sworkers[i].totalDec.get());
            sts.pos += sworkers[i].pos; //.getAndAdd(sworkers[i].pos.get());
            sts.neg += sworkers[i].neg; //.getAndAdd(sworkers[i].neg.get());
            sts.syncEvents += sworkers[i].syncEvents; //.getAndAdd(sworkers[i].syncEvents.get());
            sts.headerTotalTD += sworkers[i].headerTotalTD; //.getAndAdd(sworkers[i].headerTotalTD.get());
            sts.headersTotalPD += sworkers[i].headersTotalPD; //.getAndAdd(sworkers[i].headersTotalPD.get());
            sts.totalT += sworkers[i].totalT; //.getAndAdd(sworkers[i].totalT.get());
            sts.totalP += sworkers[i].totalP; //.getAndAdd(sworkers[i].totalP.get());
            sts.totalNegDecTime += sworkers[i].totalNegDecTime; //.getAndAdd(sworkers[i].totalNegDecTime.get());
            sts.avgTmo += sworkers[i].avgTmo;
            sts.avgActTmo += sworkers[i].avgActTmo;
            sts.suspected += BFD.size(i);
            maxT = max(maxT, sworkers[i].maxTmo);
        }
        sts.suspected /= workers;
        sts.maxTmo = maxT;
        return sts;
    }

    public int getTxCount() {
        return txCount;//.get();
    }

    public int getOptimisticDec() {
        return optimisticDec;
//        .get();
    }

    public int getTotalDec() {
        return totalDec;
        //.get();
    }

    public int getPosDec() {
        return pos;
//        .get();
    }

    public int getNegDec() {
        return neg;
//        .get();
    }

    public int getSyncEnvents() {
        return syncEvents;
//        .get();
    }

    public long getHeadersTotalTD() {
        return headerTotalTD;
//        .get();
    }

    public long getHeadersTtoalPD() {
        return headersTotalPD;
//        .get();
    }

    public int getTotalT() {
        return totalT;
//        .get();
    }

    public int getTotalP() {
        return totalP;
//        .get();
    }

    public long getTotalNegTime() {
        return totalNegDecTime;
//    .get();
    }

    public int getAvgTmo() {
        return avgTmo;
    }

    public int getAvgActTmo() {
        return avgActTmo;
    }

    public int getMaxTmo() {
        return maxTmo;
    }

    public int getSuspected() {
        return suspected;
    }
}
