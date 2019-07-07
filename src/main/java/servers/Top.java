package servers;

import blockchain.data.BCS;
import communication.CommLayer;
import communication.overlays.clique.Clique;
import proto.types.client;
import utils.Node;
import das.ab.ABService;
import das.bbc.BBC;
import das.bbc.OBBC;
import das.ms.Membership;
import das.wrb.WRB;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import utils.cache.TxCache;
import utils.DBUtils;
import utils.DiskUtils;
import proto.types.transaction.*;
import proto.types.block.*;

import java.io.IOException;
import java.util.*;
import static java.lang.String.format;

public class Top {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Top.class);
    private static CommLayer comm;
    private static int n;
    private static int f;
    private static ToyBaseServer[] toys;
    private static int id;
    private static int workers;
    private static String type;
    private static Server txsServer;
//    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
//    private EventLoopGroup gnio = new NioEventLoopGroup(2);


    public Top(int id, int n, int f, int workers, int tmo,
               int maxTx, ArrayList<Node> obbcCluster,
               ArrayList<Node> wrbCluster, ArrayList<Node> commCluster,
               String abConfig, String type, String serverCrt, String serverPrivKey, String caRoot) {
        Top.n = n;
        Top.f = f;
        Top.workers = workers;
        Top.toys = new ToyBaseServer[workers];
        Top.id = id;
        new BCS(id, n, f, workers);
        initProtocols(obbcCluster, wrbCluster, tmo, serverCrt,
                serverPrivKey, caRoot, commCluster, abConfig);

        Top.type = type;
        // TODO: Apply to more types
        if (type.equals("r")) {
            for (int i = 0 ; i < workers ; i++) {
                toys[i] = new ToyServer(id, i, n, f, maxTx, comm);
            }
        }
        if (type.equals("b")) {
            for (int i = 0 ; i < workers ; i++) {
                toys[i] = new ByzToyServer(id, i, n, f, maxTx, comm);
            }
        }
        if (type.equals("a")) {
            for (int i = 0 ; i < workers ; i++) {
                toys[i] = new AsyncToyServer(id, i, n, f, maxTx, comm);
            }
        }
        logger.info(format("Initiated TOP: [id=%d; n=%d; f=%d; workers=%d]", id, n, f, workers));

    }

    private static void initProtocols(ArrayList<Node> obbcCluster, ArrayList<Node> wrbCluster, int tmo,
                               String serverCrt, String serverPrivKey, String caRoot,
                               ArrayList<Node> commCluster, String abConfigHome) {
        comm = new Clique(id, workers, Top.n, commCluster);
        logger.info(format("[%d] has initiated communication layer", id));
        new ABService(id, n, f, abConfigHome);
        logger.info(format("[%d] has initiated ab service", id));
        new BBC(id, n, f, n - f);
        logger.info(format("[%d] has initiated BBC", id));
        new OBBC(id, n, f, workers, n - f, obbcCluster, comm, caRoot, serverCrt, serverPrivKey);
        logger.info(format("[%d] has initiated OBBC service", id));
        new WRB(id, workers, n, f, tmo, wrbCluster, serverCrt, serverPrivKey, caRoot);
        logger.info(format("[%d] has initiated WRB", id));
        new Membership(n);
        logger.info(format("[%d] has initiated Membership", id));
        new TxCache(0, workers);
        logger.info(format("[%d] has initiated Cache utils", id));
        new DBUtils(workers);
        logger.info(format("[%d] has initiated DB Utils", id));

    }


    public static void start() {
        logger.info("Starting OBBC");
        OBBC.start();
        logger.info("Starting wrb");
        WRB.start();
        logger.info("Joining to communication layer");
        comm.join();
        logger.info("Starting AB");
        ABService.start();
        if (Membership.start(id) == -1) {
            // Temporarily! Here we assume that everyone still correct
            logger.error("Error while trying to connect");
            shutdown();
            System.exit(0);
        }
        logger.info("Everything is up and good");

    }

    public static void shutdown() {
        ABService.shutdown();
        logger.info("shutdown AB");
        comm.leave();
        logger.info("leaving communication layer");
        WRB.shutdown();
        logger.info("shutdown WRB");
        OBBC.shutdown();
        logger.info("shutdown OBBC");
        txsServer.shutdown();
        logger.info("shutdown txsServer");
        for (int i = 0 ; i < workers ; i++) {
            toys[i].shutdown();
            logger.info(format("shutdown worker [%d]", i));
        }
        DBUtils.shutdown();
        logger.info("shutdown DBUtils");
        DiskUtils.shutdown();
        logger.info("shutdown DiskUtils");

        logger.info("ByeBye");
    }

    public static void serve() {
        try {
            for (int i = 0 ; i < workers ; i++) {
                toys[i].serve();
            }
        } catch (InterruptedException e) {
            logger.error(e);
            return;
        }

        try {
            txsServer = NettyServerBuilder
                    .forPort(9876)
//                    .executor(Executors.newFixedThreadPool(2))
//                    .bossEventLoopGroup(gnio)
//                    .workerEventLoopGroup(gnio)
                    .addService(new ClientRpcsService())
//                    .maxConcurrentCallsPerConnection(5) // TODO: Inspect this limit
                    .build()
                    .start();
            logger.info("starting client RPCs server, listening on port 9876");
        } catch (IOException e) {
            logger.error("", e);
        }
        logger.info("Start serving, everything looks good");
    }

    public static TxID addTransaction(Transaction tx) {
        int ps = toys[0].getTxPoolSize();
        int worker = 0;
        for (int i = 1 ; i < workers ; i++) {
            int cps = toys[i].getTxPoolSize();
            if (ps > cps) {
                ps = cps;
                worker = i;
            }
        }
        return toys[worker].addTransaction(tx);
    }

    public static TxID addTransaction(byte[] data, int clientID) {
       int ps = toys[0].getTxPoolSize();
       int worker = 0;
       for (int i = 1 ; i < workers ; i++) {
           int cps = toys[i].getTxPoolSize();
           if (ps > cps) {
               ps = cps;
               worker = i;
           }
       }
        return toys[worker].addTransaction(data, clientID);
    }

    public static client.TxState status(TxID tid, boolean blocking) throws InterruptedException {
        if (tid.getChannel() > workers) return client.TxState.UNKNOWN;
        return toys[tid.getChannel()].status(tid, blocking);
    }

    public static Transaction getTransaction(TxID txID, boolean blocking) throws InterruptedException {
        if (txID.getChannel() > workers) return Transaction.getDefaultInstance();
        return toys[txID.getChannel()].getTx(txID, blocking);
    }

    public static Block deliver(int index, boolean blocking) throws InterruptedException {
        int channel_num = index % workers;
        int block_num = index / workers;
        if (blocking) return toys[channel_num].deliver(block_num);
        return toys[channel_num].nonBlockingdeliver(block_num);
    }

    public static int getID() {
        return id;
    }

    public static void setByzSetting() {
        if (!type.equals("b")) {
            logger.debug("Unable to set byzantine behaviour to non byzantine node");
            return;
        }
        for (int i = 0 ; i < workers ; i++) {
            ((ByzToyServer) toys[i]).setByzSetting();
        }
    }

    public static void setAsyncParam(int maxTime) {
        if (!type.equals("a") && !type.equals("b")) {
            logger.debug("Unable to set async behaviour to non async node");
            return;
        }
        if (type.equals("a")) {
            for (int i = 0 ; i < workers ; i++) {
                ((AsyncToyServer) toys[i]).setAsyncParam(maxTime);
            }
        }

        if (type.equals("b")) {
            for (int i = 0 ; i < workers ; i++) {
                ((ByzToyServer) toys[i]).setAsyncParam(maxTime);
            }
        }

    }

    public static int poolSize() {
        int ps = 0;
        for (int i = 0 ; i < workers ; i++) {
            ps += toys[i].getTxPoolSize();
        }
        return ps;
    }

    public static int pendingSize() {
        int ps = 0;
        for (int i = 0; i < workers ; i++) {
            ps += toys[i].getPendingSize();
        }
        return ps;
    }

}
