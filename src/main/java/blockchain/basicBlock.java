package blockchain;

import proto.Transaction;

public class basicBlock extends block {

    public basicBlock(int creatorID) {
        super(creatorID);
    }

    @Override
    boolean validateTransaction(Transaction t) {
        return true;
    }
}
