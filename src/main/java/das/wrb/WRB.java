package das.wrb;

import com.google.protobuf.ByteString;
import communication.CommLayer;
import config.Node;
import crypto.DigestMethod;
import crypto.blockDigSig;
import das.bbc.OBBC;
import das.data.BbcDecData;
import das.data.Data;

import io.grpc.stub.StreamObserver;
import proto.Types.*;
import proto.WrbGrpc;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static das.bbc.OBBC.setFastBbcVote;
import static java.lang.Math.max;
import static java.lang.String.format;

public class WRB {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WRB.class);

    private static int id;
    private static int n;
    private static int f;
    private static int tmo;
    private static int tmoInterval;
    private static int[] currentTmo;
    private static AtomicInteger totalDeliveredTries = new AtomicInteger(0);
    private static AtomicInteger optimialDec = new AtomicInteger(0);
    private static AtomicInteger pos = new AtomicInteger(0);
    private static AtomicInteger neg = new AtomicInteger(0);

    private static WrbRpcs rpcs;
    private static CommLayer comm;

    public WRB(int id, int workers, int n, int f, int tmo, int tmoInterval, ArrayList<Node> wrbCluster,
               String serverCrt, String serverPrivKey, String caRoot, CommLayer comm) {
        WRB.tmo = tmo;
        WRB.tmoInterval = tmoInterval;
        WRB.currentTmo = new int[workers];
        WRB.f = f;
        WRB.n = n;
        WRB.id = id;
        for (int i = 0 ; i < workers ; i++) {
            WRB.currentTmo[i] = tmo;
        }
        WRB.comm = comm;
        new Data(workers);
        WRB.rpcs = new WrbRpcs(id, workers, n, f, wrbCluster, serverCrt, serverPrivKey, caRoot);
        logger.info(format("Initiated WRB: [id=%d; n=%d; f=%d; tmo=%d; tmoInterval=%d]", id, n, f, tmo,
                tmoInterval));
    }


    static public void start() {
        rpcs.start();
    }

    static public long getTotalDeliveredTries() {
        return totalDeliveredTries.get();
    }

    static public int getTotalPos() {
        return pos.get();
    }

    static public int getTotalNeg() {
        return neg.get();
    }

    static public long getOptimialDec() {
        return optimialDec.get();
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

    static public BlockHeader WRBDeliver(int channel, int cidSeries, int cid, int sender, int height, BlockHeader next)
            throws InterruptedException {
        totalDeliveredTries.incrementAndGet();
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();

        preDeliverLogic(key, channel, cidSeries, cid);
        BbcDecData dec = OBBC.propose(setFastBbcVote(key, channel, sender, cidSeries, cid, next), channel, height, sender);

        if (!dec.getDec()) {
            currentTmo[channel] += tmoInterval;
            logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, 0, cidSeries, cid));
            neg.getAndIncrement();
            return null;
        }
        currentTmo[channel] = tmo;
        if (dec.fv) {
            pos.getAndIncrement();
            optimialDec.getAndIncrement();
        }
        logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, 1, cidSeries, cid));

        return postDeliverLogic(key, channel, cidSeries, cid, sender, height);
    }

    static private void preDeliverLogic(Meta key, int channel, int cidSeries, int cid) throws InterruptedException {
        int realTmo = currentTmo[channel];
        long startTime = System.currentTimeMillis();
        synchronized (Data.pending[channel]) {
            while (realTmo > 0 && !Data.pending[channel].containsKey(key)) {
                logger.debug(format("[#%d-C[%d]] going to sleep for at most [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                        id, channel, realTmo, cidSeries, cid));
                Data.pending[channel].wait(realTmo);

                realTmo -= max(0, (System.currentTimeMillis() - startTime));
//                logger.debug(format("[#%d-C[%d]] real TMO is [%d] ms for data msg [cidSeries=%d ; cid=%d]",
//                        id, channel, realTmo, cidSeries, cid));
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.debug(format("[#%d-C[%d]] have waited [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                id, channel, estimatedTime, cidSeries, cid));
    }

    static private BlockHeader postDeliverLogic(Meta key, int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        requestData(channel, cidSeries, cid, sender, height);
        BlockHeader msg = Data.pending[channel].get(key);
        return msg;
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
