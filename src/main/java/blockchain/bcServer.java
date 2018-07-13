package blockchain;

import com.google.protobuf.InvalidProtocolBufferException;
import config.Node;
import proto.Block;
import proto.Transaction;
import rmf.RmfNode;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

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
        startServices();


    }

    private void startServices() {
      rmfServer.start();
        try {
            Thread.sleep(1000 * 5); // TODO: Do we really need this out of tests??
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        mainThread = new Thread(this::mainLoop); // TODO: did a problem might occur if not all servers starts at once?
      mainThread.start();

    }
    public void shutdown() {
        stopped =  true;
        mainThread.interrupt();
//        rmfThread.interrupt();
        try {
//            rmfThread.join();
            mainThread.join();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        rmfServer.stop();
    }
    private void updateLeader() {
            currLeader = (currLeader + 1) % n;
    }

    private void mainLoop() {
        while (!stopped) {
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
                        setCreatorID(currLeader).
                        setHeight(currHeight).
                        setPrevHash(bc.getBlock(currHeight - 1).hashCode())
                        .build();
            }
            bc.addBlock(recBlock);

//            synchronized (heightLock) {
                currHeight++;
//            }
            updateLeader();
            leaderImpl();

        }
    }

    private void leaderImpl() {
        if (currLeader != getID()) {
            return;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

        synchronized (blockLock) {
            Block sealedBlock = currBlock.construct(currHeight, bc.getBlock(currHeight - 1).hashCode());
            currBlock = bc.createNewBLock();
            rmfServer.broadcast(sealedBlock.toByteArray(), currHeight);
        }


    }

    public boolean addTransaction(Transaction t) {
        synchronized (blockLock) {
            if (currBlock.getSize() >= maxTransactionInBlock) {
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
}
