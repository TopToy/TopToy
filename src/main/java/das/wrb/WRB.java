package das.wrb;

import config.Node;
import das.bbc.OBBC;
import das.data.BbcDecData;
import das.data.Data;

import das.ms.BFD;
import das.utils.TmoUtils;
import proto.Types.*;
import utils.statistics.Statistics;

import java.util.*;

import static das.bbc.OBBC.setFastBbcVote;
import static das.utils.TmoUtils.*;
import static java.lang.Math.max;
import static java.lang.String.format;
import static utils.statistics.Statistics.*;

public class WRB {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WRB.class);

    private static int id;
    private static int n;
    private static int f;
    private static int tmo;
    private static int tmoInterval;
//    private static int[][] currentTmo;
//    private static AtomicInteger totalDeliveredTries = new AtomicInteger(0);
//    private static AtomicInteger optimialDec = new AtomicInteger(0);
//    private static AtomicInteger pos = new AtomicInteger(0);
//    private static AtomicInteger neg = new AtomicInteger(0);

    private static WrbRpcs rpcs;
//    private static CommLayer comm;

    public WRB(int id, int workers, int n, int f, int tmo, int tmoInterval, ArrayList<Node> wrbCluster,
               String serverCrt, String serverPrivKey, String caRoot) {
        WRB.tmo = tmo;
        WRB.tmoInterval = tmoInterval;
//        WRB.currentTmo = new int[n][workers];
        WRB.f = f;
        WRB.n = n;
        WRB.id = id;
//        for (int j = 0 ; j < n ; j++) {
//            for (int i = 0 ; i < workers ; i++) {
//                WRB.currentTmo[j][i] = tmo;
//            }
//        }

//        WRB.comm = comm;
        new TmoUtils(n, workers, tmo);
        new Data(workers);
        new BFD(n, f, workers,20 * tmo);
        BFD.activateAll();
        WRB.rpcs = new WrbRpcs(id, workers, n, f, wrbCluster, serverCrt, serverPrivKey, caRoot);
        logger.info(format("Initiated WRB: [id=%d; n=%d; f=%d; tmo=%d; tmoInterval=%d]", id, n, f, tmo,
                tmoInterval));
    }


    static public void start() {
        rpcs.start();
    }

//    static public long getTotalDeliveredTries() {
//        return totalDeliveredTries.get();
//    }
//
//    static public int getTotalPos() {
//        return pos.get();
//    }
//
//    static public int getTotalNeg() {
//        return neg.get();
//    }
//
//    static public long getOptimialDec() {
//        return optimialDec.get();
//    }

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

    static public BlockHeader WRBDeliver(int worker, int cidSeries, int cid, int sender, int height, BlockHeader next)
            throws InterruptedException {
//        totalDeliveredTries.incrementAndGet();
        Statistics.updateAll();
        Meta key = Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();

        preDeliverLogic(key, worker, cidSeries, cid, sender);

        BbcDecData dec = OBBC.propose(setFastBbcVote(key, worker, sender, cidSeries, cid, next), worker, height, sender);

        if (!dec.getDec()) {
//            currentTmo[sender][worker] += tmoInterval;
//            currentTmo[sender][worker] += tmo;

            logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, worker, 0, cidSeries, cid));
//            neg.getAndIncrement();
            Statistics.updateNeg();
            return null;
        }
//        currentTmo[sender][worker] = tmo;
        Statistics.updatePos();
        if (dec.fv) {
            Statistics.updateOpt();
        }
        logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, worker, 1, cidSeries, cid));

        return postDeliverLogic(key, worker, cidSeries, cid, sender, height);
    }

    static private void preDeliverLogic(Meta key, int worker, int cidSeries, int cid, int sender) throws InterruptedException {

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

    static private BlockHeader postDeliverLogic(Meta key, int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        requestData(channel, cidSeries, cid, sender, height);
        return Data.pending[channel].get(key);
    }

    static private void requestData(int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (Data.pending[channel].containsKey(key)) return;
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setChannel(channel).
                build();
        WrbReq req = WrbReq.newBuilder()
                .setMeta(meta)
                .setHeight(height)
                .setSender(id)
                .build();
        rpcs.broadcastReqMsg(req,channel, cidSeries, cid, sender);
        synchronized (Data.pending[channel]) {
            while (!Data.pending[channel].containsKey(key)) {
                Data.pending[channel].wait();
            }
        }

    }

    
}
