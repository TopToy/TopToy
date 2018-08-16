package blockchain;

import crypto.DigestMethod;

import proto.Types.*;
import static java.lang.String.format;

public class cbcServer extends bcServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cbcServer.class);

    public cbcServer(String addr, int rmfPort, int id) {
        super(addr, rmfPort, id);
    }

    void leaderImpl() {
        if (currLeader != getID()) {
            return;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

//        synchronized (blockLock) {
            addTransactionsToCurrBlock();
            Block sealedBlock = currBlock.construct(getID(), currHeight, DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
//            currBlock = bc.createNewBLock();
            rmfServer.broadcast(cidSeries, cid, sealedBlock.toByteArray(), currHeight);
//        }
    }

    @Override
    blockchain initBC(int id) {
        return new basicBlockchain(id);
    }

    @Override
    blockchain getBC(int start, int end) {
        return new basicBlockchain(this.bc, start, end);
    }
}
