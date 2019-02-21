package blockchain;

import java.io.IOException;

public class SBlockchain extends BaseBlockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SBlockchain.class);

    public SBlockchain(BaseBlockchain orig, int start, int end) throws IOException {
        super(orig, start, end);
    }
    public SBlockchain(int creatorID, int channel) {
        super(creatorID, channel);
    }

    public SBlockchain(int creatorID) {
        super(creatorID);
    }

    @Override
    public BaseBlock createNewBLock() {
        return new SBlock();
    }

    @Override
    void createGenesis(int channel) {
        addBlock(new SBlock()
                .construct(-1, 0, -1, -1, channel,  null));
    }

//    @Override
//    public boolean validateBlockData(Types.Block b) {
//        return true;
//    }
}
