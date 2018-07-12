package blockchain;

import proto.Blockchain;

public class basicBlock extends abstractBlock {

    public basicBlock(int creatorID, int prevHash) {
        super(creatorID, prevHash);
    }

    @Override
    boolean validateTransaction(Blockchain.Transaction t) {
        return true;
    }
}
