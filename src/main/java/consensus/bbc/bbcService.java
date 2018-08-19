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

import static java.lang.String.format;

/*
    TODO:
    1. Truncate the executed consensuses (or swap them to db)
 */
public class bbcService extends DefaultSingleRecoverable {
    class consVote {
        int pos = 0;
        int neg = 0;
        BbcDecision.Builder dec = BbcDecision.newBuilder();
    }

    class fastVotePart {
        BbcDecision d;
        boolean done;
    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcService.class);

    private int id;
    final Object globalLock = new Object();
    private AsynchServiceProxy bbcProxy;
    private final Table<Integer, Integer, consVote> rec;
    private int quorumSize;
    private String configHome;
    private ServiceReplica sr;
    private Table<Integer, Integer, fastVotePart> fastVote = HashBasedTable.create();
    public bbcService(int id, int quorumSize, String configHome) {
        this.id = id;
        rec = HashBasedTable.create();
        sr = null;
        this.quorumSize = quorumSize;
        this.configHome = configHome;
    }

    public void start() {
        sr = new ServiceReplica(id, this, this, configHome);
        bbcProxy = new AsynchServiceProxy(id, configHome);
        logger.info(format("[#%d] bbc service is up", id));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         // Use stderr here since the logger may have been reset by its JVM shutdown hook.
         logger.warn(format("[#%d] shutting down bbc server since JVM is shutting down", id));
         shutdown();
        }));
    }
    public void shutdown() {
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
        int cid;
        try {
            synchronized (globalLock) {
            BbcMsg msg = BbcMsg.parseFrom(command);
            cid = msg.getCid();
            int cidSeries = msg.getCidSeries();
            logger.debug(format("[#%d] has received bbc message from [#%d]", id, msg.getPropserID()));
                if (fastVote.contains(cidSeries, cid) && !fastVote.get(cidSeries, cid).done) {
                    if (msg.getPropserID() != id) {
                        logger.debug(format("[#%d] re-participating in a consensus [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
                        propose(fastVote.get(cidSeries, cid).d.getDecosion(), cidSeries, cid);
                        fastVote.get(cidSeries, cid).done = true;
                    }
                }
                if (!rec.contains(cidSeries, cid)) {
                    consVote v = new consVote();
                    v.dec.setCid(cid);
                    v.dec.setCidSeries(cidSeries);
                    rec.put(cidSeries, cid, v);
                }
                consVote curr = rec.get(cidSeries, cid);
                if (curr.neg +  curr.pos < quorumSize) {
                    if (msg.getVote() == 1) {
                        curr.pos++;
                    } else {
                        curr.neg++;
                    }
                }
                if (curr.neg + curr.pos == quorumSize) {
                    logger.debug(format("[#%d] notifies on  [cid=%d]", id, cid));
                    globalLock.notify();
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

    public BbcDecision decide(int cidSeries, int cid) throws InterruptedException {
        synchronized (globalLock) {
            while (!rec.contains(cidSeries, cid) || rec.get(cidSeries, cid).pos + rec.get(cidSeries, cid).neg < quorumSize) {
                globalLock.wait();
            }
            return (rec.get(cidSeries, cid).pos > rec.get(cidSeries, cid).neg ?
                    rec.get(cidSeries, cid).dec.setDecosion(1).build() : rec.get(cidSeries, cid).dec.setDecosion(0).build());
        }
    }

    public int propose(int vote, int cidSeries, int cid) {
        BbcMsg.Builder b = BbcMsg.newBuilder();
        b.setPropserID(id);
        b.setCid(cid);
        b.setCidSeries(cidSeries);
        b.setVote(vote);
        BbcMsg msg= b.build();
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
            int fcid = b.getCid();
            int fcidSeries = b.getCidSeries();
            if (!fastVote.contains(fcidSeries, fcid)) {
                fastVotePart fv = new fastVotePart();
                fv.d = b;
                fv.done = false;
                fastVote.put(fcidSeries, fcid, fv);
            }
        }
    }

    public void periodicallyVoteMissingConsensus(BbcDecision b) {
        synchronized (globalLock) {
            int fcid = b.getCid();
            int fcidSeries = b.getCidSeries();
            if (rec.contains(fcidSeries, fcid)) {
                if (!fastVote.contains(fcidSeries, fcid)) {
                    fastVotePart fv = new fastVotePart();
                    fv.d = b;
                    fv.done = false;
                    fastVote.put(fcidSeries, fcid, fv);
                }
                if (!fastVote.get(fcidSeries, fcid).done) {
                    logger.debug(format("[#%d] re-participating in a consensus (periodicallyVoteMissingConsensus) " +
                            "[cidSeries=%d ; cid=%d]", id, fcidSeries, fcid));
                    propose(b.getDecosion(), fcidSeries, fcid);
                    fastVote.get(fcidSeries, fcid).done = true;
                }
            }
        }
    }

}

