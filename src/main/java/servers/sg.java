package servers;

import blockchain.blockchain;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import proto.Types;
import rmf.ByzantineRmfNode;
import rmf.RmfNode;
import proto.blockchainServiceGrpc.blockchainServiceImplBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

public class sg implements server {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(sg.class);

    RmfNode rmf;
    RBrodcastService deliverFork;
    RBrodcastService sync;
    int n;
    int gcCount = 0;
    int gcLimit = 1;
    bcServer[] group;
    int[][] lastDelivered;
    int[] lastGCpoint;
    final blockchain bc;
    int id;
    int c;
    String type;
    int lastChannel = 0;
    boolean stopped = false;
    statistics sts = new statistics();
    Thread deliverThread = new Thread(() -> {
        try {
            deliverFromGroup();
        } catch (InterruptedException e) {
            logger.debug(format("G-%d interrupted while delivering from group", id));
        }
    });
    int maxTx;
    Server txsServer;
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);


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
            txsServer = ServerBuilder
                    .forPort(9876)
                    .executor(executor)
                    .addService(new txServer(this))
                    .build()
                    .start();
            logger.info("starting tx server");
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    private void deliverFromGroup() throws InterruptedException {
        int currChannel = 0;
        int currBlock = 0;
        while (!stopped) {
            for (currChannel = 0 ; currChannel < c ; currChannel++) {
                long start = System.currentTimeMillis();
//                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                Types.Block cBlock = group[currChannel].deliver(currBlock);
                gc(cBlock.getHeader().getHeight(), cBlock.getHeader().getM().getSender(), currChannel);
                if (cBlock.getDataCount() == 0) continue;
                cBlock = cBlock.toBuilder()
                        .setHeader(cBlock.getHeader().toBuilder()
                            .setHeight(bc.getHeight() + 1)
                            .setPrev(ByteString.copyFrom(
                                DigestMethod.hash(bc.getBlock(bc.getHeight()).getHeader().toByteArray())))
                            .build())
                            .setTs(System.currentTimeMillis())
                        .build();
                synchronized (bc) {
                    bc.addBlock(cBlock);
                    bc.notify();
                }
                updateStat(cBlock);
                logger.info(format("[#%d-C[%d]] deliver took about [%d] ms [height=%d], [cidSeries=%d ; cid=%d]",
                        getID(), currChannel, System.currentTimeMillis() - start, cBlock.getHeader().getHeight(),
                        cBlock.getHeader().getM().getCidSeries() ,cBlock.getHeader().getM().getCid()));
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
        logger.info(format("data count is %d, bc size is: %d, total: %d", b.getDataCount(), bc.getHeight(), sts.txCount));
        long bTs = b.getTs();
//        for (Types.Transaction t : b.getDataList()) {
//            long diff = bTs - Longs.fromByteArray(Arrays.copyOfRange(t.getData().toByteArray(), 0, 8));
//            logger.info(format("---%d---", diff));
//            sts.delaysSum += diff;
//        }
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
//        new Thread(() -> {
//            for (int i = 0 ; i < c ; i++) {
//                gcForChannel(i);
//            }
//        }).start();


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
        stopped = true;
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
        txsServer.shutdownNow();

    }

    public void serve() {
        for (int i = 0 ; i < c ; i++) {
            group[i].serve();
        }
        deliverThread.start();
    }

    public String addTransaction(byte[] data, int clientID) {
        String ret = null;
        try {
            ret = group[lastChannel].addTransaction(data, clientID);
        } catch (InterruptedException e) {
            logger.error("", e);
            return ret;
        }
        lastChannel = (lastChannel + 1) % c;
        return ret;
    }

    public int isTxPresent(String txID) {
        for (int i = 0 ; i < c ; i++) {
            int ret = group[i].isTxPresent(txID);
            if (ret != -1) return ret;
        }
        return -1;
    }

    public Types.Block deliver(int index) throws InterruptedException {
        synchronized (bc) {
            while (bc.getHeight() < index) {
                bc.wait();
            }
            return bc.getBlock(index);
        }
    }

    public Types.Block nonBlockingDeliver(int index) {
        if (bc.getHeight() < index) return null;
        return bc.getBlock(index);
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
        server.addTransaction(request.getData().toByteArray(), request.getClientID());
        responseObserver.onNext(Types.accepted.newBuilder().build());
        responseObserver.onCompleted();
    }
}