package blockchain;

import proto.Block;

public class basicBlockchain extends blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(basicBlockchain.class);
    public basicBlockchain(int creatorID) {
        super(creatorID);
    }

    @Override
    block createNewBLock() {
        return new basicBlock(getCreatorID());
    }

    @Override
    void createGenesis() {
        addBlock(new basicBlock( -1).construct(0, 0));
    }

    @Override
    boolean validateBlockData(Block b) {
        return true;
    }



}
