package servers;

import blockchain.BaseBlockchain;
import blockchain.SBlockchain;
import config.Node;
import das.RBroadcast.RBrodcastService;
import proto.Types.*;
import das.wrb.WrbNode;
import java.util.ArrayList;

import static java.lang.String.format;

public class ToyServer extends ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyServer.class);

    public ToyServer(String addr, int wrbPort, int id, int channel, int f, int maxTx, boolean fastMode,
                     WrbNode wrb, RBrodcastService rb) {
        super(addr, wrbPort, id, channel, f, maxTx, fastMode, wrb, rb);
    }

    public ToyServer(String addr, int wrbPort, int id, int channel, int f, int tmo, int tmoInterval,
                     int maxTx, boolean fastMode, ArrayList<Node> cluster,
                     String bbcConfig, String rbConfigPath,  String serverCrt, String serverPrivKey, String caRoot) {
        super(addr, wrbPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster,
                bbcConfig, rbConfigPath, serverCrt, serverPrivKey, caRoot);
    }

    Block leaderImpl() {
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }

    private Block normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
        logger.debug(format("[#%d -C[%d]] prepare to disseminate a new block of [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), channel, currHeight, cidSeries, cid));
        addTransactionsToCurrBlock();
//        Block sealedBlock = currBlock.construct(getID(), currHeight, cidSeries, cid, channel, bc.getBlock(currHeight - 1).getHeader());
        Block sealedBlock;
        synchronized (blocksForPropose.element()) {
            sealedBlock = blocksForPropose
                    .element()
                    .construct(getID(), currHeight, cidSeries, cid, channel, bc.getBlock(currHeight - 1).getHeader());
        }
        rmfServer.broadcast(sealedBlock);
        return null;
    }

    Block fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.debug(format("[#%d-C[%d]] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), channel, currHeight + 1, cidSeries, cid + 1));
        addTransactionsToCurrBlock();
        synchronized (blocksForPropose.element()) {
            return blocksForPropose
                    .element()
                    .construct(getID(), currHeight + 1, cidSeries, cid + 1, channel, null);
        }
//        return currBlock.construct(getID(), currHeight + 1, cidSeries, cid + 1, channel, null);
    }

    @Override
    public BaseBlockchain initBC(int id, int channel) {
        return new SBlockchain(id, channel, 0, sPath);
    }

    @Override
    public BaseBlockchain getBC(int start, int end) {
        return new SBlockchain(this.bc, start, end);
    }

    @Override
    public BaseBlockchain getEmptyBC() {
        return new SBlockchain(this.getID());
    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {

    }

}
