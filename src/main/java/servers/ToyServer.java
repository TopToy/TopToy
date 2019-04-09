package servers;

import blockchain.Blockchain;
import blockchain.Utils;
import com.google.protobuf.InvalidProtocolBufferException;
import communication.CommLayer;
import config.Node;
import das.ab.ABService;
import java.util.ArrayList;

import das.data.Data;
import das.wrb.WRB;
import proto.Types.*;

import static blockchain.Utils.createBlockchain;
import static java.lang.String.format;

public class ToyServer extends ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyServer.class);

    public ToyServer(int id, int worker, int n, int f, int maxTx, boolean fastMode,
                      CommLayer comm) {
        super(id, worker, n, f, maxTx, fastMode, comm);
    }

//    public ToyServer(String addr, int wrbPort, int commPort, int id, int channel, int f, int tmo, int tmoInterval,
//                     int maxTx, boolean fastMode, ArrayList<Node> wrbCluster, ArrayList<Node> commCluster,
//                     String bbcConfig, String rbConfigPath, String serverCrt, String serverPrivKey, String caRoot) {
//
//        super(addr, wrbPort, commPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, wrbCluster, commCluster,
//                bbcConfig, rbConfigPath, serverCrt, serverPrivKey, caRoot);
//    }



    BlockHeader leaderImpl() {
        addTransactionsToCurrBlock();
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }


    private BlockHeader normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
        logger.debug(format("[#%d -C[%d]] prepare to disseminate a new block header for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight, cidSeries, cid));
        broadcastEmptyIfNeeded();
        WRB.WRBBroadcast(getHeaderForCurrentBlock(bc.getBlock(currHeight - 1).getHeader(),
                currHeight, cidSeries, cid));
        return null;
    }

    BlockHeader fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        broadcastEmptyIfNeeded();
        logger.debug(format("[#%d-C[%d]] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), worker, currHeight + 1, cidSeries, cid + 1));
        return getHeaderForCurrentBlock(null, currHeight + 1, cidSeries, cid + 1);
    }

    @Override
    public Blockchain initBC(int id, int channel) {
        return createBlockchain(Utils.BCT.SGC, id, 100, sPath);
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


}
