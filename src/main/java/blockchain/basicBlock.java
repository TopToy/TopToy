package blockchain;

import proto.Transaction;

public class basicBlock extends abstractBlock {

    public basicBlock(int height, int creatorID, int prevHash) {
        super(height, creatorID, prevHash);
    }

    @Override
    boolean validateTransaction(Transaction t) {
        return true;
    }
}
