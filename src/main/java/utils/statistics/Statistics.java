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

    static public void activate() {
        active = true;
        h1 = BCS.height();
        start = System.currentTimeMillis();
    }

    static public void deactivate() {
        active = false;
        h2 = BCS.height();
        stop = System.currentTimeMillis();
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

}
