package blockchain;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Config;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import crypto.rmfDigSig;
import proto.*;
import rmf.RmfNode;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class cbcServer extends bcServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cbcServer.class);

    public cbcServer(String addr, int port, int id) {
        super(addr, port, id);
    }
//    protected RmfNode rmfServer;
//    protected RBrodcastService rbService;
//    protected blockchain bc; // TODO: Currently implement as basicBlockchain but we will make it dynamically (using config file)
//    protected int currHeight;
//    protected int f;
//    protected int n;
//    protected int currLeader;
//    protected boolean stopped;
//    protected final Object blockLock = new Object();
//    protected block currBlock; // TODO: As any server should disseminates its block once in an epoch, currently a block is shipped as soon the server turn is coming.
//    protected final Object newBlockNotifyer = new Object();
//    protected int maxTransactionInBlock; // TODO: Should be done by configuration
//    protected int tmo;
//    protected int tmoInterval;
//    protected int initTmo;
//    protected Thread mainThread;
//    protected Thread panicThread;
//    protected final List<Integer> cids;
//    protected HashMap<Integer, ForkProof> fp;
//    final Object panicLock = new Object();
//
//    // TODO: Currently nodes, f, tmoInterval, tmo and configHome are coded but we will turn it into configuration file
//    public cbcServer(String addr, int port, int id) {
//        super(addr, port, id); // TODO: Should be changed according to Config!
//        rmfServer = new RmfNode(id, addr, port, Config.getF(),
//                Config.getRMFcluster(), Config.getRMFbbcConfigHome());
//
//        rbService = new RBrodcastService(id, Config.getRBroadcastConfigHome());
//        bc = new basicBlockchain(id);
//        currBlock = bc.createNewBLock();
//        this.f = Config.getF();
//        n = Config.getN();
//        stopped = false;
//        currHeight = 1; // starts from 1 due to the genesis block
//        currLeader = 0;
//        tmo =  Config.getTMO();
//        tmoInterval = Config.getTMOInterval();
//        initTmo = tmo;
//        this.maxTransactionInBlock = Config.getMaxTransactionsInBlock();
//        cids = new ArrayList<>();
//        fp = new HashMap<>();
//        mainThread = new Thread(this::mainLoop);
//        panicThread = new Thread(() -> {
//            try {
//                deliverForkAnnounce();
//            } catch (Exception e) {
//                logger.error("", e);
//            }
//        });
//    }
//
//    public void start() {
//        rmfServer.start();
//        logger.info(format("[#%d] has been initialized successfully", getID()));
//    }
//
//    public void serve() {
//         // TODO: did a problem might occur if not all servers starts at once?
//        panicThread.start();
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
//            currLeader = (currLeader + 1) % n;
//    }
//
//    private void mainLoop() {
//        while (!stopped) {
//            synchronized (panicLock) {
//                if (fp.containsKey(currHeight)) {
//                    handleFork();
//                }
//                leaderImpl();
//                RmfResult msg = rmfServer.deliver(currHeight, currLeader, tmo);
//                byte[] recData = msg.getData().toByteArray();
//                int cid = msg.getCid();
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
//                    announceFork(recBlock, cid);
//                    continue;
//                }
//
//                if (bc.getHeight() + 1 > f && bc.getBlocks(bc.getHeight() - f, bc.getHeight()).
//                        stream().
//                        map(b -> b.getHeader().getCreatorID()).
//                        collect(Collectors.toList()).
//                        contains(recBlock.getHeader().getCreatorID())) {
//                    logger.info(format("[#%d] Unable to receive block, sender is in the last f blocks", getID()));
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
//                tmo = initTmo;
//                synchronized (newBlockNotifyer) {
//                    synchronized (cids) {
//                        cids.add(cid);
//                        bc.addBlock(recBlock);
//                        newBlockNotifyer.notify();
//                    }
//                }
//
//                currHeight++;
//                updateLeader();
//            }
//        }
//    }

    void leaderImpl() {
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
//    private void deliverForkAnnounce() throws InterruptedException, InvalidProtocolBufferException {
//        while (!stopped) {
//            ForkProof p = ForkProof.parseFrom(rbService.deliver());
//            synchronized (panicLock) {
//                Block curr = Block.parseFrom(p.getCurr().getData());
//                int currBlockH = curr.getHeader().getHeight();
//                if (currBlockH >= currHeight) {
//                    if (!fp.containsKey(currBlockH)) {
//                        fp.put(currBlockH, p);
//                    }
//                    continue;
//                }
//
//                Data currData = Data.newBuilder().
//                        setMeta(Meta.newBuilder().
//                                setCid(p.getCurr().getCid()).
//                                setHeight(curr.getHeader().getHeight()).
//                                setSender(curr.getHeader().getCreatorID()).
//                                build()).
//                        setData(curr.toByteString()).
//                        setSig(rmfServer.getRmfDataSig(p.getCurr().getCid())).
//                        build();
//                if (!rmfDigSig.verify(curr.getHeader().getCreatorID(), currData)) {
//                    continue;
//                }
//
//                Block prev = Block.parseFrom(p.getPrev().getData());
//
//                Data prevData = Data.newBuilder().
//                        setMeta(Meta.newBuilder().
//                                setCid(p.getPrev().getCid()).
//                                setHeight(prev.getHeader().getHeight()).
//                                setSender(prev.getHeader().getCreatorID()).
//                                build()).
//                        setData(prev.toByteString()).
//                        setSig(rmfServer.getRmfDataSig(p.getPrev().getCid())).
//                        build();
//                if (!rmfDigSig.verify(prev.getHeader().getCreatorID(), prevData)) {
//                    continue;
//                }
//
//                logger.info(format("[#%d] panic for fork has been delivered", getID()));
//                handleFork();
//            }
//        }
//    }
//
//    private void announceFork(Block b, int cid) {
//        logger.warn(format("[#%d] possible fork! [height=%d] [%s] : [%s]",
//                getID(), currHeight, Arrays.toString(b.getHeader().getPrev().toByteArray()),
//                Arrays.toString(Objects.requireNonNull(DigestMethod.hash(bc.getBlock(b.getHeader().getHeight() - 1).getHeader())))));
//        int prevCid = cids.get(bc.getHeight());
//        ForkProof p = ForkProof.
//                newBuilder().
//                setCurr(rmfServer.nonBlockingDeliver(cid)).
//                setPrev(rmfServer.nonBlockingDeliver(prevCid))
//                .build();
//        if (!fp.containsKey(b.getHeader().getHeight())) {
//            fp.put(b.getHeader().getHeight(), p);
//        }
//        rbService.broadcast(p.toByteArray(), getID());
//      handleFork();
//    }
//
//    public Block deliver(int index) {
//        synchronized (newBlockNotifyer) {
//            while (index > bc.getHeight() - f) {
//                try {
//                    newBlockNotifyer.wait();
//                } catch (InterruptedException e) {
//                    logger.error("Servers returned tentative block", e);
//                    return null;
//                }
//            }
//        }
//        return bc.getBlock(index);
//    }
//
//    private void handleFork() {
//        logger.info(format("[#%d] handleFork has been called", getID()));
//        System.exit(1);
//    }
}
