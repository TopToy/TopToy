package servers;

import blockchain.blockchain;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import proto.Types;
import rmf.ByzantineRmfNode;
import rmf.RmfNode;
import proto.blockchainServiceGrpc.blockchainServiceImplBase;
import utils.chainCutter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

public class sg implements server {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(sg.class);

    private RmfNode rmf;
    boolean up = false;
    HashMap<String, Integer> txMap = new HashMap<>();
    private RBrodcastService deliverFork;
    private RBrodcastService sync;
    private int n;
    private int gcCount = 0;
    private int gcLimit = 1;
    private bcServer[] group;
    private int[][] lastDelivered;
    private int[] lastGCpoint;
    private final blockchain bc;
    private int id;
    private int c;
    private String type;
    int lastChannel = 0;
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private statistics sts = new statistics();
    private Thread deliverThread = new Thread(() -> {
        try {
            deliverFromGroup();
        } catch (InterruptedException e) {
            logger.debug(format("G-%d interrupted while delivering from group", id));
        }
    });
    private int maxTx;
    private int cutterBatch = Config.getCutterBatch();
    Path cutterDirName = Paths.get(System.getProperty("user.dir"), "blocks");
    private Server txsServer;
//    private ExecutorService chainCutterExecutor =  Executors.newSingleThreadExecutor();
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    private EventLoopGroup gnio = new NioEventLoopGroup(2);


