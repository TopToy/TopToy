package das.bbc;

import config.Node;

import das.data.BbcDecData;

import das.data.Data;
import das.data.VoteData;

import io.grpc.*;
import proto.ObbcGrpc;
import proto.Types;

import java.util.ArrayList;
import java.util.Objects;


import static blockchain.data.BCS.bcs;
import static das.data.Data.*;
import static java.lang.String.format;

public class OBBC extends ObbcGrpc.ObbcImplBase  {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OBBC.class);

    static private int id;
    private static OBBCRpcs rpcs;

    public OBBC(int id, int n, int f, int qSize,  ArrayList<Node> obbcCluster, String caRoot, String serverCrt,
                String serverPrivKey) {
        OBBC.id = id;
        rpcs = new OBBCRpcs(id, n, f, qSize, obbcCluster, caRoot, serverCrt, serverPrivKey);
        logger.info(format("Initiated ABBftSMaRt: [id=%d]", id));

    }

    static public void start() {
        rpcs.start();
    }

    static public void shutdown() {
        rpcs.shutdown();
    }


    static public BbcDecData propose(Types.BbcMsg v, int worker, int height, int expSender) throws InterruptedException
    {
        Types.Meta key = v.getM().toBuilder().clearSender().build();
        rpcs.broadcastFVMessage(v);
        synchronized (bbcFastDec[worker]) {
            while (!bbcFastDec[worker].containsKey(key)) {
                bbcFastDec[worker].wait();
            }
        }

        if (bbcFastDec[worker].get(key).getDec()) {
            logger.debug(format("decided (1) by fast vote [w=%d ; cidSereis=%d ; cid=%d ; height=%d]", worker, key.getCidSeries(), key.getCid(), height));
            return bbcFastDec[worker].get(key);
        }

        pending[worker].computeIfAbsent(key, k -> {
            logger.debug(format("broadcast evidence request [w=%d ; cidSeries=%d ; cid=%d ; height=%d]", worker, key.getCidSeries(), key.getCid(), height));
            rpcs.broadcastEvidenceReq(Types.EvidenceReq.newBuilder()
                    .setHeight(height)
                    .setMeta(v.getM())
                    .build(), key, expSender);
            return null;
        });

        if (!pending[worker].containsKey(key)) {
            synchronized (preConsDone[worker]) {
                while (!preConsDone[worker].contains(key)) {
                    preConsDone[worker].wait();
                }
            }
        }

        boolean vote = false;
        if (pending[worker].containsKey(key)) {
            vote = true;
        }

        return BBC.propose(Types.BbcMsg.newBuilder()
                .setM(v.getM())
                .setVote(vote)
                .setHeight(height)
                .build(), key);
    }
    
    static void reCons(Types.Meta key, int id, int height) {
        int worker = key.getChannel();
        bbcFastDec[worker].computeIfPresent(key, (k1, v1) -> {
            if (v1.getDec()) {
                bbcVotes[worker].computeIfAbsent(key, k2 -> {
                    logger.debug(format("[#%d-C[%d]] (reCons) found that a full bbc initialized, thus propose [cidSeries=%d ; cid=%d]",
                             id, worker, key.getCidSeries(),  key.getCid()));
                    BBC.nonBlockingPropose(Types.BbcMsg.newBuilder()
                            .setM(key.toBuilder().setSender(id).build())
                            .setHeight(height)
                            .setVote(v1.getDec()).build());
                    return new VoteData();
                });
            }
            return v1;
        });
        bbcFastDec[worker].computeIfAbsent(key, k -> {
            if (bcs[worker].contains(height)) {
                logger.debug(format("[#%d-C[%d]] (reCons) found that a full bbc initialized and a block is exist, " +
                                "thus propose [cidSeries=%d ; cid=%d; height=%d]",
                        id, worker, key.getCidSeries(),  key.getCid(), height));
                BBC.nonBlockingPropose(Types.BbcMsg.newBuilder()
                        .setM(key.toBuilder().setSender(id).build())
                        .setHeight(height)
                        .setVote(true).build());
                return new BbcDecData(true, true);
            }
            return null;

        });

    }




}
