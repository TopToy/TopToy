package serverGroup;

import blockchain.bcServer;
import blockchain.byzantineBcServer;
import blockchain.asyncBcServer;
import blockchain.blockchain;
import blockchain.cbcServer;
import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import consensus.bbc.bbcService;
import crypto.DigestMethod;
import proto.Types;
import rmf.ByzantineRmfNode;
import rmf.RmfNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;

public class sg {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(sg.class);
    RmfNode rmf;
    RBrodcastService deliverFork;
    RBrodcastService sync;

    bcServer[] group;

    final blockchain bc;
    int id;
    int g;
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


    public sg(String addr, int port, int id, int f, int g, int tmo, int tmoInterval,
              int maxTx, boolean fastMode, ArrayList<Node> cluster, String bbcConfig, String panicConfig,
              String syncConfig, String type) {
        this.group = new bcServer[g];
        this.id = id;
        this.g = g;
        if (type.equals("r") || type.equals("a")) {
            rmf = new RmfNode(g, id, addr, port, f, cluster, bbcConfig);
        }
        if (type.equals("b")) {
            rmf = new ByzantineRmfNode(g, id, addr, port, f, cluster, bbcConfig);
        }
        deliverFork = new RBrodcastService(id, panicConfig);
        sync = new RBrodcastService(id, syncConfig);
        this.type = type;
        // TODO: Apply to more types
        if (type.equals("r")) {
            for (int i = 0 ; i < g ; i++) {
                group[i] = new cbcServer(addr, port, id, i, f, tmo, tmoInterval, maxTx,
                        fastMode, cluster, rmf, deliverFork, sync);
            }
        }
        if (type.equals("b")) {
            for (int i = 0 ; i < g ; i++) {
                group[i] = new byzantineBcServer(addr, port, id, i, f, tmo, tmoInterval, maxTx,
                        fastMode, cluster, rmf, deliverFork, sync);
            }
        }
        if (type.equals("a")) {
            for (int i = 0 ; i < g ; i++) {
                group[i] = new asyncBcServer(addr, port, id, i, f, tmo, tmoInterval, maxTx,
                        fastMode, cluster, rmf, deliverFork, sync);
            }
        }
        bc = group[0].initBC(id);
    }

    private void deliverFromGroup() throws InterruptedException {
        int currChannel = 0;
        int currBlock = 0;
        while (!stopped) {
            for (currChannel = 0 ; currChannel < g ; currChannel++) {
                Types.Block cBlock = group[currChannel].deliver(currBlock);
                cBlock = cBlock.toBuilder()
                        .setHeader(cBlock.getHeader().toBuilder()
                            .setHeight(bc.getHeight() + 1)
                            .setPrev(ByteString.copyFrom(
                                DigestMethod.hash(bc.getBlock(bc.getHeight()).getHeader().toByteArray())))
                            .build())
                        .build();
                synchronized (bc) {
                    bc.addBlock(cBlock);
                    bc.notify();
                }

            }
            currBlock++;
        }
    }
    public void start() throws InterruptedException {
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
        latch.await();
        for (int i = 0 ; i < g ; i++) {
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
        for (int i = 0 ; i < g ; i++) {
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
        for (int i = 0 ; i < g ; i++) {
            group[i].serve();
        }
        deliverThread.start();
    }

    public String addTransaction(byte[] data, int clientID) {
        String ret = group[lastChannel].addTransaction(data, clientID);
        lastChannel = (lastChannel + 1) % g;
        return ret;
    }

    public int isTxPresent(String txID) {
        for (int i = 0 ; i < g ; i++) {
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
        for (int i = 0 ; i < g ; i++) {
            ((byzantineBcServer) group[i]).setByzSetting(fullByz, groups);
        }
    }

    public void setAsyncParam(int maxTime) {
        if (!type.equals("a")) {
            logger.debug(format("G-%d Unable to set async behaviour to non async node", id));
            return;
        }
        for (int i = 0 ; i < g ; i++) {
            ((asyncBcServer) group[i]).setAsyncParam(maxTime);
        }
    }

}
