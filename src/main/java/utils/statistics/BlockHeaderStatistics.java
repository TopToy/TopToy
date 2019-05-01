package utils.statistics;

public class BlockHeaderStatistics {
    int sender;
    int worker;
    int height;
    long proposedTime;
    long tentativeTime;
    long permanentTime;
    boolean tentative = false;
    boolean definite = false;

    public BlockHeaderStatistics updateTentative() {
        this.tentative = true;
        return this;
    }

    public BlockHeaderStatistics updateDefinite() {
        this.tentative = false;
        this.definite = true;
        return this;
    }

    public BlockHeaderStatistics setSender(int sender) {
        this.sender = sender;
        return this;
    }

    public BlockHeaderStatistics setWorker(int worker) {
        this.worker = worker;
        return this;
    }

    public BlockHeaderStatistics setHeight(int height) {
        this.height = height;
        return this;
    }

//    public BlockHeaderStatistics setActTmo(int actTmo) {
//        this.actTmo = actTmo;
//        return this;
//    }
//
//    public BlockHeaderStatistics setTmo(int tmo) {
//        this.tmo = tmo;
//        return this;
//    }

    public BlockHeaderStatistics setProposedTime(long proposedTime) {
        this.proposedTime = proposedTime;
        return this;
    }

    public BlockHeaderStatistics setTentativeTime(long tentativeTime) {
        this.tentativeTime = tentativeTime;
        return this;
    }

    public BlockHeaderStatistics setPermanentTime(long permanentTime) {
        this.permanentTime = permanentTime;
        return this;
    }

    public int getSender() {
        return sender;
    }

    public int getWorker() {
        return worker;
    }

    public int getHeight() {
        return height;
    }

//    public int getActTmo() {
//        return actTmo;
//    }
//
//    public int getTmo() {
//        return tmo;
//    }

    public long getProposedTime() {
        return proposedTime;
    }

    public long getTentativeTime() {
        return tentativeTime;
    }

    public long getPermanentTime() {
        return permanentTime;
    }

    public boolean getDefinite() {
        return definite;
    }

    public boolean getTentative() {
        return tentative;
    }
}


