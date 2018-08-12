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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class bcServer extends Node {
    class fpEntry {
        ForkProof fp;
        boolean done;
    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bcServer.class);
    protected RmfNode rmfServer;
//    private syncService syncServ;
    protected RBrodcastService panicRB;
    protected RBrodcastService syncRB;
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
    int cid = 0;
    int cidSeries = 0;
//    protected final List<Integer> cids;
    protected final HashMap<Integer, fpEntry> fp;
    protected HashMap<Integer, ArrayList<subChainVersion>> scVersions;
    final Object panicLock = new Object();

    // TODO: Currently nodes, f, tmoInterval, tmo and configHome are coded but we will turn it into configuration file
    public bcServer(String addr, int rmfPort, int syncPort, int id) {

        super(addr, rmfPort, syncPort, id); // TODO: Should be changed according to Config!
        this.f = Config.getF();
        n = Config.getN();
        rmfServer = new RmfNode(id, addr, rmfPort, Config.getF(),
                Config.getCluster(), Config.getRMFbbcConfigHome());
//        this.syncServ = new syncService(getID(), f, syncPort, Config.getCluster());
        panicRB = new RBrodcastService(id, Config.getPanicRBConfigHome());
        syncRB = new RBrodcastService(id, Config.getSyncRBConfigHome());
        bc = initBC(id);
        currBlock = bc.createNewBLock();

        stopped = false;
        currHeight = 1; // starts from 1 due to the genesis block
        currLeader = 0;
        tmo =  Config.getTMO();
        tmoInterval = Config.getTMOInterval();
        initTmo = tmo;
        this.maxTransactionInBlock = Config.getMaxTransactionsInBlock();
//        cids = new ArrayList<>();
        fp = new HashMap<>();
        scVersions = new HashMap<>();
        mainThread = new Thread(this::mainLoop);
        panicThread = new Thread(this::deliverForkAnnounce);

    }

    public void start() {
        rmfServer.start();
        logger.info(format("[#%d] rmf server is up", getID()));
//        syncServ.start();
        syncRB.start();
        logger.info(format("[#%d] sync server is up", getID()));
//        syncRB.start();
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
//        syncServ.shutdown();
        panicRB.shutdown();
        syncRB.shutdown();
        panicThread.interrupt();
        try {
            panicThread.join();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
//        syncRB.shutdown();
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
                    }

                }
            }
//            synchronized (panicLock) {
//            byte[] panicMsg = panicRB.nonBlockingDeliver();
//            if (panicMsg != null) {
//                try {
//                    ForkProof p = ForkProof.parseFrom(panicMsg);
//                    handleFork(p);
//                } catch (InvalidProtocolBufferException e) {
//                    logger.warn("", e);
//                    continue;
//                }
//            }

            try {
                leaderImpl();
            } catch (InterruptedException e) {
                logger.info(format("[#%d] main thread has been interrupted on leader impl", getID()));
                continue;
            }
            RmfResult msg = null;
            try {
                msg = rmfServer.deliver(cidSeries, cid, currHeight, currLeader, tmo);
            } catch (InterruptedException e) {
                logger.info(format("[#%d] main thread has been interrupted on rmf deliver", getID()));
                continue;
            }
//                if (new String(msg.getData().toByteArray()).equals("I")) { // rmf server was interrupted (possibly by the panic mechanism)
//                    continue;
//                }
                byte[] recData = msg.getData().toByteArray();
                int cid = msg.getCid();
                int cidSeries = msg.getCidSeries();
//                if (cid == -1) { // TODO: What happens if Byz process sends a cid = -1 (to part of the network)
//                    logger.info(format("[#%d] deliver was interrupted", getID()));
//                    continue;
//                }
                if (recData.length == 0) {
                    tmo += tmoInterval;
                    logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
                    updateLeaderAndHeight();
                    continue;
                }
                Block recBlock;
                try {
                    recBlock = Block.parseFrom(recData);
                } catch (InvalidProtocolBufferException e) {
                    logger.warn("Unable to parse received block", e);
                    updateLeaderAndHeight();
                    continue;
                    // TODO: Here also we should create an empty block!
                }
                // TODO: validate meta info (if the meta isn't match we should treat it as a Byz behaviour
