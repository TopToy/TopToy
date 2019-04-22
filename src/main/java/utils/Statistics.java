package utils;

import das.ms.BFD;
import proto.Types;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
    static private Queue<Integer>[] wTxs;
    static private long start;
    static private long stop;
    static private int workers;
    static private boolean active = false;

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
        Statistics.wTxs = new Queue[workers];
        for (int i = 0 ; i < workers ; i++) {
            Statistics.sworkers[i] = new Statistics();
            Statistics.wTxs[i] = new LinkedList<>();
        }
    }

    static public void updateHeaderProposed(Types.BlockHeader h, int worker) {

        sworkers[worker].headerStats
                .putIfAbsent(h, Types.HeaderStatistics.newBuilder().setProposed(System.currentTimeMillis()).build());
    }

    static public void updateHeaderTD(Types.BlockHeader h, int worker) {
        if (!active) return;
        sworkers[worker].headerStats
                .computeIfPresent(h, (k, v1) -> v1.toBuilder().setTD(System.currentTimeMillis() - v1.getProposed()).build());

    }

    static public void updateHeaderPD(Types.BlockHeader h, int worker) {
        if (!active) return;
        sworkers[worker].headerStats
                .computeIfPresent(h, (k, v1) -> v1.toBuilder().setPD(System.currentTimeMillis() - v1.getProposed()).build());
    }

    static public void updateHeaderStatus(Types.BlockHeader h, int worker) {
        if (!active) return;
        if (!sworkers[worker].headerStats.containsKey(h)) return;
        sworkers[worker].headerTotalTD += sworkers[worker].headerStats.get(h).getTD();
        sworkers[worker].headersTotalPD += sworkers[worker].headerStats.get(h).getPD();
        sworkers[worker].headerStats.remove(h);
    }
    static public void activate() {
        active = true;
        start = System.currentTimeMillis();
    }

    static public void deactivate() {
        active = false;
        stop = System.currentTimeMillis();
    }

//    static public void updateStart() {
//        if (!active) return;
//        start = System.currentTimeMillis();
//    }
//
//    static public void updateStop() {
//        if (!active) return;
//        stop = System.currentTimeMillis();
//    }

    static public void updateTxCount(int worker, int count) {
        if (!active) return;
//        sworkers[worker].txCount += count;
        wTxs[worker].add(count);
    }

    static public void updateTotalDec(int worker) {
        if (!active) return;
        sworkers[worker].totalDec++;
    }

    static public void updateOptimisitcDec(int worker) {
        if (!active) return;
        sworkers[worker].optimisticDec++;
    }

    static public void updateSyncEvents(int worker) {
        if (!active) return;
        sworkers[worker].syncEvents++;
    }

    static public void updatePosDec(int worker) {
        if (!active) return;
        sworkers[worker].pos++;
    }

    static public void updateNegDec(int worker) {
        if (!active) return;
        sworkers[worker].neg++;
    }

    static public void updateT(int worker) {
        if (!active) return;
        sworkers[worker].totalT++;
    }

    static public void updateP(int worker) {
        if (!active) return;
        sworkers[worker].totalP++;
    }

    static public void updateNegTime(int worker, long time) {
        if (!active) return;
        sworkers[worker].totalNegDecTime += time;
    }

    static public void updateActTmo(int worker, int tmo) {
        if (!active) return;
        sworkers[worker].avgActTmo += tmo;

    }

    static public void updateMaxTmo(int worker, int tmo) {
        if (!active) return;
        sworkers[worker].maxTmo = max(sworkers[worker].maxTmo, tmo);

    }

    static public void updateAvgTmo(int worker, int tmo) {
        if (!active) return;
        sworkers[worker].avgTmo += tmo;
    }

    static public void updateSuspected(int worker) {
        if (!active) return;
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
//            sts.txCount += sworkers[i].txCount;
            sts.optimisticDec += sworkers[i].optimisticDec;
            sts.totalDec += sworkers[i].totalDec;
            sts.pos += sworkers[i].pos;
            sts.neg += sworkers[i].neg;
            sts.syncEvents += sworkers[i].syncEvents;
            sts.headerTotalTD += sworkers[i].headerTotalTD;
            sts.headersTotalPD += sworkers[i].headersTotalPD;
            sts.totalT += sworkers[i].totalT;
            sts.totalP += sworkers[i].totalP;
            sts.totalNegDecTime += sworkers[i].totalNegDecTime;
            sts.avgTmo += sworkers[i].avgTmo;
            sts.avgActTmo += sworkers[i].avgActTmo;
            sts.suspected += BFD.size(i);
            maxT = max(maxT, sworkers[i].maxTmo);
        }
        sts.suspected /= workers;
        sts.maxTmo = maxT;
        sts.txCount = getAllTxs();

        return sts;
    }

    static int getAllTxs() {
        int count = 0;
        int minS = wTxs[0].size();
        for (int i = 1 ; i < workers ; i++) {
            minS = min(minS, wTxs[i].size());
        }
        for (int i = 0 ; i < workers ; i++) {
            for (int j = 0 ; j < minS ; j++) {
                count += wTxs[i].remove();
            }
        }
        return count;
    }

    public int getTxCount() {
        return txCount;
    }

    public int getOptimisticDec() {
        return optimisticDec;
    }

    public int getTotalDec() {
        return totalDec;

    }

    public int getPosDec() {
        return pos;
    }

    public int getNegDec() {
        return neg;
    }

    public int getSyncEnvents() {
        return syncEvents;
    }

    public long getHeadersTotalTD() {
        return headerTotalTD;
    }

    public long getHeadersTtoalPD() {
        return headersTotalPD;
    }

    public int getTotalT() {
        return totalT;
    }

    public int getTotalP() {
        return totalP;
    }

    public long getTotalNegTime() {
        return totalNegDecTime;
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
