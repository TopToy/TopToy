package blockchain;

public class basicBlockchain extends blockchain {
    basicBlock current;

    public basicBlockchain(int creatorID) {
        super(creatorID);
    }

    @Override
    abstractBlock createNewBLock() {
        return new basicBlock(getHeight(), getCreatorID(), getBlock(getHeight() - 1).hashCode());
    }

    @Override
    void createGenesis() {
        addBlock(new basicBlock(0, -1, 0));
    }


}
