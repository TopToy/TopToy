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
import das.data.GlobalData;
import proto.Types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

            Meta key = Meta.newBuilder()
                    .setCidSeries(msg.getM().getCidSeries())
                    .setCid(msg.getM().getCidSeries())
                    .setChannel(msg.getM().getChannel())
                    .build();
            switch (GlobalData.getRBType(msg.getType())) {
                case FORK: addForkProof(msg, key);
                    break;
                case SYNC: addToSyncData(msg, key);
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

    private void addForkProof(RBMsg msg, Meta key) throws InvalidProtocolBufferException {
        int channel = key.getChannel();
        synchronized (GlobalData.forksRBData[channel]) {
            if (GlobalData.forksRBData[channel].containsKey(key)) return;
            ForkProof p = ForkProof.parseFrom(msg.getData());
            if (!GlobalData.validateForkProof(p)) return;
            GlobalData.forksRBData[channel].put(key, p);
            GlobalData.forksRBData[channel].notifyAll();
        }
    }

    private void addToSyncData(RBMsg msg, Meta key) throws InvalidProtocolBufferException {
        int channel = key.getChannel();
        synchronized (GlobalData.syncRBData[channel]) {
            if (GlobalData.syncRBData[channel].containsKey(key)
                    && GlobalData.syncRBData[channel].get(key).size() == n - f) return;
        }
        subChainVersion sbv = subChainVersion.parseFrom(msg.getData());
        synchronized (GlobalData.syncRBData[channel]) {
            if (!GlobalData.validateSubChainVersion(sbv, f)) return;
            GlobalData.syncRBData[channel].computeIfAbsent(key, k -> new ArrayList<>());
            GlobalData.syncRBData[channel].computeIfPresent(key, (k, v) -> {
                v.add(sbv);
                return v;
            });
            if (GlobalData.syncRBData[channel].get(key).size() == n - f) {
                GlobalData.syncRBData[channel].notifyAll();
            }
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[1];
    }

    public void broadcast(byte[] m, Meta key) {
        RBMsg msg = RBMsg.
                newBuilder().
                setM(key).
                setData(ByteString.copyFrom(m)).
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

