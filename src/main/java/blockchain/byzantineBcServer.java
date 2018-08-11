package blockchain;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Config;
import config.Node;
import crypto.DigestMethod;
import proto.Block;
import proto.BlockHeader;
import proto.Transaction;
import rmf.ByzantineRmfNode;
import rmf.RmfNode;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class byzantineBcServer extends bcServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cbcServer.class);
    private boolean fullByz = false;
    public byzantineBcServer(String addr, int rmfPort, int syncPort, int id) {
        super(addr, rmfPort, syncPort, id);
        rmfServer.stop();
        rmfServer = new ByzantineRmfNode(id, addr, rmfPort, Config.getF(),
                Config.getCluster(), Config.getRMFbbcConfigHome());
    }

//    private ByzantineRmfNode rmfServer;
//    private blockchain bc; // TODO: Currently implement as basicBlockchain but we will make it dynamically (using config file)
//    private int currHeight;
//    private int f;
//    private int n;
//    private int currLeader;
//    private boolean stopped;
//    private boolean fullByz = false;
//    private final Object blockLock = new Object();
//    private final Object newBlockNotifyer = new Object();
//    private block currBlock; // TODO: As any server should disseminates its block once in an epoch, currently a block is shipped as soon the server turn is coming.
//
//    private int maxTransactionInBlock; // TODO: Should be done by configuration
//    private int tmo;
//    private int tmoInterval;
//    private int initTmo;
//    private Thread mainThread;
//
//    public byzantineBcServer(String addr, int port, int id) {
//        super(addr, port, id); // TODO: Should be changed according to Config!
//        rmfServer = new ByzantineRmfNode(id, addr, port, Config.getF(),
//                Config.getRMFcluster(), Config.getRMFbbcConfigHome());
//        bc = new basicBlockchain(id);
//        currBlock = bc.createNewBLock();
//        this.f = Config.getF();
//        n = Config.getN();
//        stopped = false;
//        currHeight = 1; // starts from 1 due to the genesis block
//        currLeader = 0;
//        this.maxTransactionInBlock = Config.getMaxTransactionsInBlock();
//        tmo =  Config.getTMO();
//        tmoInterval =  Config.getTMOInterval();
//        initTmo = tmo;
//        mainThread = new Thread(this::mainLoop);
//    }
//
//    public void start() {
//        rmfServer.start();
//        logger.info(format("[#%d] has been initialized successfully", getID()));
//    }
//
//    public void serve() {
//        // TODO: did a problem might occur if not all servers starts at once?
//        mainThread.start();
//
//    }
//
//    public void shutdown() {
//        stopped =  true;
//        mainThread.interrupt();
//        try {
//            mainThread.join();
//        } catch (InterruptedException e) {
//            logger.error("", e);
//        }
//        rmfServer.stop();
//        logger.info(format("[#%d] has been shutdown successfully", getID()));
//    }
//    private void updateLeader() {
//        currLeader = (currLeader + 1) % n;
//    }
//
//    private void mainLoop() {
//            while (!stopped) {
//                leaderImpl();
//                byte[] recData = rmfServer.deliver(currHeight, currLeader, tmo).getData().toByteArray();
//                if (recData.length == 0) {
//                    tmo += tmoInterval;
//                    logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
//                    updateLeader();
//                    continue;
//                }
//                Block recBlock;
//                try {
//                    recBlock = Block.parseFrom(recData);
//                } catch (InvalidProtocolBufferException e) {
//                    logger.warn("Unable to parse received block", e);
//                    updateLeader();
//                    continue;
//                }
//
//                if (!bc.validateBlockHash(recBlock)) {
//                    handlePossibleFork(recBlock);
//                    continue;
//                }
//
//                if (bc.getHeight() + 1 > f && bc.getBlocks(bc.getHeight() - f, bc.getHeight()).
//                        stream().
//                        map(b -> b.getHeader().getCreatorID()).
//                        collect(Collectors.toList()).
//                        contains(recBlock.getHeader().getCreatorID())) {
//                    tmo += tmoInterval;
//                    logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
//                    updateLeader();
//                    continue;
//                }
//
//                if (!bc.validateBlockData(recBlock)) {
//                    logger.warn(format("[#%d] received an invalid data in a valid block, creating an empty block [height=%d]", getID(), currHeight));
//                    recBlock = Block.newBuilder(). // creates emptyBlock
//                            setHeader(BlockHeader.
//                            newBuilder().
//                            setCreatorID(currLeader).
//                            setHeight(currHeight).
//                            setPrev(ByteString.copyFrom(DigestMethod.
//                                    hash(bc.getBlock(currHeight - 1).getHeader())))).
//                            build();
//                }
//
//                tmo = initTmo;
//                synchronized (newBlockNotifyer) {
//                    bc.addBlock(recBlock);
//                    newBlockNotifyer.notify();
//                }
//
//                currHeight++;
//                updateLeader();
//            }
//    }

     void leaderImpl() {
        if (currLeader != getID()) {
            return;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

        synchronized (blockLock) {
//            logger.info(format("[#%d] [heigh1=%d", getID(), currHeight, ));
            Block sealedBlock1 = currBlock.construct(getID(), currHeight,
                    DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
            String msg = "Hello Byz";
            addTransaction(msg.getBytes(), getID());
            Block sealedBlock2 = currBlock.construct(getID(), currHeight,
                    DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
            currBlock = bc.createNewBLock();
            List<Integer> all = new ArrayList<>();
            for (int i = 0 ; i < n ;i++) {
                all.add(i);
            }
//            Collections.shuffle(all);
            List<Integer> heights = new ArrayList<>();
            List<byte[]> msgs = new ArrayList<>();
            msgs.add(sealedBlock1.toByteArray());
            msgs.add(sealedBlock2.toByteArray());
            List<List<Integer>> sids = new ArrayList<>();
            sids.add(all.subList(0, 2));
            sids.add(all.subList(2, 4));
            for (int i = 0 ; i < 2 ; i++) {
                heights.add(currHeight);
            }
            if (fullByz) {
                ((ByzantineRmfNode)rmfServer).devidedBroadcast(cidSeries, cid, msgs, heights, sids);

            } else {
                ((ByzantineRmfNode)rmfServer).selectiveBroadcast(cidSeries, cid, sealedBlock1.toByteArray(), currHeight, all.subList(0, n/2));
            }
        }
    }

    @Override
    blockchain initBC(int id) {
        return new basicBlockchain(id);
    }

    @Override
    blockchain getBC(int start, int end) {
        return new basicBlockchain(this.bc, start, end);
    }

    //    public boolean addTransaction(byte[] data, int clientID) {
//        Transaction t = Transaction.newBuilder().setClientID(clientID).setData(ByteString.copyFrom(data)).build();
//        synchronized (blockLock) {
//            if (currBlock.getTransactionCount() >= maxTransactionInBlock) {
//                logger.info(format("[#%d] can't add new transaction from [client=%d], due to lack of space", getID(), t.getClientID()));
//                return false;
//            }
//            if (!currBlock.validateTransaction(t)) {
//                logger.info(format("[#%d] received an invalid transaction from [client=%d]", getID(), t.getClientID()));
//                return false;
//            }
//            currBlock.addTransaction(t);
//            return true;
//        }
//
//    }
//
    public void setFullByz() {
        fullByz = true;
    }
//    private void handlePossibleFork(Block b) {
//        logger.warn(format("[#%d] possible fork! [height=%d] [%s] : [%s]",
//                getID(), currHeight, Arrays.toString(b.getHeader().getPrev().toByteArray()),
//                Arrays.toString(Objects.requireNonNull(DigestMethod.hash(bc.getBlock(b.getHeader().getHeight() - 1).getHeader())))));
//        System.exit(1);
//    }
//
//    public Block deliver(int index) {
//        synchronized (newBlockNotifyer) {
//            while (index > bc.getHeight() - f) {
//                try {
//                    newBlockNotifyer.wait();
//                } catch (InterruptedException e) {
//                    logger.error("Servers returned tentative block", e);
//                }
//            }
//        }
//        return bc.getBlock(index);
//    }
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
