package blockchain;
import proto.Types;

public class basicBlockchain extends blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(basicBlockchain.class);

    public basicBlockchain(blockchain orig, int start, int end) {
        super(orig, start, end);
    }
    public basicBlockchain(int creatorID) {
        super(creatorID);
    }

    @Override
    public block createNewBLock() {
        return new basicBlock();
    }

    @Override
    void createGenesis() {
        addBlock(new basicBlock()
                .construct(-1, 0, -1, -1, new byte[0]));
    }

//    @Override
//    public boolean validateBlockData(Types.Block b) {
//        return true;
//    }
}
