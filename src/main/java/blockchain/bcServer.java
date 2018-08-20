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
    blockchain bc;
    int currHeight;
    protected int f;
    protected int n;
    int currLeader;
    protected boolean stopped;
//    final Object blockLock = new Object();
    block currBlock;
    private final Object newBlockNotifyer = new Object();
    private int maxTransactionInBlock;
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

    bcServer(String addr, int rmfPort, int id) {

        super(addr, rmfPort, id);
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
        logger.debug(format("[#%d] rmf server is up", getID()));
        syncRB.start();
        logger.debug(format("[#%d] sync server is up", getID()));
        panicRB.start();
        logger.debug(format("[#%d] panic server is up", getID()));
        logger.info(format("[#%d] is up", getID()));
    }

    public void serve() {
        panicThread.start();
        logger.debug(format("[#%d] starts panic thread", getID()));
        mainThread.start();
        logger.debug(format("[#%d] starts main thread", getID()));
        logger.info(format("[#%d] starts serving", getID()));
    }

    public void shutdown() {
        stopped =  true;
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d]", getID()), e);
        }
        rmfServer.stop();
        panicRB.shutdown();
        syncRB.shutdown();
        panicThread.interrupt();
        try {
            panicThread.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d]", getID()), e);
        }
        logger.info(format("[#%d] shutdown bc server", getID()));
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
                        logger.debug(format("[#%d] have found panic message [height=%d] [fp=%d]", getID(), currHeight, key));
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
                logger.debug(format("[#%d] main thread has been interrupted on leader impl", getID()));
                continue;
            }
            byte[][] pmsg;
            try {
                pmsg = rmfServer.deliver(cidSeries, cid, currHeight, currLeader, tmo, next);
            } catch (InterruptedException e) {
                logger.debug(format("[#%d] main thread has been interrupted on rmf deliver", getID()));
                continue;
            }
            byte[] msg = pmsg[0];
            fastMode = configuredFastMode;
//            byte[] recData = msg.getData().toByteArray();
//            int mcid = msg.getCid();
//            int mcidSeries = msg.getCidSeries();
            if (msg == null) {
                tmo += tmoInterval;
                logger.debug(format("[#%d] Unable to receive block, timeout increased to [%d] ms", getID(), tmo));
                updateLeaderAndHeight();
                fastMode = false;
                cid++;
                continue;
            }
            if (currLeader == getID()) {
                logger.debug(format("[#%d] nullifies currBlock [height=%d] [cidSeries=%d, cid=%d]", getID(), currHeight,
                        cidSeries, cid));
                currBlock = null;
            }
            String msgSign = Base64.getEncoder().encodeToString(pmsg[1]);
            Block recBlock;
            try {
                recBlock = Block.parseFrom(msg);
            } catch (InvalidProtocolBufferException e) {
                logger.warn("Unable to parse received block", e);
//                updateLeaderAndHeight();
//                continue;
                recBlock = Block.newBuilder()
                        .setHeader(BlockHeader.newBuilder()
                                .setCreatorID(currLeader)
                                .setHeight(currHeight)
                                .setCidSeries(cidSeries)
                                .setCid(cid)
                                .setPrev(ByteString.copyFrom(new byte[0]))
                                .setProof(msgSign)
                                .build())
                        .setOrig(ByteString.copyFrom(msg))
                        .build();
                /*
                    We should create an empty block to the case in which a byzantine leader sends valid block to
                    part of the network and invalid one to the other part.
                    But, creating an empty block requires to change the fork proof mechanism (as the signature is not the
                    original one). Hence currently we leave it to a later development.
                 */
            }
