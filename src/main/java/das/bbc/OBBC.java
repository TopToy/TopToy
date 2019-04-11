package das.bbc;

import com.google.protobuf.ByteString;
import communication.CommLayer;
import config.Node;

import crypto.DigestMethod;
import crypto.blockDigSig;
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
    private static CommLayer comm;

    public OBBC(int id, int n, int f, int qSize,  ArrayList<Node> obbcCluster, CommLayer comm, String caRoot, String serverCrt,
                String serverPrivKey) {
        OBBC.id = id;
        rpcs = new OBBCRpcs(id, n, f, qSize, obbcCluster, caRoot, serverCrt, serverPrivKey);
        OBBC.comm = comm;
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
            Types.BlockHeader h = pending[worker].get(key);
            if (comm.contains(worker, h)) {
                vote = true;
            }

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

    static public Types.BbcMsg setFastBbcVote(Types.Meta key, int channel, int sender, int cidSeries, int cid, Types.BlockHeader next) {
        Types.BbcMsg.Builder bv = Types.BbcMsg
                .newBuilder()
                .setM(Types.Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .setSender(id)
                        .build())
                .setVote(false);

        Data.pending[channel].computeIfPresent(key, (k, val) -> {

            if (val.getM().getSender() != sender ||
                    val.getM().getCidSeries() != cidSeries ||
                    val.getM().getCid() != cid ||
                    val.getM().getChannel() != channel) {
                logger.debug(format("[s=%d:%d; w=%d:%d ; cidSeries=%d:%d ; cid=%d:%d]",sender, val.getM().getSender(), channel, val.getM().getChannel(), cidSeries, val.getM().getCidSeries(), cid, val.getM().getCid()));
                return val;
            }
            Types.BlockID bid = val.getBid();
            if (!comm.contains(channel, val)) {
                logger.debug(format("[#%d-C[%d]] comm does not contains the block itself (or it is invalid) [cidSeries=%d ; cid=%d ; bid=%d ; empty=%b]",
                        id, channel, val.getM().getCidSeries(), val.getM().getCid(), bid.getBid(), val.getEmpty()));
                return val;
            }
            bv.setVote(true);
            if (next != null) {
                logger.debug(format("[#%d-C[%d]] broadcasts [cidSeries=%d ; cid=%d] via fast mode",
                        id, channel, next.getM().getCidSeries(), next.getM().getCid()));
                bv.setNext(setFastModeData(val, next));
            }
            return val;
        });
        logger.debug(format("[#%d-C[%d]] sending fast vote [cidSeries=%d ; cid=%d ; val=%b]",
                id, channel, cidSeries, cid, bv.getVote()));
        return bv.build();
    }

    static private Types.BlockHeader setFastModeData(Types.BlockHeader curr, Types.BlockHeader next) {
        Types.BlockHeader.Builder nextBuilder = next
                .toBuilder()
                .setPrev(ByteString
                        .copyFrom(DigestMethod
                                .hash(curr.toByteArray())));
        String signature = blockDigSig.sign(next);
        return nextBuilder
                .setProof(signature)
                .build();
    }


}
