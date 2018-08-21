package consensus.RBroadcast;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.google.protobuf.ByteString;

import proto.Types.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class RBrodcastService extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RBrodcastService.class);
    private int id;
    private List<RBMsg> recMsg;
    private AsynchServiceProxy RBProxy;
    private ServiceReplica sr;
    private String configHome;
    private final Object globalLock = new Object();
    private int cid = 0;
    private boolean stopped = false;

    public RBrodcastService(int id, String configHome) {
        this.id = id;
        this.configHome = configHome;
        sr = null;
        recMsg = new ArrayList<>();
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
        releaseWaiting();
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
            RBMsg msg = RBMsg.parseFrom(command);
            synchronized (globalLock) {
                recMsg.add(msg);
                globalLock.notify();
            }
        } catch (Exception e) {
            logger.error(format("[#%d]"), e);
        }

        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[1];
    }

    public byte[] deliver() throws InterruptedException {
        synchronized (globalLock) {
            while (recMsg.isEmpty()) {
                globalLock.wait();
            }
            RBMsg msg = recMsg.get(0);
            recMsg.remove(0);
            return msg.getData().toByteArray();
        }
    }

    public void notifyOnNewBlock() throws InterruptedException {
        synchronized (globalLock) {
            while (recMsg.isEmpty()) {
                globalLock.wait();
            }
        }
    }

    public byte[] nonBlockingDeliver() {
        synchronized (globalLock) {
            if(recMsg.isEmpty()) {
                return null;
            }
            RBMsg msg = recMsg.get(0);
            recMsg.remove(0);
            return msg.getData().toByteArray();
        }
    }


    public int broadcast(byte[] m, int id) {
        RBMsg msg = RBMsg.
                newBuilder().
                setCid(cid).
                setId(id).
                setData(ByteString.copyFrom(m)).
                build();
        int ret = cid;
        cid++;
        byte[] data = msg.toByteArray();
        RBProxy.invokeAsynchRequest(data, new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {

            }
        }, TOMMessageType.ORDERED_REQUEST);
        return ret;
    }

}