//            recBlock = recBlock
//                    .toBuilder()
//                    .setHeader(recBlock
//                            .getHeader()
//                            .toBuilder()
////                            .setProof(msgSign)
//                            .build())
//                    .build();
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
                logger.debug(String.format("[#%d] adds new block with [height=%d] [cidSeries=%d ; cid=%d]",
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
            logger.debug(format("[#%d] adds transaction from [client=%d]", getID(), t.getClientID()));
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
                    logger.debug(format("[#%d] detects an invalid transaction from [client=%d]", getID(), t.getClientID()));
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
            logger.debug(format("[#%d] invalid fork proof #1", getID()));
            return -1;
        }

        int currCreator = currLeader;
        if (bc.getHeight() >= curr.getHeader().getHeight()) {
            currCreator = bc.getBlock(curr.getHeader().getHeight()).getHeader().getCreatorID();
        }
        if (currCreator != curr.getHeader().getCreatorID()) {
            logger.debug(format("[#%d] invalid fork proof #2", getID()));
            return -1;
        }
        if (!curr.getOrig().isEmpty()) {
            Data asInRmf = Data
                    .newBuilder()
                    .setMeta(Meta
                            .newBuilder()
                          //  .setHeight(curr.getHeader().getHeight())
                            .setSender(curr.getHeader().getCreatorID())
                            .setCidSeries(curr.getHeader().getCidSeries())
                            .setCid(curr.getHeader().getCid())
                            .build())
                    .setData(curr.getOrig())
                    .setSig(curr.getHeader().getProof())
                    .build();
            if (!rmfDigSig.verify(curr.getHeader().getCreatorID(), asInRmf)) {
                logger.debug(format("[#%d] invalid fork proof #6", getID()));
                return -1;
            }
        } else if (!blockDigSig.verify(curr.getHeader().getCreatorID(), curr)) {
            logger.debug(format("[#%d] invalid fork proof #3", getID()));
            return -1;
        }

        if (!blockDigSig.verify(prev.getHeader().getCreatorID(), prev)) {
            logger.debug(format("[#%d] invalid fork proof #4", getID()));
            return -1;
        }

        logger.debug(format("[#%d] panic for fork is valid [fp=%d]", getID(), p.getCurr().getHeader().getHeight()));
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
                    logger.debug(format("[#%d] interrupts the main thread", getID()));
                    mainThread.interrupt();
                }
            }
        }
    }
    private void announceFork(Block b) {
        logger.warn(format("[#%d] possible fork! [height=%d]",
                getID(), currHeight));
//        int prevCid = bc.getBlock(bc.getHeight()).getHeader().getCid();
//        int prevCidSeries = bc.getBlock(bc.getHeight()).getHeader().getCidSeries();
        ForkProof p = ForkProof.
                newBuilder().
                setCurr(b).
//                setCurrSig(rmfServer.getRmfDataSig(b.getHeader().getCidSeries(), b.getHeader().getCid())).
                setPrev(bc.getBlock(bc.getHeight())).
//                setPrevSig(rmfServer.getRmfDataSig(prevCidSeries, prevCid)).
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
                    logger.error(format("[#%d] Servers has been interrupted while trying to deliver [index=%d]",
                            getID(), index), e);
                    return null;
                }
            }
        }
        return bc.getBlock(index);
    }

    private void handleFork(ForkProof p) {
        if (validateForkProof(p) == -1) return;
        int fpoint = p.getCurr().getHeader().getHeight();
        logger.debug(format("[#%d] handleFork has been called", getID()));
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
            int low = Math.max(forkPoint - f, 1);
            int high = bc.getHeight() + 1;
            for (Block b : bc.getBlocks(low, high)) {
//                int cid = b.getHeader().getCid();
//                int cidSeries = b.getHeader().getCidSeries();
//                proofedBlock pb = proofedBlock.newBuilder().
////                        setCid(cid).
//                        setSig(rmfServer.getRmfDataSig(cidSeries, cid)).
//                        setB(b).
//                        build();
                sv.addV(b);
            }
            sv.setSuggested(1);
            sv.setSender(getID());
            sv.setForkPoint(forkPoint);
        return syncRB.broadcast(sv.build().toByteArray(), getID());
    }

    private boolean validateSubChainVersion(subChainVersion v, int forkPoint) {
        int lowIndex = v.getV(0).getHeader().getHeight();
        if (lowIndex != forkPoint - f && forkPoint >= f) {
            logger.debug(format("[#%d] invalid sub chain version, [lowIndex=%d != forkPoint -f=%d] [fp=%d ; sender=%d]",
                    getID(), lowIndex, forkPoint - f, forkPoint, v.getSender()));
            return false;
        }

        if (v.getVList().size() < f && forkPoint >= f) {
            logger.debug(format("[#%d] invalid sub chain version, block list size is smaller then f [size=%d] [fp=%d]",
                    getID(), v.getVList().size(), forkPoint));
            return false;
        }

        blockchain lastSyncBC = getBC(0, lowIndex);
        for (Block pb : v.getVList()) {
//            Block b = pb;
//            if (b.hasOrig()) {
//                b = b.getOrig(); // To handle self created empty block
//            }
            if (!blockDigSig.verify(pb.getHeader().getCreatorID(), pb)) {
                logger.debug(format("[#%d] invalid sub chain version, block [height=%d] digital signature is invalid " +
                        "[fp=%d ; sender=%d]", getID(), pb.getHeader().getHeight(), forkPoint, v.getSender()));
                return false;
            }
            //            if (!lastSyncBC.validateBlockData(curr)) {
//                logger.debug(format("[#%d] invalid sub chain version, block data is invalid [height=%d] [fp=%d]",
//                        getID(), curr.getHeader().getHeight(), forkPoint));
//                return false;
//            }
            if (!lastSyncBC.validateBlockHash(pb)) {
                logger.debug(format("[#%d] invalid sub chain version, block hash is invalid [height=%d] [fp=%d]",
                        getID(), pb.getHeader().getHeight(), forkPoint));
                return false;
            }
            if (!lastSyncBC.validateBlockCreator(pb, f)) {
                logger.debug(format("[#%d] invalid invalid sub chain version, block creator is invalid [height=%d] [fp=%d]",
                        getID(), pb.getHeader().getHeight(), forkPoint));
                return false;
            }
            lastSyncBC.addBlock(pb);
        }
        return true;
    }

    private void sync(int forkPoint) throws InvalidProtocolBufferException {
        logger.debug(format("[#%d] start sync method with [fp=%d]", getID(), forkPoint));
        disseminateChainVersion(forkPoint);
        while (!scVersions.containsKey(forkPoint) || scVersions.get(forkPoint).size() < 2*f + 1) {
            subChainVersion v;
            try {
                v = subChainVersion.parseFrom(syncRB.deliver());
            } catch (InterruptedException e) {
                if (!stopped) {
                    // might interrupted if more then one panic message received.
                    logger.debug(format("[#%d] sync operation has been interrupted, try again...", getID()));
                    continue;
                } else {
                    return;
                }
            }
            if (v == null) {
                logger.debug(format("[#%d Unable to parse sub chain version [fp=%d]]", getID(), forkPoint));
                continue;
            }
            if (!validateSubChainVersion(v, v.getForkPoint())) {
                logger.debug(format("[#%d] Sub chain version is invalid [fp=%d]]", getID(), v.getForkPoint()));
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
                mapToInt(v -> v.getV(v.getVCount() - 1).getHeader().getHeight()).
                max().getAsInt();
        subChainVersion choosen = scVersions.
                get(forkPoint).
                stream().
                filter(v -> v.getV(v.getVCount() - 1).getHeader().getHeight() == max).
                findFirst().get();
        logger.debug(format("[#%d] adopts sub chain version [length=%d] from [#%d]", getID(), choosen.getVList().size(), choosen.getSender()));
        synchronized (newBlockNotifyer) {
            bc.setBlocks(choosen.getVList(), forkPoint - 1);
            if (!bc.validateBlockHash(bc.getBlock(bc.getHeight()))) { //||
//                    !bc.validateBlockData(bc.getBlock(bc.getHeight()))) {
                logger.debug(format("[#%d] deletes a block [height=%d]", getID(), bc.getHeight()));
                bc.removeBlock(bc.getHeight()); // meant to handle the case in which the front is split between the to leading blocks
            }
            newBlockNotifyer.notify();
        }
        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getCreatorID() + 2) % n;
        currHeight = bc.getHeight() + 1;
        cid = 0;
        cidSeries++;
        logger.debug(format("[#%d] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), currHeight, currLeader, cidSeries));
    }

}
