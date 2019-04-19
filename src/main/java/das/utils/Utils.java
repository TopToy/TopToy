package das.utils;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Utils {

    private static int[][] tmos;
    private static int[][] counts;
    private static int[][] accTmos;
    public static int origTmo;
    private static int n;
    private static int workers;

    public Utils(int n, int workers, int tmo) {
        Utils.origTmo = tmo;
        Utils.n = n;
        Utils.workers = workers;
        Utils.tmos = new int[n][workers];
        Utils.counts = new int[n][workers];
        Utils.accTmos = new int[n][workers];
        for (int i = 0 ; i < n ; i++) {
            for (int j = 0 ; j < workers ; j++) {
                Utils.tmos[i][j] = tmo;
                Utils.counts[i][j] = 0;
                Utils.accTmos[i][j] = 0;
            }
        }
    }

    static public int getTmo(int s, int worker) {
        return Utils.tmos[s][worker];
    }

    static public void updateTmo(int s, int worker, int newTmo) {
        if (newTmo == 0) {
            return;
        }
        updateTmoVars(s, worker, newTmo);
        Utils.tmos[s][worker] = getAvgTmo(s, worker);
    }

//    static public void updateTmo(int s, int worker, int newTmo) {
//        Utils.tmos[s][worker] = min(Utils.tmos[s][worker] / 2, 1);
//
//    }

    static public void setTmo(int s, int worker, int tmo) {
        Utils.tmos[s][worker] = tmo;
    }

    static private int getAvgTmo(int s, int worker) {
        if (Utils.counts[s][worker] == 0) return origTmo;
        return max(1, Utils.accTmos[s][worker] / Utils.counts[s][worker]);
    }

    static public void updateTmoVars(int s, int worker, int tmo) {
        Utils.accTmos[s][worker] += tmo;
        Utils.counts[s][worker]++;
    }

    static public int getOverallTmoAvg() {
        int res = 0;
        for (int i = 0 ; i < n ; i++) {
            for (int j = 0 ; j < workers ; j++) {
                res += tmos[i][j];
            }
        }
        return max(1, res / (n*workers));
    }
}
