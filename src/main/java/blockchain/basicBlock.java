package blockchain;

import proto.Transaction;

public class basicBlock extends block {

    @Override
    boolean validateTransaction(Transaction t) {
        return true;
    }
}
