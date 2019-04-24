package blockchain.data;

import blockchain.Blockchain;

public class BCS {
    static public Blockchain[] bcs;

    public BCS(int workers) {
        bcs = new Blockchain[workers];
        for (int i = 0 ; i < workers ; i++) {
            bcs[i] = new Blockchain(-1);
        }
    }

    public void registerBC(int worker, Blockchain bc) {
        bcs[worker] = bc;
    }
}
