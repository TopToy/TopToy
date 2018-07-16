package blockchain;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Node;
import crypto.DigestMethod;
import proto.Block;
import proto.BlockHeader;
import proto.Transaction;
import rmf.RmfNode;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import static java.lang.String.format;

public class bcServer extends Node {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bcServer.class);
    private RmfNode rmfServer;
    private blockchain bc; // TODO: Currently implement as basicBlockchain but we will make it dynamically (using config file)
    private int currHeight;
    private int f;
    private int n;
    private int currLeader;
    private boolean stopped;
//    final Object leaderLock = new Object();
    private final Object blockLock = new Object();
    private Semaphore newBlockNotifyer = new Semaphore(0);
    private final Object latencyNotifyer = new Object();
//    final Object heightLock = new Object();
    private block currBlock; // TODO: As any server should disseminates its block once in an epoch, currently a block is shipped as soon the server turn is coming.

    private int maxTransactionInBlock; // TODO: Should be done by configuration

    private Thread mainThread;
//    private Thread rmfThread;

    // TODO: Currently nodes, f, tmoInterval, tmo and configHome are coded but we will turn it into configuration file
    public bcServer(String addr, int port, int id, int f, ArrayList<Node> nodes, String bbcConfigHome, int maxTransactionInBlock) {
        super(addr, port, id);
        rmfServer = new RmfNode(id, addr, port, f, 100, 1000 * 1, nodes, bbcConfigHome);
        bc = new basicBlockchain(id);
        currBlock = bc.createNewBLock();
        this.f = f;
        n = 3*f +1;
        stopped = false;
        currHeight = 1; // starts from 1 due to the genesis block
        currLeader = 0;
        this.maxTransactionInBlock = maxTransactionInBlock;
    }

    public void start() {
        startServices();
        logger.info(format("[#%d] has been initialized successfully", getID()));
    }
     private void startServices() {
      rmfServer.start(); // TODO: Note that if more than one server is running then this call blocks until all bbc servers are up.
//        try {
//            Thread.sleep(1000 * 5); // TODO: Do we really need this out of tests??
//        } catch (InterruptedException e) {
//            logger.error("", e);
//        }

//      waitForAll();
//         try {
//             Thread.sleep(10 * 1000);
//         } catch (InterruptedException e) {
//             e.printStackTrace();
//         }
         mainThread = new Thread(this::mainLoop); // TODO: did a problem might occur if not all servers starts at once?
      mainThread.start();

    }

//    /*
//    Currently the system is unable to run unless all servers are up (not only 2f + 1)? we now tries to connect them all.
//     */
//    private void waitForAll() {
//        rmfServer.broadcast("ready".getBytes(), 0);
//        Thread[] allUp = new Thread[n];
//        for (int i = 0 ; i < n ; i++) {
//            int finalI = i;
//            allUp[i] = new Thread(() -> {
//                while (rmfServer.deliver(0, finalI) == null) {
//                    try {
//                        Thread.sleep(1000 * 1);
//                    } catch (InterruptedException e) {
//                        logger.error(format("[#%d] interrupted while waiting for [#%d]", getID(), finalI), e);
//                    }
//                    logger.info(format("[#%d] waits for [#%d] to be ready", getID(), finalI));
//                }
//                logger.info(format("[#%d] received ready from [#%d]", getID(), finalI));
//            });
//            allUp[i].start();
//        }
//        for (int i = 0 ; i < n ; i++) {
//            try {
//                allUp[i].join();
//            } catch (InterruptedException e) {
//                logger.error(format("[#%d] interrupted while waiting for [#%d] to join", getID(), i), e);
//            }
//        }
//    }
    public void shutdown() {
        stopped =  true;
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        rmfServer.stop();
        logger.info(format("[#%d] has been shutdown successfully", getID()));
    }
    private void updateLeader() {
            currLeader = (currLeader + 1) % n;
    }

    private void mainLoop() {
        while (!stopped) {
            leaderImpl();
            byte[] recData = rmfServer.deliver(currHeight, currLeader);
            if (recData == null) {
                updateLeader();
                continue;
            }
            Block recBlock;
            try {
                recBlock = Block.parseFrom(recData);
            } catch (InvalidProtocolBufferException e) {
                logger.warn("Unable to parse received block", e);
                updateLeader();
                continue;
            }

            if (!bc.validateBlockHash(recBlock)) {
                handlePossibleFork();
                continue;
            }
            if (!bc.validateBlockData(recBlock)) {
                logger.warn(format("[#%d] received an invalid data in a valid block, creating an empty block [height=%d]", getID(), currHeight));
                recBlock = Block.newBuilder(). // creates emptyBlock
                        setHeader(BlockHeader.
                        newBuilder().
                        setCreatorID(currLeader).
                        setHeight(currHeight).
                        setPrev(DigestMethod.
                                hash(bc.getBlock(currHeight - 1)))).
                        build();
            }
            bc.addBlock(recBlock);
            currHeight++;
            updateLeader();
            synchronized (latencyNotifyer) {
                if (bc.getHeight() < f) {
                    continue;
                }
                if (bc.getHeight() == f) {
                    latencyNotifyer.notify();
                }
            }

            newBlockNotifyer.release();

        }
    }

    private void leaderImpl() {
        if (currLeader != getID()) {
            return;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

        synchronized (blockLock) {
            Block sealedBlock = currBlock.construct(getID(), currHeight, DigestMethod.hash(bc.getBlock(currHeight - 1)));
            currBlock = bc.createNewBLock();
            rmfServer.broadcast(sealedBlock.toByteArray(), currHeight);
        }


    }

    public boolean addTransaction(byte[] data, int clientID) {
        Transaction t = Transaction.newBuilder().setClientID(clientID).setData(ByteString.copyFrom(data)).build();
        synchronized (blockLock) {
            if (currBlock.getTransactionCount() >= maxTransactionInBlock) {
                logger.info(format("[#%d] can't add new transaction from [client=%d], due to lack of space", getID(), t.getClientID()));
                return false;
            }
            if (!currBlock.validateTransaction(t)) {
                logger.info(format("[#%d] received an invalid transaction from [client=%d]", getID(), t.getClientID()));
                return false;
            }
            currBlock.addTransaction(t);
            return true;
        }

    }

    private void handlePossibleFork() {
        logger.fatal("Possible fork has been detected, currently the synchronization service is not yet implemented");
    }

    public Block deliver() {
        synchronized (latencyNotifyer) {
            while (bc.getHeight() < f) {
                try {
                    latencyNotifyer.wait();
                } catch (InterruptedException e) {
                    logger.error("Servers returned tentative block", e);
                }
            }
        }
        try {
            newBlockNotifyer.acquire();
        } catch (InterruptedException e) {
            logger.error("Server released before receiving a new block", e);
        }
        return bc.getBlock(bc.getHeight() - f);
    }
}
