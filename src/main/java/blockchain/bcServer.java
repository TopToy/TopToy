package blockchain;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Config;
import config.Node;
import crypto.DigestMethod;
import proto.Block;
import proto.BlockHeader;
import proto.Crypto;
import proto.Transaction;
import rmf.RmfNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
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
//    private Semaphore newBlockNotifier = new Semaphore(0, true);
//    private final Object latencyNotifier = new Object();
//    final Object heightLock = new Object();
    private block currBlock; // TODO: As any server should disseminates its block once in an epoch, currently a block is shipped as soon the server turn is coming.
    private final Object newBlockNotifyer = new Object();
    private int maxTransactionInBlock; // TODO: Should be done by configuration

    private Thread mainThread;
//    private Thread rmfThread;

    // TODO: Currently nodes, f, tmoInterval, tmo and configHome are coded but we will turn it into configuration file
    public bcServer(String addr, int port, int id) {
        super(addr, port, id); // TODO: Should be changed according to Config!
        rmfServer = new RmfNode(id, addr, port, Config.getF(), Config.getTMOInterval(), Config.getTMO(),
                Config.getRMFcluster(), Config.getRMFbbcConfigHome());
        bc = new basicBlockchain(id);
        currBlock = bc.createNewBLock();
        this.f = Config.getF();
        n = Config.getN();
        stopped = false;
        currHeight = 1; // starts from 1 due to the genesis block
        currLeader = 0;
        this.maxTransactionInBlock = Config.getMaxTransactionsInBlock();
        mainThread = new Thread(this::mainLoop);
    }

    public void start() {
        rmfServer.start();
        logger.info(format("[#%d] has been initialized successfully", getID()));
    }

    public void serve() {
         // TODO: did a problem might occur if not all servers starts at once?
        mainThread.start();

    }

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
                handlePossibleFork(recBlock);
                continue;
            }
            if (!bc.validateBlockData(recBlock)) {
                logger.warn(format("[#%d] received an invalid data in a valid block, creating an empty block [height=%d]", getID(), currHeight));
                recBlock = Block.newBuilder(). // creates emptyBlock
                        setHeader(BlockHeader.
                        newBuilder().
                        setCreatorID(currLeader).
                        setHeight(currHeight).
                        setPrev(ByteString.copyFrom(DigestMethod.
                                hash(bc.getBlock(currHeight - 1).getHeader())))).
                        build();
            }

            synchronized (newBlockNotifyer) {
                bc.addBlock(recBlock);
                newBlockNotifyer.notify();
            }

            currHeight++;
            updateLeader();
//            synchronized (latencyNotifier) {
//                if (bc.getHeight() < f) {
//                    continue;
//                }
//                if (bc.getHeight() == f) {
//                    latencyNotifier.notify();
//                }
//            }

//            newBlockNotifier.release();

        }
    }

    private void leaderImpl() {
        if (currLeader != getID()) {
            return;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

        synchronized (blockLock) {
//            logger.info(format("[#%d] [heigh1=%d", getID(), currHeight, ));
            Block sealedBlock = currBlock.construct(getID(), currHeight, DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader()));
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

    private void handlePossibleFork(Block b) {
        logger.warn(format("[#%d] possible fork! [height=%d] [%s] : [%s]",
                getID(), currHeight, Arrays.toString(b.getHeader().getPrev().toByteArray()),
                Arrays.toString(Objects.requireNonNull(DigestMethod.hash(bc.getBlock(b.getHeader().getHeight() - 1).getHeader())))));
        System.exit(1);
    }

    public Block deliver(int index) {
        synchronized (newBlockNotifyer) {
            while (index > bc.getHeight() - f) {
                try {
                    newBlockNotifyer.wait();
                } catch (InterruptedException e) {
                    logger.error("Servers returned tentative block", e);
                    return null;
                }
            }
        }
        return bc.getBlock(index);
    }
//    public Block deliverLast() {
//        synchronized (latencyNotifier) {
//            while (bc.getHeight() < f) {
//                try {
//                    latencyNotifier.wait();
//                } catch (InterruptedException e) {
//                    logger.error("Servers returned tentative block", e);
//                }
//            }
//        }
//        try {
//            newBlockNotifier.acquire();
//        } catch (InterruptedException e) {
//            logger.error("Server released before receiving a new block", e);
//        }
//        return bc.getBlock(bc.getHeight() - f);
//    }
}
