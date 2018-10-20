package servers;

public class statistics {
    public int txCount;
    public long firstTxTs;
    public long lastTxTs;
    public long delaysSum;
    public int txSize;
    public long totalDec;
    public long optemisticDec;

    public statistics() {
        txCount = 0;
        firstTxTs = 0;
        lastTxTs = 0;
        delaysSum = 0;
        totalDec = 0;
        optemisticDec = 0;
    }
}
