package das.utils;


import java.util.LinkedList;
import java.util.Queue;

import static java.lang.Math.max;

public class TmoUtils {

    private static int[][] tmos;
    private static int origTmo;
    private static Queue<Integer>[][] accAvg;
    private static int qSize = 50;
    private static int k;

    public TmoUtils(int n, int workers, int tmo) {
        TmoUtils.origTmo = tmo;
        TmoUtils.tmos = new int[n][workers];
        accAvg = new Queue[n][workers];
        for (int i = 0 ; i < n ; i++) {
            for (int j = 0 ; j < workers ; j++) {
                TmoUtils.tmos[i][j] = tmo;
                accAvg[i][j] = new LinkedList<>();
            }
        }
        int wSize = 5;
        k = 2/(wSize + 1);
    }


    static public int getTmo(int s, int worker) {
        return TmoUtils.tmos[s][worker];
    }

    static public void updateTmo(int s, int worker, int newTmo, boolean received) {
        if (newTmo == 0) {
            return;
        }
        if (!received) {
            TmoUtils.tmos[s][worker] += origTmo;
            return;
        }
        TmoUtils.tmos[s][worker] = max(1, newTmo*k + TmoUtils.tmos[s][worker]*(1-k));
//        updateAccAvg(s, worker, newTmo);
//        TmoUtils.tmos[s][worker] = max(1, calcAvg(s, worker));
//        if (!received) {
//            TmoUtils.tmos[s][worker] += origTmo;
//        }
    }

    static void updateAccAvg(int s, int w, int newTmo) {
        if (accAvg[s][w].size() > qSize) {
            accAvg[s][w].remove();
        }
        accAvg[s][w].add(newTmo);
    }

    static int calcAvg(int s, int w) {
        return accAvg[s][w].stream().mapToInt(t -> t).sum() / qSize;
    }
}
