package utils.config.yaml;

public class Settings {
    private int tmo;
    private String atomicbroadcast;
    private int maxTransactionInBlock;
    private int txSize;
    private String caRootPath;

    public int getMaxTransactionInBlock() {
        return maxTransactionInBlock;
    }

    public void setMaxTransactionInBlock(int maxTransactionInBlock) {
        this.maxTransactionInBlock = maxTransactionInBlock;
    }

    public int getTmo() {
        return tmo;
    }

    public void setTmo(int tmo) {
        this.tmo = tmo;
    }

    public int getTxSize() {
        return txSize;
    }

    public void setTxSize(int txSize) {
        this.txSize = txSize;
    }

    public String getAtomicbroadcast() {
        return atomicbroadcast;
    }

    public void setAtomicbroadcast(String atomicbroadcast) {
        this.atomicbroadcast = atomicbroadcast;
    }

    public String getCaRootPath() {
        return caRootPath;
    }

    public void setCaRootPath(String caRootPath) {
        this.caRootPath = caRootPath;
    }

    public Settings() {}
}
