package servers;

import blockchain.blockchain;
import com.google.protobuf.ByteString;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import proto.Types;
import rmf.ByzantineRmfNode;
import rmf.RmfNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;

public class sg implements server {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(sg.class);
    RmfNode rmf;
    RBrodcastService deliverFork;
    RBrodcastService sync;

    bcServer[] group;

    final blockchain bc;
    int id;
    int c;
    String type;
    int lastChannel = 0;
    boolean stopped = false;
    Thread deliverThread = new Thread(() -> {
        try {
            deliverFromGroup();
        } catch (InterruptedException e) {
            logger.debug(format("G-%d interrupted while delivering from group", id));
        }
    });


    public sg(String addr, int port, int id, int f, int c, int tmo, int tmoInterval,
              int maxTx, boolean fastMode, ArrayList<Node> cluster, String bbcConfig, String panicConfig,
              String syncConfig, String type,  String serverCrt, String serverPrivKey, String caRoot) {
        this.c = c;
        this.group = new bcServer[c];
        this.id = id;
        if (type.equals("r") || type.equals("a")) {
            rmf = new RmfNode(c, id, addr, port, f, cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        }
        if (type.equals("b")) {
            rmf = new ByzantineRmfNode(c, id, addr, port, f, cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
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
    }

    private void deliverFromGroup() throws InterruptedException {
        int currChannel = 0;
        int currBlock = 0;
        while (!stopped) {
            for (currChannel = 0 ; currChannel < c ; currChannel++) {
                long start = System.currentTimeMillis();
//                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                Types.Block cBlock = group[currChannel].deliver(currBlock);
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
                logger.info(format("[#%d-C[%d]] deliver took about [%d] ms [height=%d], [cidSeries=%d ; cid=%d]",
                        getID(), currChannel, System.currentTimeMillis() - start, cBlock.getHeader().getHeight(),
                        cBlock.getHeader().getM().getCidSeries() ,cBlock.getHeader().getM().getCid()));
            }
            currBlock++;
        }
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
        logger.debug(format("G-%d shutdown rmf Service", id));
        deliverFork.shutdown();
        logger.debug(format("G-%d shutdown panic service", id));
        sync.shutdown();
        logger.debug(format("G-%d shutdown sync service", id));

    }

    public void serve() {
        for (int i = 0 ; i < c ; i++) {
            group[i].serve();
        }
        deliverThread.start();
    }

    public String addTransaction(byte[] data, int clientID) {
        String ret = group[lastChannel].addTransaction(data, clientID);
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
