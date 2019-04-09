package blockchain.data;

import blockchain.Blockchain;

public class BCS {
    static public Blockchain[] bcs;

    public BCS(int workers) {
        bcs = new Blockchain[workers];
    }

    public void registerBC(int worker, Blockchain bc) {
        bcs[worker] = bc;
    }
}
