package blockchain;

import config.Node;
import consensus.RBroadcast.RBrodcastService;
import consensus.bbc.bbcService;
import crypto.DigestMethod;

import proto.Types.*;
import rmf.RmfNode;

import java.util.ArrayList;

import static java.lang.String.format;

public class cbcServer extends bcServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cbcServer.class);

    public cbcServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                     int maxTx, boolean fastMode, ArrayList<Node> cluster, RmfNode rmf, RBrodcastService panic, RBrodcastService sync) {
        super(addr, rmfPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster, rmf, panic, sync);
    }

    public cbcServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                     int maxTx, boolean fastMode, ArrayList<Node> cluster,
                     String bbcConfig, String panicConfig, String syncConfig,  String serverCrt, String serverPrivKey, String caRoot) {
        super(addr, rmfPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster,
                bbcConfig, panicConfig, syncConfig, serverCrt, serverPrivKey, caRoot);
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
        logger.debug(format("[#%d] prepare to disseminate a new block of [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), currHeight, cidSeries, cid));
        addTransactionsToCurrBlock();
        Block sealedBlock = currBlock.construct(getID(), currHeight, cidSeries, cid, DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
        rmfServer.broadcast(channel, cidSeries, cid, sealedBlock.toByteArray(), currHeight);
        return null;
    }

    byte[] fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.debug(format("[#%d] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), currHeight + 1, cidSeries, cid + 1));
        addTransactionsToCurrBlock();
        return currBlock.construct(getID(), currHeight + 1, cidSeries, cid + 1, new byte[0]).toByteArray();
    }

    @Override
    public blockchain initBC(int id) {
        return new basicBlockchain(id);
    }

    @Override
    public blockchain getBC(int start, int end) {
        return new basicBlockchain(this.bc, start, end);
    }
}
