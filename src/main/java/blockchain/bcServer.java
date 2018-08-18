package blockchain;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Config;
import config.Node;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import crypto.blockDigSig;
import proto.*;
import rmf.RmfNode;
import proto.Types.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class bcServer extends Node {
    class fpEntry {
        ForkProof fp;
        boolean done;
    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bcServer.class);
    RmfNode rmfServer;
    private RBrodcastService panicRB;
    private RBrodcastService syncRB;
    blockchain bc; // TODO: Currently implement as basicBlockchain but we will make it dynamically (using config file)
    int currHeight;
    protected int f;
    protected int n;
    int currLeader;
    protected boolean stopped;
//    final Object blockLock = new Object();
    block currBlock; // TODO: As any server should disseminates its block once in an epoch, currently a block is shipped as soon the server turn is coming.
    private final Object newBlockNotifyer = new Object();
    private int maxTransactionInBlock; // TODO: Should be done by configuration
    private int tmo;
    private int tmoInterval;
    private int initTmo;
    private Thread mainThread;
    private Thread panicThread;
    int cid = 0;
    int cidSeries = 0;
    private final HashMap<Integer, fpEntry> fp;
    private HashMap<Integer, ArrayList<Types.subChainVersion>> scVersions;
    private final ArrayList<Types.Transaction> transactionsPool = new ArrayList<>();
    boolean configuredFastMode = Config.getFastMode();
    boolean fastMode = configuredFastMode;

    // TODO: Currently nodes, f, tmoInterval, tmo and configHome are coded but we will turn it into configuration file
    bcServer(String addr, int rmfPort, int id) {

        super(addr, rmfPort, id); // TODO: Should be changed according to Config!
        this.f = Config.getF();
        n = Config.getN();
        rmfServer = new RmfNode(id, addr, rmfPort, Config.getF(),
                Config.getCluster(), Config.getRMFbbcConfigHome());
        panicRB = new RBrodcastService(id, Config.getPanicRBConfigHome());
        syncRB = new RBrodcastService(id, Config.getSyncRBConfigHome());
        bc = initBC(id);
        currBlock = null;

        stopped = false;
        currHeight = 1; // starts from 1 due to the genesis block
        currLeader = 0;
        tmo =  Config.getTMO();
        tmoInterval = Config.getTMOInterval();
        initTmo = tmo;
        this.maxTransactionInBlock = Config.getMaxTransactionsInBlock();
        fp = new HashMap<>();
        scVersions = new HashMap<>();
        mainThread = new Thread(this::mainLoop);
        panicThread = new Thread(this::deliverForkAnnounce);

    }

    public void start() {
        rmfServer.start();
        logger.info(format("[#%d] rmf server is up", getID()));
        syncRB.start();
        logger.info(format("[#%d] sync server is up", getID()));
        panicRB.start();
        logger.info(format("[#%d] panic server is up", getID()));
        logger.info(format("[#%d] has been initialized successfully", getID()));
    }

    public void serve() {
        // TODO: did a problem might occur if not all servers starts at once?
        panicThread.start();
        logger.info(format("[#%d] started panic thread", getID()));
        mainThread.start();
        logger.info(format("[#%d] started main thread", getID()));
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
        panicRB.shutdown();
        syncRB.shutdown();
        panicThread.interrupt();
        try {
            panicThread.join();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        logger.info(format("[#%d] has been shutdown successfully", getID()));
    }
    private void updateLeaderAndHeight() {
        currHeight = bc.getHeight() + 1;
        currLeader = (currLeader + 1) % n;
        cid++;
    }

    private void mainLoop() {
        while (!stopped) {

            synchronized (fp) {
                for (Integer key : fp.keySet().stream().filter(k -> k < currHeight).collect(Collectors.toList())) {
                    fpEntry pe = fp.get(key);
                    if (!pe.done) {
                        logger.info(format("[#%d] have found a panic message [height=%d] [fp=%d]", getID(), currHeight, key));
                        handleFork(pe.fp);
                        fp.get(key).done = true;
                        fastMode = false;
                    }

                }
            }
            byte[] next;
            try {
                next = leaderImpl();
            } catch (InterruptedException e) {
                logger.info(format("[#%d] main thread has been interrupted on leader impl", getID()));
                continue;
            }
            byte[] msg;
            try {
                msg = rmfServer.deliver(cidSeries, cid, currHeight, currLeader, tmo, next);
            } catch (InterruptedException e) {
                logger.info(format("[#%d] main thread has been interrupted on rmf deliver", getID()));
                continue;
            }
            fastMode = configuredFastMode;
//            byte[] recData = msg.getData().toByteArray();
//            int mcid = msg.getCid();
//            int mcidSeries = msg.getCidSeries();
            if (msg == null) {
                tmo += tmoInterval;
                logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
                updateLeaderAndHeight();
                fastMode = false;
                cid++;
                continue;
            }
            if (currLeader == getID()) {
                logger.info(format("[#%d] nullify currBlock [height=%d] [cidSeries=%d, cid=%d]", getID(), currHeight,
                        cidSeries, cid));
                currBlock = null;
            }
            Block recBlock;
            try {
                recBlock = Block.parseFrom(msg);
            } catch (InvalidProtocolBufferException e) {
                logger.warn("Unable to parse received block", e);
                updateLeaderAndHeight();
                continue;
                // TODO: Here also we should create an empty block!
            }
            // TODO: validate meta info (if the meta isn't match we should treat it as a Byz behaviour
            // TODO: This may cause a problem for the fork proof! (the siganture isn't the original one, as the data) we should handle it carefully
            if(!bc.validateBlockData(recBlock)) {
                logger.warn(format("[#%d] received an invalid data in a valid block, creating an empty block [height=%d]", getID(), currHeight));
                recBlock = Block.newBuilder(). // creates emptyBlock
                        setHeader(BlockHeader.
                        newBuilder().
                        setCreatorID(currLeader).
                        setHeight(currHeight).
                        setCidSeries(cidSeries).
                        setCid(cid).
                        setPrev(ByteString.copyFrom(DigestMethod.
                                hash(bc.getBlock(currHeight - 1).getHeader().toByteArray())))).
                        build();
            }
//            recBlock = recBlock.
//                    toBuilder().
//                    setHeader(recBlock.
//                            getHeader().
//                            toBuilder().
//                            setCid(mcid).
//                            setCidSeries(mcidSeries).
//                            build()).
//                    build();
            if (!bc.validateBlockHash(recBlock)) {
                bc.validateBlockHash(recBlock);
                announceFork(recBlock);
                fastMode = false;
                continue;
            }
            if (!bc.validateBlockCreator(recBlock, f)) {
                updateLeaderAndHeight();
                tmo += tmoInterval;
                continue;
            }


            tmo = initTmo;
            synchronized (newBlockNotifyer) {
                bc.addBlock(recBlock);
                logger.info(String.format("[#%d] adds new block with [height=%d] [cidSeries=%d ; cid=%d]",
                        getID(), recBlock.getHeader().getHeight(), cidSeries, cid));
                newBlockNotifyer.notify();
            }
            updateLeaderAndHeight();
        }
    }

    abstract byte[] leaderImpl() throws InterruptedException;

    abstract blockchain initBC(int id);

    abstract blockchain getBC(int start, int end);


    public void addTransaction(byte[] data, int clientID) {
        Transaction t = Transaction.newBuilder().setClientID(clientID).setData(ByteString.copyFrom(data)).build();
        synchronized (transactionsPool) {
            logger.info(format("[#%d] add transaction from [client=%d]", getID(), t.getClientID()));
            transactionsPool.add(t);
        }
    }

    void addTransactionsToCurrBlock() {
        if (currBlock != null) return;
        currBlock = bc.createNewBLock();
        synchronized (transactionsPool) {
            if (transactionsPool.isEmpty()) return;
            while ((!transactionsPool.isEmpty()) && currBlock.getTransactionCount() < maxTransactionInBlock) {
                Transaction t = transactionsPool.get(0);
                transactionsPool.remove(0);
                if (!currBlock.validateTransaction(t)) {
                    logger.info(format("[#%d] detect an invalid transaction from [client=%d]", getID(), t.getClientID()));
                    continue;
                }
                currBlock.addTransaction(t);

            }
        }
    }

    private int validateForkProof(ForkProof p)  {
        Block curr = p.getCurr();
        Block prev = p.getPrev();
        int prevBlockH = prev.getHeader().getHeight();

        if (bc.getBlock(prevBlockH).getHeader().getCreatorID() != prev.getHeader().getCreatorID()) {
            return -1;
        }

        int currCreator = currLeader;
        if (bc.getHeight() >= curr.getHeader().getHeight()) {
            currCreator = bc.getBlock(curr.getHeader().getHeight()).getHeader().getCreatorID();
        }
        if (currCreator != curr.getHeader().getCreatorID()) {
            return -1;
        }

//        Block currAsInRmf = curr;
////        currAsInRmf = currAsInRmf.toBuilder().
//                setHeader(curr.getHeader().toBuilder().setCid(0).setCidSeries(0).build()).
//                build();
        if (!blockDigSig.verify(curr.getHeader().getCreatorID(), p.getCurr().getHeader().getCidSeries()
                , p.getCurr().getHeader().getCid(), p.getCurrSig(), curr)) {
            return -1;
        }

//        Block prevAsInRmf = prev;
//        prevAsInRmf = prevAsInRmf.toBuilder().
//                setHeader(prev.getHeader().toBuilder().setCid(0).setCidSeries(0).build()).
//                build();
        if (!blockDigSig.verify(prev.getHeader().getCreatorID(), p.getPrev().getHeader().getCidSeries(),
                p.getPrev().getHeader().getCid(), p.getPrevSig(), prev)) {
//            blockDigSig.verify(prev.getHeader().getCreatorID(), p.getPrev().getHeader().getCid(), p.getPrevSig(), prevAsInRmf);
                return -1;
        }

        logger.info(format("[#%d] panic for fork is valid [fp=%d]", getID(), p.getCurr().getHeader().getHeight()));
        return prev.getHeader().getHeight();
    }

    private void deliverForkAnnounce() {
        while (!stopped) {
            ForkProof p;
            try {
                p = ForkProof.parseFrom(panicRB.deliver());
            } catch (Exception e) {
                logger.error(format("[#%d]", getID()), e);
                continue;
            }
            synchronized (fp) {
                int pHeight = p.getCurr().getHeader().getHeight();
                if (!fp.containsKey(pHeight)) {
                    fpEntry fpe = new fpEntry();
                    fpe.done = false;
                    fpe.fp = p;
                    fp.put(pHeight, fpe);
                    logger.info(format("[#%d] interrupts the main thread", getID()));
                    mainThread.interrupt();
                }
            }
        }
    }
    // TODO: We should handle the case where cid = 0 (edge case, not critical for correctness)
    private void announceFork(Block b) {
        logger.warn(format("[#%d] possible fork! [height=%d] [%s] : [%s]",
                getID(), currHeight, Arrays.toString(b.getHeader().getPrev().toByteArray()),
                Arrays.toString(Objects.requireNonNull(DigestMethod.hash(bc.getBlock(b.getHeader().getHeight() - 1).getHeader().toByteArray())))));
        int prevCid = bc.getBlock(bc.getHeight()).getHeader().getCid();
        int prevCidSeries = bc.getBlock(bc.getHeight()).getHeader().getCidSeries();
// = cids.get(bc.getHeight() - 1);
        ForkProof p = ForkProof.
                newBuilder().
                setCurr(b).
                setCurrSig(rmfServer.getRmfDataSig(b.getHeader().getCidSeries(), b.getHeader().getCid())).
                setPrev(bc.getBlock(bc.getHeight())).
                setPrevSig(rmfServer.getRmfDataSig(prevCidSeries, prevCid)).
                setSender(getID()).
                build();
        synchronized (fp) {
            int pHeight = p.getCurr().getHeader().getHeight();
            if (!fp.containsKey(pHeight)) {
                fpEntry fpe = new fpEntry();
                fpe.done = false;
                fpe.fp = p;
                fp.put(pHeight, fpe);
            }
        }
        panicRB.broadcast(p.toByteArray(), getID());
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
        if (validateForkProof(p) == -1) return;
        int fpoint = p.getCurr().getHeader().getHeight();
        logger.info(format("[#%d] handleFork has been called", getID()));
        try {
            sync(fpoint);
            synchronized (fp) {
                fp.get(fpoint).done = true;
            }
        } catch (Exception e) {
            logger.error(format("[#%d]", getID()), e);
        }

    }

    private int disseminateChainVersion(int forkPoint) {
        subChainVersion.Builder sv = subChainVersion.newBuilder();
            int low = forkPoint - f;
            int high = bc.getHeight() + 1;
            for (Block b : bc.getBlocks(low, high)) {
                int cid = b.getHeader().getCid();
                int cidSeries = b.getHeader().getCidSeries();
                proofedBlock pb = proofedBlock.newBuilder().
                        setCid(cid).
                        setSig(rmfServer.getRmfDataSig(cidSeries, cid)).
                        setB(b).
                        build();
                sv.addV(pb);
            }
            sv.setSuggested(1);
            sv.setSender(getID());
            sv.setForkPoint(forkPoint);
        return syncRB.broadcast(sv.build().toByteArray(), getID());
    }

    private boolean validateSubChainVersion(subChainVersion v, int forkPoint) {
        int lowIndex = v.getV(0).getB().getHeader().getHeight();
        if (lowIndex != forkPoint - f) {
            logger.info(format("[#%d] invalid #1 [fp=%d]]", getID(), forkPoint));
            return false;
        }
        for (proofedBlock pb : v.getVList()) {
//            Block bAsInRmf = pb.getB();
//            bAsInRmf = bAsInRmf.toBuilder().
//                    setHeader(bAsInRmf.getHeader().toBuilder().setCid(0).setCidSeries(0).build()).
//                    build();
            if (!blockDigSig.verify(pb.getB().getHeader().getCreatorID(),
                    pb.getB().getHeader().getCidSeries(), pb.getB().getHeader().getCid(), pb.getSig(), pb.getB())) {
                logger.info(format("[#%d] invalid #2 [fp=%d]]", getID(), forkPoint));
                return false;
            }
        }
        if (v.getVList().size() < f) {
            logger.info(format("[#%d] invalid #3 [fp=%d]]", getID(), forkPoint));
            return false;
        }
        blockchain lastSyncBC = getBC(0, lowIndex); // TODO: Should be more generic (configuration file and reflection??)
        for (proofedBlock pb : v.getVList()) {
            Block curr = pb.getB();
            if (!lastSyncBC.validateBlockData(curr)) {
                logger.info(format("[#%d] invalid #4 [fp=%d]]", getID(), forkPoint));
                return false;
            }
            if (!lastSyncBC.validateBlockHash(curr)) {
                logger.info(format("[#%d] invalid #5 [fp=%d]]", getID(), forkPoint));
                return false;
            }
            if (!lastSyncBC.validateBlockCreator(curr, f)) {
                logger.info(format("[#%d] invalid #6 [fp=%d]]", getID(), forkPoint));
                return false;
            }
            lastSyncBC.addBlock(curr);
        }
        return true;
    }

    private void sync(int forkPoint) throws InvalidProtocolBufferException {
        logger.info(format("[#%d] start sync method with [fp=%d]", getID(), forkPoint));
        disseminateChainVersion(forkPoint);
        while (!scVersions.containsKey(forkPoint) || scVersions.get(forkPoint).size() < 2*f + 1) {
            subChainVersion v;
            try {
                v = subChainVersion.parseFrom(syncRB.deliver());
            } catch (InterruptedException e) {
                if (!stopped) {
                    logger.info(format("[#%d] sync operation has been interrupted, try again...", getID()));
                    continue;
                } else {
                    return;
                }
            }
            if (v == null) {
                logger.info(format("[#%d Unable to parse sub chain version [fp=%d]]", getID(), forkPoint));
                continue;
            }
            if (!validateSubChainVersion(v, v.getForkPoint())) {
                logger.info(format("[#%d] Sub chain version is invalid [fp=%d]]", getID(), v.getForkPoint()));
                continue;
            }
            ArrayList<subChainVersion> l = new ArrayList<>();
            if (!scVersions.containsKey(v.getForkPoint())) {
                scVersions.put(v.getForkPoint(), l);
            }
            scVersions.get(v.getForkPoint()).add(v);
        }
        int max = scVersions.get(forkPoint).
                stream().
                mapToInt(v -> v.getV(v.getVCount() - 1).getB().getHeader().getHeight()).
                max().getAsInt();
        subChainVersion choosen = scVersions.
                get(forkPoint).
                stream().
                filter(v -> v.getV(v.getVCount() - 1).getB().getHeader().getHeight() == max).
                findFirst().get();
        logger.info(format("[#%d] adopt a version of [length=%d] from [#%d]", getID(), choosen.getVList().size(), choosen.getSender()));
        synchronized (newBlockNotifyer) {
            bc.setBlocks(choosen.getVList().stream().map(proofedBlock::getB).collect(Collectors.toList()), forkPoint - 1);
            if (!bc.validateBlockHash(bc.getBlock(bc.getHeight())) ||
                    !bc.validateBlockData(bc.getBlock(bc.getHeight()))) {
                logger.info(format("[#%d] deleted a block [height=%d]", getID(), bc.getHeight()));
                bc.removeBlock(bc.getHeight()); // meant to handle the case in which the front is split between the to leading blocks
            }
            newBlockNotifyer.notify();
        }
        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getCreatorID() + 1) % n;
        currHeight = bc.getHeight() + 1;
        cid = 0;
        cidSeries++;
        logger.info(format("[#%d] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), currHeight, currLeader, cidSeries));
    }

}
