package consensus.RBroadcast;

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
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import proto.RBrodcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

public class RBrodcastService extends DefaultSingleRecoverable {
        private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RBrodcastService.class);
        private int id;
        private List<RBrodcast.RBMsg> recMsg;
        private List<RBrodcast.RBMsg> delivered;
        private AsynchServiceProxy RBProxy;
        private ServiceReplica sr;
        private String configHome;
        final Object globalLock = new Object();
        private int cid = 0;

        public RBrodcastService(int id, String configHome) {
            this.id = id;
            this.configHome = configHome;
            sr = null;
            recMsg = new ArrayList<>();
            delivered = new ArrayList<>();
        }

        public void start() {
            sr = new ServiceReplica(id, this, this, configHome);
            RBProxy = new AsynchServiceProxy(id, configHome);


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                logger.warn(format("[#%d] shutting down RB server since JVM is shutting down", id));
                shutdown();

            }));
        }
        public void shutdown() {
            logger.info(format("[#%d] shutting down bbc server", id));
            if (RBProxy != null) {
                RBProxy.close();
                logger.info(format("[#%d] shut down bbc client successfully", id));
            }
            if (sr != null) {
                sr.kill();
                logger.info(format("[#%d] bbc server has been shutting down successfully", id));
                sr = null;
            }
        }

        @Override
        public void installSnapshot(byte[] state) {
            logger.info(format("[#%d] installSnapshot called", id));
        }

        @Override
        public byte[] getSnapshot() {
            logger.info(format("[#%d] getSnapshot called", id));
            return new byte[1];
        }

        @Override
        public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
            try {
                RBrodcast.RBMsg msg = RBrodcast.RBMsg.parseFrom(command);
                synchronized (globalLock) {
                    recMsg.add(msg);
                    recMsg.notify();
                }
            } catch (Exception e) {
                logger.error("", e);
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
                RBrodcast.RBMsg msg = recMsg.get(0);
                recMsg.remove(0);
                delivered.add(msg);
                return msg.getData().toByteArray();
            }
        }

    public int broadcast(byte[] m, int id) {
        RBrodcast.RBMsg msg = RBrodcast.RBMsg.
                newBuilder().
                setCid(cid).
                setId(id).
                setData(ByteString.copyFrom(m)).
                build();
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
        return 0;
    }

}

