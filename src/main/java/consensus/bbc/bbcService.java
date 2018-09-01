package consensus.bbc;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import proto.Types.*;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.String.format;

public class bbcService extends DefaultSingleRecoverable {
    class consVote {
        int pos = 0;
        int neg = 0;
//        BbcDecision.Builder dec = BbcDecision.newBuilder();
        ArrayList<Integer> proposers = new ArrayList<>();
    }

    class fastVotePart {
        BbcDecision d;
        boolean done;
    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcService.class);

    private int id;
    final Object globalLock = new Object();
    private AsynchServiceProxy bbcProxy;

    private final HashMap<Meta, consVote> rec;

    private int quorumSize;
    private String configHome;
    private ServiceReplica sr;
    private boolean stopped = false;
    private HashMap<Meta, fastVotePart> fastVote = new HashMap<>();

    public bbcService(int id, int quorumSize, String configHome) {
        this.id = id;
        rec = new HashMap<>();
        sr = null;
        this.quorumSize = quorumSize;
        this.configHome = configHome;
    }

    public void start() {
        sr = new ServiceReplica(id, this, this, configHome);
        bbcProxy = new AsynchServiceProxy(id, configHome);
        logger.info(format("[#%d] bbc service is up", id));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (stopped) return;
         // Use stderr here since the logger may have been reset by its JVM shutdown hook.
         logger.warn(format("[#%d] shutting down bbc server since JVM is shutting down", id));
         shutdown();
        }));
    }
    public void shutdown() {
        stopped = true;
        releaseWaiting();
        if (bbcProxy != null) {
            bbcProxy.close();
            logger.debug(format("[#%d] shutting down bbc client", id));
        }
        if (sr != null) {
            sr.kill();
            logger.info(format("[#%d] shutting down bbc server", id));
            sr = null;
        }
        logger.debug(format("[#%d] shutting down bbc service", id));
    }

    private void releaseWaiting() {
        synchronized (globalLock) {
            globalLock.notifyAll();
        }
    }
    @Override
    public void installSnapshot(byte[] state) {
        logger.debug(format("[#%d] installSnapshot called", id));
    }

    @Override
    public byte[] getSnapshot() {
        logger.debug(format("[#%d] getSnapshot called", id));
        return new byte[1];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            synchronized (globalLock) {
            BbcMsg msg = BbcMsg.parseFrom(command);
            int channel = msg.getM().getChannel();
            int cidSeries = msg.getM().getCidSeries();
            int cid = msg.getM().getCid();
            Meta key = Meta.newBuilder()
                    .setCidSeries(cidSeries)
                    .setCid(cid)
                    .setChannel(channel)
                    .build();
//            logger.debug(format("[#%d] has received bbc message from [#%d]", id, msg.getPropserID()));
                if (fastVote.containsKey(key) && !fastVote.get(key).done) {
                    if (msg.getM().getSender() != id) {
                        logger.debug(format("[#%d] re-participating in a consensus " +
                                "[channel=%d ; cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
                        propose(fastVote.get(key).d.getDecosion(), channel, cidSeries, cid);
                        fastVote.get(key).done = true;
                    }
                }
                if (!rec.containsKey(key)) {
                    consVote v = new consVote();
//                    v.dec.setCid(cid);
//                    v.dec.setCidSeries(cidSeries);
                    rec.put(key, v);
                }
                consVote curr = rec.get(key);
                if (curr.proposers.contains(msg.getM().getSender())) return new byte[0];
                if (curr.neg +  curr.pos < quorumSize) {
                    curr.proposers.add(msg.getM().getSender());
                    if (msg.getVote() == 1) {
                        curr.pos++;
                    } else {
                        curr.neg++;
                    }
                    if (curr.neg + curr.pos == quorumSize) {
                        logger.debug(format("[#%d] notifies on  [channel=%d ; cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
                        globalLock.notifyAll();
                    }
                }

            }
        } catch (Exception e) {
            logger.error(format("[#%d]", id), e);
        }
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[1];
    }

    public BbcDecision decide(int channel, int cidSeries, int cid) throws InterruptedException {
        synchronized (globalLock) {
            Meta key = Meta.newBuilder()
                    .setCidSeries(cidSeries)
                    .setCid(cid)
                    .setChannel(channel)
                    .build();
            while (!rec.containsKey(key) || rec.get(key).pos + rec.get(key).neg < quorumSize) {
                globalLock.wait();
            }
            return BbcDecision.newBuilder()
                    .setM(Meta.newBuilder()
                            .setChannel(channel)
                            .setCidSeries(cidSeries)
                            .setCid(cid)
                            .build())
                    .setDecosion(rec.get(key).pos > rec.get(key).neg ? 1: 0)
                    .build();

        }
    }

    public int propose(int vote, int channel, int cidSeries, int cid) {
        BbcMsg msg= BbcMsg.newBuilder()
                .setM(Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .setSender(id)
                        .build())
                .setVote(vote)
                .build();
        byte[] data = msg.toByteArray();
        bbcProxy.invokeAsynchRequest(data, new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {

            }
        }, TOMMessageType.ORDERED_REQUEST);
        return 0;
    }


    public void updateFastVote(BbcDecision b) {
        synchronized (globalLock) {
            Meta key = Meta.newBuilder()
                    .setChannel(b.getM().getChannel())
                    .setCidSeries(b.getM().getCidSeries())
                    .setCid(b.getM().getCid())
                    .build();
            if (!fastVote.containsKey(key)) {
                fastVotePart fv = new fastVotePart();
                fv.d = b;
                fv.done = false;
                fastVote.put(key, fv);
            }
        }
    }

    public void periodicallyVoteMissingConsensus(BbcDecision b) {
        synchronized (globalLock) {
            Meta key = Meta.newBuilder()
                    .setChannel(b.getM().getChannel())
                    .setCidSeries(b.getM().getCidSeries())
                    .setCid(b.getM().getCid())
                    .build();
            if (rec.containsKey(key)) {
                if (!fastVote.containsKey(key)) {
                    fastVotePart fv = new fastVotePart();
                    fv.d = b;
                    fv.done = false;
                    fastVote.put(key, fv);
                }
                if (!fastVote.get(key).done) {
                    logger.debug(format("[#%d] re-participating in a consensus (periodicallyVoteMissingConsensus) " +
                            "[channel=%d ; cidSeries=%d ; cid=%d]", id, key.getChannel(), key.getCidSeries()
                            , key.getCid()));
                    propose(b.getDecosion(), key.getChannel(), key.getCidSeries(), key.getCid());
                    fastVote.get(key).done = true;
                }
            }
        }
    }

}

