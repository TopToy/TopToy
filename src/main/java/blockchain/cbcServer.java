package blockchain;

import crypto.DigestMethod;

import proto.Types.*;
import static java.lang.String.format;

public class cbcServer extends bcServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cbcServer.class);

    public cbcServer(String addr, int rmfPort, int id) {
        super(addr, rmfPort, id);
    }

    byte[] leaderImpl() {
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }

    byte[] normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), currHeight, cidSeries, cid));
        addTransactionsToCurrBlock();
        Block sealedBlock = currBlock.construct(getID(), currHeight, DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
        rmfServer.broadcast(cidSeries, cid, sealedBlock.toByteArray(), currHeight);
        return null;
    }

    byte[] fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.info(format("[#%d] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), currHeight + 1, cidSeries, cid + 1));
        addTransactionsToCurrBlock();
        return currBlock.construct(getID(), currHeight,
                DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray())).toByteArray();
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
