package servers;

import blockchain.Blockchain;
import blockchain.Utils;
import communication.CommLayer;
import config.Node;
import das.ab.ABService;
import das.wrb.WRB;
import proto.Types;

import java.util.ArrayList;
import java.util.Random;

import static blockchain.Utils.createBlockchain;
import static java.lang.String.format;

public class AsyncToyServer extends ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AsyncToyServer.class);

    int maxTime = 0;
    public AsyncToyServer(int id, int worker, int n, int f, int maxTx, boolean fastMode,
                      CommLayer comm) {
        super(id, worker, n, f, maxTx, fastMode, comm);
    }


    Types.BlockHeader leaderImpl() throws InterruptedException {
        if (maxTime > 0) {
            Random rand = new Random();
            int x = rand.nextInt(maxTime);
//            int x = maxTime;
            logger.info(format("[#%d] sleeps for %d ms", getID(), x));
            Thread.sleep(x);
        }
        addTransactionsToCurrBlock();
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }


    private Types.BlockHeader normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
//        broadcastEmptyIfNeeded();
        logger.debug(format("[#%d -C[%d]] prepare to disseminate a new block header for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight, cidSeries, cid));

        WRB.WRBBroadcast(getHeaderForCurrentBlock(bc.getBlock(currHeight - 1).getHeader(),
                currHeight, cidSeries, cid));
        return null;
    }

    Types.BlockHeader fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
//        broadcastEmptyIfNeeded();
        logger.debug(format("[#%d-C[%d]] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight + 1, cidSeries, cid + 1));
        return getHeaderForCurrentBlock(null, currHeight + 1, cidSeries, cid + 1);
    }

    @Override
    public Blockchain initBC(int id, int channel) {
        return createBlockchain(Utils.BCT.SGC, id, 10000, sPath);
    }

    @Override
    public Blockchain getBC(int start, int end) {
        return new Blockchain(this.bc, start, end);
    }

    @Override
    public Blockchain getEmptyBC() {
        return new Blockchain(this.getID());
    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {

    }

    public void setAsyncParam(int maxTime) {
        this.maxTime = maxTime;

    }
}
