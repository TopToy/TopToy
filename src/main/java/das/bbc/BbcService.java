package das.bbc;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import das.data.GlobalData;
import das.data.VoteData;
import proto.Types.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.lang.String.format;

public class BbcService extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BbcService.class);
    private int id;
    private Object[] notes;
    private AsynchServiceProxy bbcProxy;
    private int quorumSize;
    private String configHome;
    private ServiceReplica sr;
    private boolean stopped = false;

    public BbcService(int channels, int id, int quorumSize, String configHome) {
        this.id = id;
        sr = null;
        this.quorumSize = quorumSize;
        this.configHome = configHome;
        notes = new Object[channels];
        for (int i = 0 ; i < channels ; i++) {
            notes[i] = new Object();
        }
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
            BbcMsg msg = BbcMsg.parseFrom(command);
            int channel = msg.getM().getChannel();
            int cidSeries = msg.getM().getCidSeries();
            int cid = msg.getM().getCid();
            Meta key = Meta.newBuilder()
                    .setCidSeries(cidSeries)
                    .setCid(cid)
                    .setChannel(channel)
                    .build();
            logger.debug(format("[#%d] has received bbc message from [sender=%d ; channel=%d ; cidSeries=%d ; cid=%d]",
                    id, msg.getM().getSender(), channel, cidSeries, cid));
            AtomicBoolean done = new AtomicBoolean(false);
            GlobalData.bbcDec.computeIfPresent(key, (k, v) -> {
                logger.debug(format("[#%d] already has a decision, re-participating consensus" +
                        "[channel=%d ; cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
                propose(v, channel, cidSeries, cid);
                done.set(true);
                return v;
            });

            if (done.get()) return new byte[0];

            GlobalData.votes.computeIfAbsent(key, k -> new VoteData());
            GlobalData.votes.computeIfPresent(key, (k, v) -> {
                if (v.getVotersNum() == quorumSize) return v;
                v.addVote(msg.getM().getSender(), msg.getVote());
                if (v.getVotersNum() == quorumSize) {
                    GlobalData.bbcDec.computeIfAbsent(key, k1 -> v.getVoteReasult());
                    logger.debug(format("[#%d] notifies on  [channel=%d ; cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
                    notes[channel].notify();
                }
                return v;
            });
        } catch (Exception e) {
            logger.error(format("[#%d]", id), e);
        }
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[1];
    }

    public boolean decide(int channel, int cidSeries, int cid) throws  InterruptedException {
        Meta key = Meta.newBuilder()
                .setCidSeries(cidSeries)
                .setCid(cid)
                .setChannel(channel)
                .build();
        synchronized (notes[channel]) {
            while (!GlobalData.bbcDec.containsKey(key)) {
                notes[channel].wait();
            }
        }
        return GlobalData.bbcDec.get(key);
    }

    public int propose(boolean vote, int channel, int cidSeries, int cid) {
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

}

