package blockchain;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Config;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import crypto.blockDigSig;
import crypto.rmfDigSig;
import proto.*;
import rmf.RmfNode;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class bcServer extends Node {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bcServer.class);
    protected RmfNode rmfServer;
    protected RBrodcastService rbService;
    protected blockchain bc; // TODO: Currently implement as basicBlockchain but we will make it dynamically (using config file)
    protected int currHeight;
    protected int f;
    protected int n;
    protected int currLeader;
    protected boolean stopped;
    protected final Object blockLock = new Object();
    protected block currBlock; // TODO: As any server should disseminates its block once in an epoch, currently a block is shipped as soon the server turn is coming.
    protected final Object newBlockNotifyer = new Object();
    protected int maxTransactionInBlock; // TODO: Should be done by configuration
    protected int tmo;
    protected int tmoInterval;
    protected int initTmo;
    protected Thread mainThread;
    protected Thread panicThread;
    protected final List<Integer> cids;
    protected HashMap<Integer, ForkProof> fp;
    final Object panicLock = new Object();

    // TODO: Currently nodes, f, tmoInterval, tmo and configHome are coded but we will turn it into configuration file
    public bcServer(String addr, int port, int id) {

        super(addr, port, id); // TODO: Should be changed according to Config!
        rmfServer = new RmfNode(id, addr, port, Config.getF(),
                Config.getRMFcluster(), Config.getRMFbbcConfigHome());

        rbService = new RBrodcastService(id, Config.getRBroadcastConfigHome());
        bc = new basicBlockchain(id);
        currBlock = bc.createNewBLock();
        this.f = Config.getF();
        n = Config.getN();
        stopped = false;
        currHeight = 1; // starts from 1 due to the genesis block
        currLeader = 0;
        tmo =  Config.getTMO();
        tmoInterval = Config.getTMOInterval();
        initTmo = tmo;
        this.maxTransactionInBlock = Config.getMaxTransactionsInBlock();
        cids = new ArrayList<>();
        fp = new HashMap<>();
        mainThread = new Thread(this::mainLoop);
        panicThread = new Thread(() -> {
            try {
                deliverForkAnnounce();
            } catch (Exception e) {
                logger.error("", e);
            }
        });
    }

    public void start() {
        rmfServer.start();
        rbService.start();
        logger.info(format("[#%d] has been initialized successfully", getID()));
    }

    public void serve() {
        // TODO: did a problem might occur if not all servers starts at once?
        panicThread.start();
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
        rbService.shutdown();
        logger.info(format("[#%d] has been shutdown successfully", getID()));
    }
    private void updateLeader() {
        currLeader = (currLeader + 1) % n;
    }

    private void mainLoop() {
        while (!stopped) {
            synchronized (panicLock) {
                if (fp.containsKey(currHeight)) {
                    ForkProof p = fp.get(currHeight);
                    fp.remove(currHeight);
                    handleFork(p);
                }
                leaderImpl();
                RmfResult msg = rmfServer.deliver(currHeight, currLeader, tmo);
                byte[] recData = msg.getData().toByteArray();
                int cid = msg.getCid();
                if (recData.length == 0) {
                    tmo += tmoInterval;
                    logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
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
                // TODO: validate meta info (if the meta isn't match we should treat it as a Byz behaviour
                if (recBlock.getHeader().getHeight() != currHeight ||
                        recBlock.getHeader().getCreatorID() != currLeader ||
                        !bc.validateBlockData(recBlock)) {
                    logger.warn(format("[#%d] received an invalid data in a valid block, creating an empty block [height=%d]", getID(), currHeight));
                    recBlock = Block.newBuilder(). // creates emptyBlock
                            setHeader(BlockHeader.
                            newBuilder().
                            setCreatorID(currLeader).
                            setHeight(currHeight).
                            setPrev(ByteString.copyFrom(DigestMethod.
                                    hash(bc.getBlock(currHeight - 1).getHeader().toByteArray())))).
                            build();
                }

                if (!bc.validateBlockHash(recBlock)) {
                    announceFork(recBlock, cid);
                    continue;
                }

                if (bc.getHeight() + 1 > f && bc.getBlocks(bc.getHeight() - f, bc.getHeight()).
                        stream().
                        map(b -> b.getHeader().getCreatorID()).
                        collect(Collectors.toList()).
                        contains(recBlock.getHeader().getCreatorID())) {
                    logger.info(format("[#%d] Unable to receive block, sender is in the last f blocks", getID()));
                    tmo += tmoInterval;
                    logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
                    updateLeader();
                    continue;
                }


                tmo = initTmo;
                synchronized (newBlockNotifyer) {
                    synchronized (cids) {
                        cids.add(cid);
                        bc.addBlock(recBlock);
                        newBlockNotifyer.notify();
                    }
                }

                currHeight++;
                updateLeader();
            }
        }
    }

    abstract void leaderImpl();

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

    private boolean validateForkProof(ForkProof p)  {
        Block curr = null;
        Block prev = null;
        try {
            curr = Block.parseFrom(p.getCurr().getData());
            prev = Block.parseFrom(p.getPrev().getData());
        } catch (InvalidProtocolBufferException e) {
            logger.error("", e);
            return false;
        }
        int currBlockH = curr.getHeader().getHeight();
        if (fp.containsKey(currBlockH)) {
            return false;
        }
        fp.put(currBlockH, p);
        if (currBlockH > currHeight) {
            return false;
        }

        if (!(curr.getHeader().getCreatorID() == currLeader && curr.getHeader().getHeight() == currHeight)
                && bc.getBlock(currBlockH).getHeader().getCreatorID() != curr.getHeader().getCreatorID()) {
            return false;
        }

        if (bc.getBlock(currBlockH - 1).getHeader().getCreatorID() != prev.getHeader().getCreatorID()) {
            return false;
        }
//        Data currData = Data.newBuilder().
//                setMeta(Meta.newBuilder().
//                        setCid(p.getCurr().getCid()).
//                        setHeight(curr.getHeader().getHeight()).
//                        setSender(curr.getHeader().getCreatorID()).
//                        build()).
//                setData(curr.toByteString()).
//                setSig(p.getCurrSig()).
//                build();
        if (!blockDigSig.verify(curr.getHeader().getCreatorID(), p.getCurr().getCid(), p.getCurrSig(), curr)) {
            return false;
        }
//        if (!rmfDigSig.verify(curr.getHeader().getCreatorID(), currData)) {
//            return false;
//        }
//        Data prevData = Data.newBuilder().
//                setMeta(Meta.newBuilder().
//                        setCid(p.getPrev().getCid()).
//                        setHeight(prev.getHeader().getHeight()).
//                        setSender(prev.getHeader().getCreatorID()).
//                        build()).
//                setData(prev.toByteString()).
//                setSig(p.getPrevSig()).
//                build();
//        if (!rmfDigSig.verify(prev.getHeader().getCreatorID(), prevData)) {
//            return false;
//        }

        if (!blockDigSig.verify(prev.getHeader().getCreatorID(), p.getPrev().getCid(), p.getPrevSig(), prev)) {
            return false;
        }

        logger.info(format("[#%d] panic for fork has been delivered", getID()));
        return true;
    }
    private void deliverForkAnnounce() throws InterruptedException, InvalidProtocolBufferException {
        while (!stopped) {
            ForkProof p = ForkProof.parseFrom(rbService.deliver());
            synchronized (panicLock) {
                handleFork(p);
            }
        }
    }
    // TODO: We should handle the case where cid = 0 (edge case, not critical for correctness)
    private void announceFork(Block b, int cid) {
        logger.warn(format("[#%d] possible fork! [height=%d] [%s] : [%s]",
                getID(), currHeight, Arrays.toString(b.getHeader().getPrev().toByteArray()),
                Arrays.toString(Objects.requireNonNull(DigestMethod.hash(bc.getBlock(b.getHeader().getHeight() - 1).getHeader().toByteArray())))));
        int prevCid = cids.get(bc.getHeight() - 1);
        ForkProof p = ForkProof.
                newBuilder().
                setCurr(rmfServer.nonBlockingDeliver(cid)).
                setCurrSig(rmfServer.getRmfDataSig(cid)).
                setPrev(rmfServer.nonBlockingDeliver(prevCid)).
                setPrevSig(rmfServer.getRmfDataSig(prevCid)).
                build();
        rbService.broadcast(p.toByteArray(), getID());
        handleFork(p);
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

    private void handleFork(ForkProof p) {
        if (!validateForkProof(p)) {
            return;
        }
        logger.info(format("[#%d] handleFork has been called", getID()));
        try {
            Thread.sleep(60 * 10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
