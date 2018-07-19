package blockchain;

import com.google.protobuf.ByteString;
import proto.Block;
import proto.Crypto;

public class basicBlockchain extends blockchain {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(basicBlockchain.class);
    public basicBlockchain(int creatorID) {
        super(creatorID, "SHA-256");
    }

    @Override
    block createNewBLock() {
        return new basicBlock();
    }

    @Override
    void createGenesis() {
        addBlock(new basicBlock()
                .construct(-1, 0, new byte[0]));
    }

    @Override
    boolean validateBlockData(Block b) {
        return true;
    }



}