//                if (recBlock.getHeader().getHeight() != currHeight ||
//                        recBlock.getHeader().getCreatorID() != currLeader ||
                // TODO: This may cause a problem for the fork proof! (the siganture isn't the original one, as the data) we should handle it carefully
                if(!bc.validateBlockData(recBlock)) {
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
//                recBlock.getHeader().toBuilder().setCid(cid).build();
                recBlock = recBlock.
                        toBuilder().
                        setHeader(recBlock.
                                getHeader().
                                toBuilder().
                                setCid(cid).
                                setCidSeries(cidSeries).
                                build()).
                        build();
                if (!bc.validateBlockHash(recBlock)) {
                    announceFork(recBlock);
                    continue;
                }
                if (!bc.validateBlockCreator(recBlock, f)) {
                    updateLeaderAndHeight();
                    tmo += tmoInterval;
                    continue;
                }
//                if (bc.getHeight() + 1 >= f && bc.getBlocks(bc.getHeight() +1 - f, bc.getHeight() + 1).
//                        stream().
//                        map(b -> b.getHeader().getCreatorID()).
//                        collect(Collectors.toList()).
//                        contains(recBlock.getHeader().getCreatorID())) {
//                    logger.info(format("[#%d] Unable to receive block, sender is in the last f blocks", getID()));
//                    tmo += tmoInterval;
//                    logger.info(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
////                    updateLeader();
//                    continue;
//                }


                tmo = initTmo;
                synchronized (newBlockNotifyer) {
//                    synchronized (cids) {
//                        cids.add(cid);
                    bc.addBlock(recBlock);
                    logger.info(String.format("[#%d] adds new block with [height=%d] [cidSeries=%d ; cid=%d]",
                            getID(), recBlock.getHeader().getHeight(), cidSeries, cid));
                    newBlockNotifyer.notify();
//                    }
                }
                updateLeaderAndHeight();

//                currHeight++;
//                updateLeader();
//            currHeight = bc.getHeight() + 1;
//            currLeader = bc.getBlock(currHeight - 1).getHeader().getCreatorID() + 1 % n;
            }
