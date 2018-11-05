package servers;

public class statistics {
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

    public statistics() {
        txCount = 0;
        firstTxTs = 0;
        lastTxTs = 0;
        delaysSum = 0;
        totalDec = 0;
        optemisticDec = 0;
        eb = 0;
    }
}
