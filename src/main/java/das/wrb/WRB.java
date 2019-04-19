package das.wrb;

import com.google.protobuf.ByteString;
import communication.CommLayer;
import config.Node;
import crypto.DigestMethod;
import crypto.blockDigSig;
import das.bbc.OBBC;
import das.data.BbcDecData;
import das.data.Data;

import das.ms.BFD;
import das.utils.Utils;
import io.grpc.stub.StreamObserver;
import proto.Types.*;
import proto.WrbGrpc;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static das.bbc.OBBC.setFastBbcVote;
import static das.utils.Utils.origTmo;
import static das.utils.Utils.setTmo;
import static java.lang.Math.max;
import static java.lang.String.format;
import static utils.Statistics.*;

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
        new Utils(n, workers, tmo);
        new Data(workers);
        new BFD(n, f, tmo, 10);
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
        updateTotalDec(worker);
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
            updateNegDec(worker);
            return null;
        }
//        currentTmo[sender][worker] = tmo;
        updatePosDec(worker);
        if (dec.fv) {
            updateOptimisitcDec(worker);
        }
        logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, worker, 1, cidSeries, cid));

        return postDeliverLogic(key, worker, cidSeries, cid, sender, height);
    }

    static private void preDeliverLogic(Meta key, int worker, int cidSeries, int cid, int sender) throws InterruptedException {
//        int realTmo = currentTmo[sender][worker];
        if (Data.pending[worker].containsKey(key) && BFD.isSuspected(id)) {
            Utils.setTmo(sender, worker, tmo);
            BFD.handleSuspection(sender, tmo);
            return;
        }
        int realTmo = Utils.getTmo(sender, worker);
        BFD.handleSuspection(sender, realTmo);
        if (BFD.isSuspected(sender)) {
            logger.info(format("[#%d-C[%d]] node [%d] is suspected, we will not wait for it [cidSeries=%d ; cid=%d]",
                    id, worker, sender, cidSeries, cid));
            return; // kind of reputation mechanism? it gives the node 10 tries before being excluded from proposing
        }

        updateMaxTmo(worker, realTmo);


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
        logger.debug(format("[#%d-C[%d]] have waited [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                id, worker, estimatedTime, cidSeries, cid));
//        updateTmo(worker, Utils.getTmo(sender, worker));
        updateActTmo(worker, (int) estimatedTime);
        if (Data.pending[worker].containsKey(key)) {
            Utils.updateTmo(sender, worker, (int) estimatedTime); // Fast decrease
        } else {
            if (estimatedTime == 1) {
                estimatedTime = 2;
            }
            Utils.updateTmoVars(sender, worker, (int) estimatedTime + origTmo); // Fast decrease
            setTmo(sender, worker, (int) estimatedTime + origTmo); // Fast recovery

        }


//        if (!Data.pending[worker].containsKey(key)) {
//            currentTmo[sender][worker] += tmo;
//        } else {
//            int epsilon = 1;
//            currentTmo[sender][worker] = (int) max(estimatedTime, epsilon);
//        }

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
