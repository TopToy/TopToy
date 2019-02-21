package servers;

import blockchain.BaseBlockchain;
import blockchain.SBlockchain;
import config.Node;
import das.RBroadcast.RBrodcastService;
import proto.Types;
import das.wrb.WrbNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.String.format;

public class AsyncToyServer extends ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AsyncToyServer.class);

    int maxTime = 0;

    public AsyncToyServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                          int maxTx, boolean fastMode, ArrayList<Node> cluster, WrbNode rmf,
                          RBrodcastService panic, RBrodcastService sync) {
        super(addr, rmfPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster, rmf, panic, sync);
    }
    public AsyncToyServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                          int maxTx, boolean fastMode, ArrayList<Node> cluster,
                          String bbcConfig, String panicConfig, String syncConfig,
                          String serverCrt, String serverPrivKey, String caRoot) {
        super(addr, rmfPort,  id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster,
                bbcConfig, panicConfig, syncConfig, serverCrt, serverPrivKey, caRoot);
    }

    Types.Block leaderImpl() throws InterruptedException {
        if (maxTime > 0) {
            Random rand = new Random();
            int x = rand.nextInt(maxTime);
//            int x = maxTime;
            logger.info(format("[#%d] sleeps for %d ms", getID(), x));
            Thread.sleep(x);
        }
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }

    Types.Block normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
        logger.debug(format("[#%d] prepare to disseminate a new block of [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), currHeight, cidSeries, cid));
        addTransactionsToCurrBlock();
        Types.Block sealedBlock = currBlock.construct(getID(), currHeight, cidSeries, cid, channel, bc.getBlock(currHeight - 1).getHeader());
        rmfServer.broadcast(sealedBlock);
        return null;
    }

    Types.Block fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.debug(format("[#%d] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), currHeight + 1, cidSeries, cid + 1));
        addTransactionsToCurrBlock();
        return currBlock.construct(getID(), currHeight + 1, cidSeries, cid + 1, channel, null);
    }

    public void setAsyncParam(int maxTime) {
        this.maxTime = maxTime;

    }
    @Override
    public BaseBlockchain initBC(int id, int channel) {
        return new SBlockchain(id, channel);
    }

    @Override
    public BaseBlockchain getBC(int start, int end) {
        try {
            return new SBlockchain(this.bc, start, end);
        } catch (IOException e) {
            logger.error("Unable to return the blockchain", e);
            return null;
        }
    }

    @Override
    public BaseBlockchain getEmptyBC() {
        return new SBlockchain(getID());
    }
}
