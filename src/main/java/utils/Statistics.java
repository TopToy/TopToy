package utils;

import proto.Types;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
    // Note that due to late nodes this list may grow infinitely. Currently we assume that this is an uncommon case.
    private ConcurrentHashMap<Types.BlockHeader, Types.HeaderStatistics> headerStats;

    private AtomicInteger txCount;
    private AtomicInteger totalDec;
    private AtomicInteger optimisticDec;
    private AtomicInteger syncEvents;
    private AtomicInteger pos;
    private AtomicInteger neg;
    private AtomicLong headerTotalTD;
    private AtomicLong headersTotalPD;
    private AtomicInteger totalT;
    private AtomicInteger totalP;

    static private Statistics[] sworkers;
    static private long start;
    static private long stop;
    static private int workers;

    public Statistics() {
        headerStats = new ConcurrentHashMap<>();
        txCount = new AtomicInteger(0);
        totalDec = new AtomicInteger(0);
        optimisticDec = new AtomicInteger(0);
        syncEvents = new AtomicInteger(0);
        pos = new AtomicInteger(0);
        neg = new AtomicInteger(0);
        headerTotalTD = new AtomicLong(0);
        headersTotalPD = new AtomicLong(0);
        totalT = new AtomicInteger(0);
        totalP = new AtomicInteger(0);
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
        sworkers[worker].headerStats.putIfAbsent(h, Types.HeaderStatistics.newBuilder().setProposed(System.currentTimeMillis()).build());
    }

    static public void updateHeaderTD(Types.BlockHeader h, int worker) {
        if (!sworkers[worker].headerStats.containsKey(h)) return;
        sworkers[worker].headerStats.computeIfPresent(h, (k, v) -> v.toBuilder().setTD(System.currentTimeMillis() - v.getProposed()).build());
    }

    static public void updateHeaderPD(Types.BlockHeader h, int worker) {
        if (!sworkers[worker].headerStats.containsKey(h)) return;
        sworkers[worker].headerStats.computeIfPresent(h, (k, v) -> v.toBuilder().setPD(System.currentTimeMillis() - v.getProposed()).build());
    }

    static public void updateHeaderStatus(Types.BlockHeader h, int worker) {
        if (!sworkers[worker].headerStats.containsKey(h)) return;
        sworkers[worker].headerTotalTD.getAndAdd(sworkers[worker].headerStats.get(h).getTD());
        sworkers[worker].headersTotalPD.getAndAdd(sworkers[worker].headerStats.get(h).getPD());
        sworkers[worker].headerStats.remove(h);
    }

    static public void updateStart() {
        start = System.currentTimeMillis();
    }

    static public void updateStop() {
        stop = System.currentTimeMillis();
    }

    static public void updateTxCount(int worker, int count) {
        sworkers[worker].txCount.getAndAdd(count);
    }

    static public void updateTotalDec(int worker) {
        sworkers[worker].totalDec.getAndIncrement();
    }

    static public void updateOptimisitcDec(int worker) {
        sworkers[worker].optimisticDec.getAndIncrement();
    }

    static public void updateSyncEvents(int worker) {
        sworkers[worker].syncEvents.getAndIncrement();
    }

    static public void updatePosDec(int worker) {
        sworkers[worker].pos.getAndIncrement();
    }

    static public void updateNegDec(int worker) {
        sworkers[worker].neg.getAndIncrement();
    }

    static public void updateT(int worker) {
        sworkers[worker].totalT.getAndIncrement();
    }

    static public void updateP(int worker) {
        sworkers[worker].totalP.getAndIncrement();
    }

    static public long getStart() {
        return start;
    }

    static public long getStop() {
        return stop;
    }

    static public Statistics getStatistics() {
        Statistics sts = new Statistics();
        for (int i = 0 ; i < workers ; i++) {
            sts.txCount.getAndAdd(sworkers[i].txCount.get());
            sts.optimisticDec.getAndAdd(sworkers[i].optimisticDec.get());
            sts.totalDec.getAndAdd(sworkers[i].totalDec.get());
            sts.pos.getAndAdd(sworkers[i].pos.get());
            sts.neg.getAndAdd(sworkers[i].neg.get());
            sts.syncEvents.getAndAdd(sworkers[i].syncEvents.get());
            sts.headerTotalTD.getAndAdd(sworkers[i].headerTotalTD.get());
            sts.headersTotalPD.getAndAdd(sworkers[i].headersTotalPD.get());
            sts.totalT.getAndAdd(sworkers[i].totalT.get());
            sts.totalP.getAndAdd(sworkers[i].totalP.get());
        }
        return sts;
    }

    public int getTxCount() {
        return txCount.get();
    }

    public int getOptimisticDec() {
        return optimisticDec.get();
    }

    public int getTotalDec() {
        return totalDec.get();
    }

    public int getPosDec() {
        return pos.get();
    }

    public int getNegDec() {
        return neg.get();
    }

    public int getSyncEnvents() {
        return syncEvents.get();
    }

    public long getHeadersTotalTD() {
        return headerTotalTD.get();
    }

    public long getHeadersTtoalPD() {
        return headersTotalPD.get();
    }

    public int getTotalT() {
        return totalT.get();
    }

    public int getTotalP() {
        return totalP.get();
    }




}
