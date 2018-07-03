package consensus.vpbc;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

public class vpbcServer extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(vpbcServer.class);

    int id;
    Validator v;
    public vpbcServer(int id, Validator v, String configHome) {
        this.id = id;
        this.v = v;
        new ServiceReplica(id, this, this, configHome);
    }

    @Override
    public void installSnapshot(byte[] state) {
        logger.info("installSnapshot currently not implemented");
    }

    @Override
    public byte[] getSnapshot() {
        logger.info("getSnapshot currently not implemented");
        return new byte[0];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        String data = new String(command);
        if (!v.validate(data))
            return null;
        logger.info(data + " from " + msgCtx.getSender());
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        logger.info("appExecuteUnordered currently not implemented");
        return new byte[0];
    }

}
