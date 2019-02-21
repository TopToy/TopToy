package servers;

public class Statistics {
    public int txCount;
    public long firstTxTs;
    public long lastTxTs;
    public long delaysSum;
    public int txSize;
    public long totalDec;
    public long optemisticDec;
    public int eb;
    public int all = 0;
    public int deliveredTime = 0;
    public int syncEvents = 0;

    public Statistics() {
        txCount = 0;
        firstTxTs = 0;
        lastTxTs = 0;
        delaysSum = 0;
        totalDec = 0;
        optemisticDec = 0;
        eb = 0;
    }
}
