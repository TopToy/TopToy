package das.ab;

import das.data.Data;
import proto.Types;

import static java.lang.String.format;

public class ABService {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ABService.class);
    static private int id;
    private static ABBftSMaRt bs;
    public ABService(int id, int n, int f, String configHome) {
        ABService.id = id;
        ABService.bs = new ABBftSMaRt(id, n, f, configHome);
        logger.info(format("Initiated ABService: [id=%d]", id));

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

