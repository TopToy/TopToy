package das.wrb;

import das.bbc.OBBC;
import das.data.BbcDecData;
import das.data.Data;

import das.ms.BFD;
import das.utils.TmoUtils;
import utils.config.yaml.ServerPublicDetails;
import utils.statistics.Statistics;

import static das.bbc.OBBC.setFastBbcVote;
import static das.utils.TmoUtils.*;
import static java.lang.Math.max;
import static java.lang.String.format;
import static utils.commons.createMeta;

import proto.types.block.*;
import proto.types.meta.*;
import proto.types.wrb.*;

public class WRB {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WRB.class);

    private static int id;
    private static int n;
    private static int f;
    private static WrbRpcs rpcs;

    public WRB(int id, int workers, int n, int f, int tmo, ServerPublicDetails[] cluster,
               String serverCrt, String serverPrivKey, String caRoot) {

        WRB.f = f;
        WRB.n = n;
        WRB.id = id;

        new TmoUtils(n, workers, tmo);
        new Data(workers);
        new BFD(n, f, workers,20 * tmo);
        BFD.activateAll();
        WRB.rpcs = new WrbRpcs(id, workers, n, f, cluster, serverCrt, serverPrivKey, caRoot);
        logger.info(format("Initiated WRB: [id=%d; n=%d; f=%d; tmo=%d]", id, n, f, tmo));
    }

    static public void reconfigure() {

    }
    static public void start() {
        rpcs.start();
    }

    static public void shutdown() {
        rpcs.shutdown();
        logger.info(format("[#%d] shutting down wrb service", id));
    }

    static public void WRBBroadcast(BlockHeader h) {
        rpcs.wrbBroadcast(h);
    }

    static public void WRBSend(BlockHeader h, int[] recipients) {
        rpcs.wrbSend(h, recipients);
    }

    static public BlockHeader WRBDeliver(int worker, int cidSeries, int cid, int sender, int height, int version, BlockHeader next)
            throws InterruptedException {
        Statistics.updateAll();
        Meta key = createMeta(worker, cid, cidSeries, version);

        preDeliverLogic(key, sender);

        BbcDecData dec = OBBC.propose(setFastBbcVote(key, sender, next), worker, height, sender);

        if (!dec.getDec()) {
            logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, worker, 0, cidSeries, cid));
            Statistics.updateNeg();
            return null;
        }
        Statistics.updatePos();
        if (dec.fv) {
            Statistics.updateOpt();
        }
        logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, worker, 1, cidSeries, cid));

        return postDeliverLogic(key, height, sender);
    }

    static private void preDeliverLogic(Meta key, int sender) throws InterruptedException {
        int worker = key.getChannel();
        int cidSeries = key.getCidSeries();
        int cid = key.getCid();
        if (BFD.isSuspected(sender, worker)) {
            if (!Data.pending[worker].containsKey(key)) {
                logger.info(format("Suspect [%d:%d] skipping...", sender, worker));
                return;
            }
            BFD.unsuspect(sender, worker);
        }


        int realTmo = TmoUtils.getTmo(sender, worker);

        Statistics.updateTmo(realTmo);
        long startTime = System.currentTimeMillis();
        synchronized (Data.pending[worker]) {
            while (realTmo > 0 && !Data.pending[worker].containsKey(key)) {
                logger.debug(format("[#%d-C[%d]] going to sleep for at most [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                        id, worker, realTmo, cidSeries, cid));
                Data.pending[worker].wait(realTmo);

                realTmo -= max(0, (System.currentTimeMillis() - startTime));
            }
        }


        long estimatedTime = System.currentTimeMillis() - startTime;
        updateTmo(sender, worker, (int) estimatedTime, Data.pending[worker].containsKey(key));
        logger.debug(format("[#%d-C[%d]] have waited [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                id, worker, estimatedTime, cidSeries, cid));

        Statistics.updateActTmo((int) estimatedTime);
        Statistics.updateMaxTmo((int) estimatedTime);
        if (Data.pending[worker].containsKey(key)) {
            BFD.activate(worker);
        }
        if (BFD.isExceedsThreshold((int) estimatedTime)) {
            BFD.suspect(sender, worker, getTmo(sender, worker));
        }

    }

    static private BlockHeader postDeliverLogic(Meta key, int height, int sender) throws InterruptedException {
        requestData(key, height, sender);
        int channel = key.getChannel();
        if (!Data.pending[channel].containsKey(key)) {
            logger.error("header was not found [cidSeries=d ; cid=%d]");
        }
        return Data.pending[channel].get(key);
    }

    static private void requestData(Meta key, int height, int sender) throws InterruptedException {

        int channel = key.getChannel();
        if (Data.pending[channel].containsKey(key)) return;

        WrbReq req = WrbReq.newBuilder()
                .setMeta(key)
                .setHeight(height)
                .setSender(id)
                .build();
        rpcs.broadcastReqMsg(req, key, sender);
        synchronized (Data.pending[channel]) {
            while (!Data.pending[channel].containsKey(key)) {
                Data.pending[channel].wait();
            }
        }

    }

    
}
