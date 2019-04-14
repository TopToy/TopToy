package das.ab;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.google.protobuf.ByteString;
import das.data.Data;
import proto.Types;

import static das.bbc.BBC.addToBBCData;
import static das.ms.Membership.handleStartMsg;
import static java.lang.String.format;
import static servers.ToyBaseServer.addForkProof;
import static servers.ToyBaseServer.addToSyncData;

public class ABBftSMaRt extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ABBftSMaRt.class);
    private int id;
    private AsynchServiceProxy ABProxy;
    private ServiceReplica sr;
    private String configHome;
    private int n;
    private int f;
    private boolean stopped = false;

    public ABBftSMaRt(int id, int n, int f, String configHome) {
        this.id = id;
        this.n = n;
        this.f = f;
        this.configHome = configHome;
        logger.info(format("Initiated ABBftSMaRt: [id=%d; n=%d; f=%d]", id, n, f));

    }
    public void start() {
        sr = new ServiceReplica(id, this, this, configHome);
        ABProxy = new AsynchServiceProxy(id, configHome);
        logger.info("Start ABBftSMaRt");

    }

    public void shutdown() {
        ABProxy.close();
        sr.kill();
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
            Types.RBMsg msg = Types.RBMsg.parseFrom(command);
            if (msg == null) {
                logger.debug("Received NULL message!");
                return new byte[0];
            }

            switch (Data.getRBType(msg.getType())) {
                case FORK: addForkProof(msg, msg.getM().getChannel());
                    break;
                case SYNC: addToSyncData(msg, msg.getM().getChannel(), n, f);
                    break;
                case BBC: addToBBCData(msg, msg.getM().getChannel());
                    break;
                case START: handleStartMsg(msg);
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

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[1];
    }

    public void broadcast(byte[] m, Types.Meta key, Data.RBTypes t) {
        Types.RBMsg msg = Types.RBMsg.
                newBuilder().
                setM(key).
                setData(ByteString.copyFrom(m)).
                setType(t.ordinal()).
                build();
        byte[] data = msg.toByteArray();
        ABProxy.invokeAsynchRequest(data, new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {

            }
        }, TOMMessageType.ORDERED_REQUEST);
    }
}
