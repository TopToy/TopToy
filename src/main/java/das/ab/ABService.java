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

import com.google.protobuf.InvalidProtocolBufferException;
import das.bbc.OBBC;
import das.data.BbcDecData;
import das.data.Data;
import das.data.VoteData;
import proto.Types;
import proto.Types.*;

import java.util.ArrayList;

import static java.lang.String.format;

public class ABService {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ABService.class);
    static private int id;
    static ABBftSMaRt bs;
    public ABService(int id, int n, int f, String configHome) {
        ABService.id = id;
        ABService.bs = new ABBftSMaRt(id, n, f, configHome);

    }


    static public void start() {
        bs.start();
        logger.debug("start ABService");
    }
    static public void shutdown() {
        bs.shutdown();
        logger.info(format("[#%d] shutting down ABService", id));
    }

    static public void broadcast(byte[] m, Types.Meta key, Data.RBTypes t) {
        bs.broadcast(m, key, t);
    }

}