    public sg(String addr, int port, int id, int f, int c, int tmo, int tmoInterval,
              int maxTx, boolean fastMode, ArrayList<Node> cluster, String bbcConfig, String panicConfig,
              String syncConfig, String type,  String serverCrt, String serverPrivKey, String caRoot) {
        n = 3 *f +1;
        this.maxTx = maxTx;
        lastDelivered = new int[c][];
        lastGCpoint = new int[c];
        for (int i = 0 ; i < c ; i++) {
            lastDelivered[i] = new int[n];
            lastGCpoint[i] = 1;
            for (int j = 0 ; j < n ; j++) {
                lastDelivered[i][j] = 0;

            }
        }
        this.c = c;
        this.group = new bcServer[c];
        this.id = id;
        if (type.equals("r") || type.equals("a")) {
            rmf = new RmfNode(c, id, addr, port, f, tmo, tmoInterval, cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        }
        if (type.equals("b")) {
            rmf = new ByzantineRmfNode(c, id, addr, port, f, tmo, tmoInterval, cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        }
        deliverFork = new RBrodcastService(c, id, panicConfig);
        sync = new RBrodcastService(c, id, syncConfig);
        this.type = type;
        // TODO: Apply to more types
        if (type.equals("r")) {
            for (int i = 0 ; i < c ; i++) {
                group[i] = new cbcServer(addr, port, id, i, f, tmo, tmoInterval, maxTx,
                        fastMode, cluster, rmf, deliverFork, sync);
            }
        }
        if (type.equals("b")) {
            for (int i = 0 ; i < c ; i++) {
                group[i] = new byzantineBcServer(addr, port, id, i, f, tmo, tmoInterval, maxTx,
                        fastMode, cluster, rmf, deliverFork, sync);
            }
        }
        if (type.equals("a")) {
            for (int i = 0 ; i < c ; i++) {
                group[i] = new asyncBcServer(addr, port, id, i, f, tmo, tmoInterval, maxTx,
                        fastMode, cluster, rmf, deliverFork, sync);
            }
        }
        bc = group[0].initBC(id, -1);
        try {
            txsServer = NettyServerBuilder
                    .forPort(9876)
                    .executor(executor)
                    .bossEventLoopGroup(gnio)
                    .workerEventLoopGroup(gnio)
                    .addService(new txServer(this))
                    .build()
                    .start();
            logger.info("starting tx server");
        } catch (IOException e) {
            logger.error("", e);
        }
        new chainCutter(cutterDirName);
        logger.info(format("cutter batch is %d", cutterBatch));
    }

    private void deliverFromGroup() throws InterruptedException {
        int currChannel = 0;
        int currBlock = 0;
        while (!stopped.get()) {
            for (currChannel = 0 ; currChannel < c ; currChannel++) {
                long start = System.currentTimeMillis();
                logger.debug(format("Trying to deliver from [channel=%d, channelBlock=%d]", currChannel, currBlock));
//                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                Types.Block cBlock = group[currChannel].deliver(currBlock);
                sts.all++;
                sts.deliveredTime += System.currentTimeMillis() - start;
                gc(cBlock.getHeader().getHeight(), cBlock.getHeader().getM().getSender(), currChannel);
                if (cBlock.getDataCount() == 0) {
                    sts.eb++;
                    logger.info(format("E - [[time=%d], [height=%d], [sender=%d], [channel=%d], [size=0]]",
                            System.currentTimeMillis() - start, cBlock.getHeader().getHeight(),
                            cBlock.getHeader().getM().getSender(), cBlock.getHeader().getM().getChannel()));
                    continue;
                }
                cBlock = cBlock.toBuilder()
                        .setHeader(cBlock.getHeader().toBuilder()
                            .setHeight(bc.getHeight() + 1)
                            .setPrev(ByteString.copyFrom(
                                DigestMethod.hash(bc.getBlock(bc.getHeight()).getHeader().toByteArray())))
//                                .setPresent(true)
                            .build())
                            .setTs(System.currentTimeMillis())
                        .build();
                synchronized (bc) {
                    bc.addBlock(cBlock);
                    bc.notify();
                }
                updateStat(cBlock);
//                if (bc.getHeight() % cutterBatch == 0) {
//                    chainCutterExecutor.execute(() -> {
//                        try {
////                            logger.info("Writing blocks...");
//                            chainCutter.cut(bc, bc.getHeight() - cutterBatch, bc.getHeight());
//                        } catch (IOException e) {
//                            logger.error("", e);
//                        }
//                    });
//                }
                logger.info(format("F - [[time=%d], [height=%d], [sender=%d], [channel=%d], [size=%d]]",
                        System.currentTimeMillis() - start, cBlock.getHeader().getHeight(),
                        cBlock.getHeader().getM().getSender(), cBlock.getHeader().getM().getChannel(), cBlock.getDataCount()));
            }

            currBlock++;
        }
    }

    void updateStat(Types.Block b) {
        if (b.getHeader().getHeight() == 1) {
            sts.firstTxTs = b.getTs();
            sts.txSize = b.getData(0).getSerializedSize();
        }
        sts.lastTxTs = max(sts.lastTxTs, b.getTs());
        sts.txCount += b.getDataCount();
//        logger.info(format("data count is %d, bc size is: %d, total: %d", b.getDataCount(), bc.getHeight(), sts.txCount));
        long bTs = b.getTs();
        for (Types.Transaction t : b.getDataList()) {
            txMap.put(t.getTxID(), b.getHeader().getHeight());
//            long diff = bTs - Longs.fromByteArray(Arrays.copyOfRange(t.getData().toByteArray(), 0, 8));
//            logger.info(format("diff is: [%d]", diff));
//            sts.delaysSum += diff;
        }
        synchronized (txMap) {
            txMap.notifyAll();
        }
    }

    public statistics getStatistics() {
        sts.totalDec = rmf.getTotolDec();
        sts.optemisticDec = rmf.getOptemisticDec();
        return sts;
    }

    void gc(int origHeight, int sender, int channel) {
        if (sender < 0 || sender > n - 1 || channel < 0 || channel > c -1 || origHeight < 0) {
            logger.debug(format("G-%d GC invalid argument [OrigHeight=%d ; sender=%d ; channel=%d]"
                    ,id, origHeight, sender, channel));
            return;
        }
        lastDelivered[channel][sender] = origHeight;
        gcCount++;
        if (gcCount < gcLimit) return;
        gcCount = 0;
        for (int i = 0 ; i < c ; i++) {
            gcForChannel(i);
        }
    }
    void gcForChannel(int channel) {
        int minHeight = lastDelivered[channel][0];
        for (int i = 0 ; i < n ; i++) {
            minHeight = min(minHeight, lastDelivered[channel][i]);
        }
        logger.debug(format("G-%d starting GC [OrigHeight=%d ; lastGCPoint=%d ;" +
                " channel=%d]",id, minHeight, lastGCpoint[channel], channel));
        for (int i = lastGCpoint[channel] ; i < minHeight ; i++) {
            group[channel].gc(i);
        }
        lastGCpoint[channel] = minHeight;

    }
    public void start() {
        CountDownLatch latch = new CountDownLatch(3);
        new Thread(() -> {
            this.rmf.start();
            latch.countDown();
        }).run();
        new Thread(() -> {
            this.deliverFork.start();
            latch.countDown();
        }).run();
        new Thread(() -> {
            this.sync.start();
            latch.countDown();
        }).run();
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
            shutdown();
            return;
        }
        for (int i = 0 ; i < c ; i++) {
            group[i].start(true);
        }
    }

    public void shutdown() {
        stopped.set(true);
        deliverThread.interrupt();
        try {
            deliverThread.join();
        } catch (InterruptedException e) {
            logger.error(format("G-%d", id), e);
        }
        for (int i = 0 ; i < c ; i++) {
            group[i].shutdown(true);
            logger.debug(format("G-%d shutdown channel %d", id, i));
        }
        logger.debug(format("G-%d shutdown deliverThread", id));
        rmf.stop();
//        sts.totalDec = rmf.getTotolDec();
//        sts.optemisticDec = rmf.getOptemisticDec();
        logger.debug(format("G-%d shutdown rmf Service", id));
        deliverFork.shutdown();
        logger.debug(format("G-%d shutdown panic service", id));
        sync.shutdown();
        logger.debug(format("G-%d shutdown sync service", id));
        txsServer.shutdown();
//        chainCutterExecutor.shutdown();

    }

    public void serve() {
        for (int i = 0 ; i < c ; i++) {
            group[i].serve();
        }
        deliverThread.start();
        up = true;
    }

//    public String addTransaction(byte[] data, int clientID) {
//        String ret = null;
////        for (int i = 0 ; i < c ; i++) {
//            try {
//                ret = group[lastChannel].addTransaction(data, clientID);
//                lastChannel = (lastChannel + 1) % c;
////                if (!ret.equals("")) {
////                    return ret;
////                }
//            } catch (InterruptedException e) {
//                logger.error("", e);
//                return ret;
//            }
//
////        }
//        return ret;
//    }


    public String addTransaction(Types.Transaction tx) {
//        if (!up) return "";
        int ps = group[0].getTxPoolSize();
        int chan = 0;
        for (int i = 1 ; i < c ; i++) {
            int cps = group[i].getTxPoolSize();
            if (ps > cps) {
                ps = cps;
                chan = i;
            }
        }
        Types.Transaction ntx = Types.Transaction.newBuilder()
                .setClientID(tx.getClientID())
                .setData(tx.getData())
                .setServerTs(System.currentTimeMillis())
                .setTxID(UUID.randomUUID().toString())
                .setClientTs(tx.getClientTs())
                .build();
        return group[chan].addTransaction(ntx);
    }

    public String addTransaction(byte[] data, int clientID) {
//        if (!up) return "";
       int ps = group[0].getTxPoolSize();
       int chan = 0;
       for (int i = 1 ; i < c ; i++) {
           int cps = group[i].getTxPoolSize();
           if (ps > cps) {
               ps = cps;
               chan = i;
           }
       }
        return group[chan].addTransaction(data, clientID);
    }

    public int isTxPresent(String txID) {
        for (int i = 0 ; i < c ; i++) {
            int ret = group[i].isTxPresent(txID);
            if (ret != -1) return ret;
        }
        return -1;
    }

    Types.approved getTransaction(String txID) throws InterruptedException {
        Types.Block b = null;
        synchronized (txMap) {
            while (!txMap.containsKey(txID)) {
                txMap.wait();
            }
        }
        if (txMap.containsKey(txID)) {
            b = nonBlockingDeliver(txMap.get(txID));
        }
        if (b == null) return Types.approved.getDefaultInstance();
        for (Types.Transaction t : b.getDataList()) {
            if (t.getTxID().equals(txID)) {
                return Types.approved.newBuilder().setTs(b.getTs()).setTx(t).build();
            }
        }
        return Types.approved.getDefaultInstance();
    }

    public Types.Block deliver(int index) throws InterruptedException {
        synchronized (bc) {
            while (bc.getHeight() < index) {
                bc.wait();
            }
            Types.Block b = bc.getBlock(index);
            if (b.getDataCount() > 0 || b.getHeader().getHeight() == 0) {
                return b;
            }
            try {
                return chainCutter.getBlockFromFile(b.getHeader().getHeight());
            } catch (IOException e) {
                logger.error("", e);
                return null;
            }
        }
    }

    public Types.Block nonBlockingDeliver(int index) {
        if (bc.getHeight() < index) return null;
        Types.Block b = bc.getBlock(index);
        if (b.getDataCount() > 0 || b.getHeader().getHeight() == 0) {
            return b;
        }

        try {
            return chainCutter.getBlockFromFile(b.getHeader().getHeight());
        } catch (IOException e) {
            logger.error("", e);
            return null;
        }
    }

    public int getID() {
        return id;
    }

    public void setByzSetting(boolean fullByz, List<List<Integer>> groups) {
        if (!type.equals("b")) {
            logger.debug(format("G-%d Unable to set byzantine behaviour to non byzantine node", id));
            return;
        }
        for (int i = 0 ; i < c ; i++) {
            ((byzantineBcServer) group[i]).setByzSetting(fullByz, groups);
        }
    }

    public void setAsyncParam(int maxTime) {
        if (!type.equals("a")) {
            logger.debug(format("G-%d Unable to set async behaviour to non async node", id));
            return;
        }
        for (int i = 0 ; i < c ; i++) {
            ((asyncBcServer) group[i]).setAsyncParam(maxTime);
        }
    }

    public int getBCSize() {
        return bc.getHeight() + 1;
    }

}
class txServer extends blockchainServiceImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(txServer.class);
    sg server;

    public txServer(sg server) {
        super();
        this.server = server;
    }
    @Override
    public void addTransaction(Types.Transaction request, StreamObserver<Types.accepted> responseObserver) {
//        logger.info("add tx...");
//        logger.info("receive write request");
        boolean ac = true;
        String txID = server.addTransaction(request);
        if (txID.equals("")) ac = false;
        responseObserver.onNext(Types.accepted.newBuilder().setTxID(txID).setAccepted(ac).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTransaction(Types.read request, StreamObserver<Types.approved> responseObserver) {
//        logger.info("receive read request");
        try {
            responseObserver.onNext(server.getTransaction(request.getTxID()));
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        responseObserver.onCompleted();
    }
}