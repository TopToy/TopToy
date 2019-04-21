package das.utils;


public class TmoUtils {

    private static int[][] tmos;
    public static int origTmo;

    public TmoUtils(int n, int workers, int tmo) {
        TmoUtils.origTmo = tmo;
        TmoUtils.tmos = new int[n][workers];
        for (int i = 0 ; i < n ; i++) {
            for (int j = 0 ; j < workers ; j++) {
                TmoUtils.tmos[i][j] = tmo;
            }
        }
    }

    static private int k() {
        int wSize = 50;
        return 2/(wSize + 1);
    }

    static public int getTmo(int s, int worker) {
        return TmoUtils.tmos[s][worker];
    }

    static public void updateTmo(int s, int worker, int newTmo, boolean received) {
        if (newTmo == 0) {
            return;
        }
        if (!received) {
            TmoUtils.tmos[s][worker] = 2*TmoUtils.tmos[s][worker];
            return;
        }
        TmoUtils.tmos[s][worker] = newTmo*k() + TmoUtils.tmos[s][worker]*(1-k());
    }
}
