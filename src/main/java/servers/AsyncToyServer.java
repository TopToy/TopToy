package servers;

import blockchain.data.BCS;
import communication.CommLayer;
import das.wrb.WRB;
import proto.Types;

import java.util.Random;

import static java.lang.String.format;

public class AsyncToyServer extends ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AsyncToyServer.class);

    private int maxTime = 0;
    AsyncToyServer(int id, int worker, int n, int f, int maxTx, boolean fastMode,
                   CommLayer comm) {
        super(id, worker, n, f, maxTx, fastMode, comm);
    }


    Types.BlockHeader leaderImpl() throws InterruptedException {
        if (maxTime > 0) {
            Random rand = new Random();
            int x = rand.nextInt(maxTime);
            logger.info(format("[#%d] sleeps for %d ms", getID(), x));
            Thread.sleep(x);
        }
        addTransactionsToCurrBlock();
        if (!fastMode) normalLeaderPhase();
        return fastModePhase();
    }


    private Types.BlockHeader normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
//        broadcastEmptyIfNeeded();
        logger.debug(format("[#%d -C[%d]] prepare to disseminate a new block header for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight, cidSeries, cid));

        WRB.WRBBroadcast(getHeaderForCurrentBlock(BCS.nbGetBlock(worker, currHeight - 1).getHeader(),
                currHeight, cidSeries, cid));
        return null;
    }

    private Types.BlockHeader fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.debug(format("[#%d-C[%d]] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight + 1, cidSeries, cid + 1));
        return getHeaderForCurrentBlock(null, currHeight + 1, cidSeries, cid + 1);
    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {

    }

    void setAsyncParam(int maxTime) {
        this.maxTime = maxTime;

    }
}
