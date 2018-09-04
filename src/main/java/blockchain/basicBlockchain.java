package blockchain;
import proto.Types;

public class basicBlockchain extends blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(basicBlockchain.class);

    public basicBlockchain(blockchain orig, int start, int end) {
        super(orig, start, end);
    }
    public basicBlockchain(int creatorID, int channel) {
        super(creatorID, channel);
    }

    @Override
    public block createNewBLock() {
        return new basicBlock();
    }

    @Override
    void createGenesis(int channel) {
        addBlock(new basicBlock()
                .construct(-1, 0, -1, -1, channel,  null));
    }

//    @Override
//    public boolean validateBlockData(Types.Block b) {
//        return true;
//    }
}
