package das.RBroadcast;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.google.protobuf.ByteString;

import com.google.protobuf.InvalidProtocolBufferException;
import das.data.Data;
import proto.Types.*;

import java.util.ArrayList;

import static java.lang.String.format;

public class RBrodcastService extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RBrodcastService.class);
    private int id;
    private AsynchServiceProxy RBProxy;
    private ServiceReplica sr;
    private String configHome;
    private int n;
    private int f;
    private boolean stopped = false;

    public RBrodcastService(int id, int n, int f, String configHome) {
        this.id = id;
        this.configHome = configHome;
        sr = null;
        this.n = n;
        this.f = f;
    }


    public void start() {
        sr = new ServiceReplica(id, this, this, configHome);
        RBProxy = new AsynchServiceProxy(id, configHome);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (stopped) return;
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.warn(format("[#%d] shutting down RB server since JVM is shutting down", id));
            shutdown();

        }));
    }
    public void shutdown() {
        stopped = true;
        if (RBProxy != null) {
            RBProxy.close();
            logger.debug(format("[#%d] shut down rb client", id));
        }
        if (sr != null) {
            sr.kill();
            logger.debug(format("[#%d] shutting sown rb server", id));
            sr = null;
        }
        logger.info(format("[#%d] shutting down rb service", id));
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
            RBMsg msg = RBMsg.parseFrom(command);
            if (msg == null) {
                logger.debug("Received NULL message!");
                return new byte[0];
            }

            switch (Data.getRBType(msg.getType())) {
                case FORK: addForkProof(msg, msg.getM().getChannel());
                    break;
                case SYNC: addToSyncData(msg, msg.getM().getChannel());
                    break;
                case NOT_MAPPED:
                    logger.error("Invalid type for RB message");
                    return new byte[0];
            }
        } catch (Exception e) {
            logger.error(format("[#%d]", id), e);
        }

        return new byte[0];
    }

    private void addForkProof(RBMsg msg, int channel) throws InvalidProtocolBufferException {
        logger.debug(format("received FORK message on channel [%d]", channel));
        ForkProof p = ForkProof.parseFrom(msg.getData());
        if (!Data.validateForkProof(p)) return;
        synchronized (Data.forksRBData[channel]) {
            Data.forksRBData[channel].add(p);
            Data.forksRBData[channel].notifyAll();
        }
    }

    private void addToSyncData(RBMsg msg, int channel) throws InvalidProtocolBufferException {
        logger.debug(format("received SYNC message on channel [%d]", channel));
        subChainVersion sbv = subChainVersion.parseFrom(msg.getData());
        int fp = sbv.getForkPoint();
        synchronized (Data.syncRBData[channel]) {
            if (Data.syncRBData[channel].containsKey(fp)
                    && Data.syncRBData[channel].get(fp).size() == n - f) return;
        }

        synchronized (Data.syncRBData[channel]) {
            if (!Data.validateSubChainVersion(sbv, f)) return;
            Data.syncRBData[channel].computeIfAbsent(fp, k -> new ArrayList<>());
            Data.syncRBData[channel].computeIfPresent(fp, (k, v) -> {
                v.add(sbv);
                return v;
            });
            if (Data.syncRBData[channel].get(fp).size() == n - f) {
                Data.syncRBData[channel].notifyAll();
            }
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[1];
    }

    public void broadcast(byte[] m, Meta key, Data.RBTypes t) {
        RBMsg msg = RBMsg.
                newBuilder().
                setM(key).
                setData(ByteString.copyFrom(m)).
                setType(t.ordinal()).
                build();
        byte[] data = msg.toByteArray();
        RBProxy.invokeAsynchRequest(data, new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {

            }
        }, TOMMessageType.ORDERED_REQUEST);
    }

}

