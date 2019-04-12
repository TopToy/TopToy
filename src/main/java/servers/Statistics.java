package servers;

public class Statistics {
    public int txCount;
    public long start;
    public long stop;
    public long delaysSum;
    public int txSize;
    public long totalDec;
    public long optemisticDec;
    public int eb;
    public int all = 0;
    public int deliveredTime = 0;
    public int syncEvents = 0;
    public int pos = 0;
    public int neg = 0;

    public Statistics() {
        txCount = 0;
        start = 0;
        stop = 0;
        delaysSum = 0;
        totalDec = 0;
        optemisticDec = 0;
        eb = 0;
    }
}