//        }
    }

    abstract void leaderImpl() throws InterruptedException;

    abstract blockchain initBC(int id);

    abstract blockchain getBC(int start, int end);


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

    private int validateForkProof(ForkProof p)  {
        Block curr = p.getCurr();
        Block prev = p.getPrev();
        int prevBlockH = prev.getHeader().getHeight();
//        if (fp.containsKey(currBlockH)) {
//            return -1;
//        }
//        fpEntry fpe = new fpEntry();
//        fpe.fp = p;
//        fpe.done = false;
//        fp.put(currBlockH, fpe);
//        if (prevBlockH >= currHeight) {
//            return -1;
//        }

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

        Block currAsInRmf = curr;
        currAsInRmf = currAsInRmf.toBuilder().
                setHeader(curr.getHeader().toBuilder().setCid(0).setCidSeries(0).build()).
                build();
        if (!blockDigSig.verify(curr.getHeader().getCreatorID(), p.getCurr().getHeader().getCidSeries()
                , p.getCurr().getHeader().getCid(), p.getCurrSig(), currAsInRmf)) {
            return -1;
        }

        Block prevAsInRmf = prev;
        prevAsInRmf = prevAsInRmf.toBuilder().
                setHeader(prev.getHeader().toBuilder().setCid(0).setCidSeries(0).build()).
                build();
        if (!blockDigSig.verify(prev.getHeader().getCreatorID(), p.getPrev().getHeader().getCidSeries(),
                p.getPrev().getHeader().getCid(), p.getPrevSig(), prevAsInRmf)) {
//            blockDigSig.verify(prev.getHeader().getCreatorID(), p.getPrev().getHeader().getCid(), p.getPrevSig(), prevAsInRmf);
                return -1;
        }

        logger.info(format("[#%d] panic for fork is valid [fp=%d]", getID(), p.getCurr().getHeader().getHeight()));
        return prev.getHeader().getHeight();
    }

    private void deliverForkAnnounce() {
        while (!stopped) {
            ForkProof p = null;
            try {
                p = ForkProof.parseFrom(panicRB.deliver());
            } catch (Exception e) {
                logger.error(format("[#%d]", getID()), e);
                continue;
            }
//            if (p.getSender() == getID()) return;
//            if (validateForkProof(p) == -1) {
//                logger.info(format("[#%d] Invalid panic proof", getID()));
//                return;
//            }
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
//        handleFork(p);
//        fp.get(currHeight).done = true;
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

    int disseminateChainVersion(int forkPoint) {
        subChainVersion.Builder sv = subChainVersion.newBuilder();
//        if (fs.stream().filter(s -> s.getHeight() == bc.getHeight()).count() < f + 1) {
//            sv.setSuggested(0);
//        } else {
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
//            for (frontSupport s : fs.stream().filter(s -> s.getHeight() == bc.getHeight() ||
//                    s.getHeight() + 1 == bc.getHeight()).collect(Collectors.toList())) {
//                sv.addFp(s);
//            }
//        }
//        sv.setForkPoint(forkPoint);
        return syncRB.broadcast(sv.build().toByteArray(), getID());
    }

    boolean validateSubChainVersion(subChainVersion v, int forkPoint) {
        int lowIndex = v.getV(0).getB().getHeader().getHeight();
//            int highIndex = v.getV(v.getVCount() - 1).getB().getHeader().getHeight();
        if (lowIndex != forkPoint - f) {
            logger.info(format("[#%d] invalid #1 [fp=%d]]", getID(), forkPoint));
            return false;
        }
//            if (highIndex != forkPoint) return false;
        for (proofedBlock pb : v.getVList()) {
            Block bAsInRmf = pb.getB();
            bAsInRmf = bAsInRmf.toBuilder().
                    setHeader(bAsInRmf.getHeader().toBuilder().setCid(0).setCidSeries(0).build()).
                    build();
            if (!blockDigSig.verify(pb.getB().getHeader().getCreatorID(),
                    pb.getB().getHeader().getCidSeries(), pb.getB().getHeader().getCid(), pb.getSig(), bAsInRmf)) {
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
//                int bCreator = curr.getHeader().getCreatorID();
//                if (!blockDigSig.verify(bCreator, curr.getHeader().getCid(), pb.getSig(), curr))
//                    return false;
            if (!lastSyncBC.validateBlockCreator(curr, f)) {
                logger.info(format("[#%d] invalid #6 [fp=%d]]", getID(), forkPoint));
                return false;
            }
            lastSyncBC.addBlock(curr);
        }
        return true;
    }

    void sync(int forkPoint) throws InvalidProtocolBufferException {
        logger.info(format("[#%d] start sync method with [fp=%d]", getID(), forkPoint));
        int height = bc.getHeight();
//        frontSupport fs = frontSupport.newBuilder().
//                setHeight(height).
//                setId(getID()).build();
//        int sid = syncServ.broadcastSupport(fs);
//        ArrayList<frontSupport> lfs = syncServ.deliver(height, sid);
        disseminateChainVersion(forkPoint);
        while (!scVersions.containsKey(forkPoint) || scVersions.get(forkPoint).size() < 2*f + 1) {
            subChainVersion v = null;
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
//        if (choosen.
//                getV(choosen.getVCount() - 1).
//                getB().
//                getHeader().
//                getHeight() + 1 == bc.getHeight()) {
//            logger.info(format("[#%d] deleted a block [height=%d]", getID(), bc.getHeight()));
//            bc.removeBlock(bc.getHeight()); // meant to handle the case in which the front is split between the to leading blocks
//        }
        logger.info(format("[#%d] adopt a version of [length=%d] from [#%d]", getID(), choosen.getVList().size(), choosen.getSender()));
        synchronized (newBlockNotifyer) {
            bc.setBlocks(choosen.getVList().stream().map(proofedBlock::getB).collect(Collectors.toList()), forkPoint - 1);
            if (!bc.validateBlockHash(bc.getBlock(bc.getHeight())) ||
                    !bc.validateBlockData(bc.getBlock(bc.getHeight()))) {
                logger.info(format("[#%d] deleted a block [height=%d]", getID(), bc.getHeight()));
                bc.removeBlock(bc.getHeight()); // meant to handle the case in which the front is split between the to leading blocks
            }
            newBlockNotifyer.notify();
//                    }
        }
//        bc.setBlocks(choosen.getVList().stream().map(proofedBlock::getB).collect(Collectors.toList()), forkPoint - 1);
//        if (!bc.validateBlockHash(bc.getBlock(bc.getHeight())) ||
//                !bc.validateBlockData(bc.getBlock(bc.getHeight()))) {
//            logger.info(format("[#%d] deleted a block [height=%d]", getID(), bc.getHeight()));
//            bc.removeBlock(bc.getHeight()); // meant to handle the case in which the front is split between the to leading blocks
////            rmfServer.updateCid(bc.getBlock(bc.getHeight()).getHeader().getCid() + 1);
//        }
        //        rmfServer.updateCid(bc.getBlock(bc.getHeight()).getHeader().getCid() + 2);
        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getCreatorID() + 1) % n;
        currHeight = bc.getHeight() + 1;
        cid = 0;
        cidSeries++;
//        rmfServer.cleanBuffers();
        logger.info(format("[#%d] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), currHeight, currLeader, cidSeries));
    }

}
