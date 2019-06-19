package servers;

import blockchain.data.BCS;
import communication.CommLayer;

import das.wrb.WRB;
import proto.types.block.*;
import static java.lang.String.format;

class ToyServer extends ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyServer.class);

    ToyServer(int id, int worker, int n, int f, int maxTx, boolean fastMode,
              CommLayer comm) {
        super(id, worker, n, f, maxTx, fastMode, comm);
    }

    BlockHeader leaderImpl() {
        addTransactionsToCurrBlock();
        sendCurrentBlockIfNeeded();
        if (!fastMode) normalLeaderPhase();
        return fastModePhase();
    }


    private BlockHeader normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
        logger.info(format("[#%d-C[%d]] prepare to disseminate a new block header for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight, cidSeries, cid));
        WRB.WRBBroadcast(getHeaderForCurrentBlock(BCS.nbGetBlock(worker, currHeight - 1).getHeader(),
                currHeight, cidSeries, cid));
        return null;
    }

    private BlockHeader fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.info(format("[#%d-C[%d]] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight + 1, cidSeries, cid + 1));
        return getHeaderForCurrentBlock(null, currHeight + 1, cidSeries, cid + 1);
    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {

    }


}
