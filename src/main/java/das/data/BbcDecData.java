package das.data;

public class BbcDecData {
    public boolean dec;
    public boolean fv;

    public BbcDecData(boolean dec, boolean fv) {
        this.dec = dec;
        this.fv = fv;
    }
    public int getDec() {
        if (fv) return dec ? 1 : -1;
        return dec ? 1 : 0;
    }
}
