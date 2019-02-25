package servers;

import blockchain.BaseBlock;
import blockchain.BaseBlockchain;
import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import crypto.blockDigSig;
import das.RBroadcast.RBrodcastService;
import das.wrb.WrbNode;
import proto.Types;
import proto.Types.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static blockchain.BaseBlockchain.validateBlockHash;
import static java.lang.Math.max;
import static java.lang.String.format;

public abstract class ToyBaseServer extends Node {
//    class fpEntry {
//        ForkProof fp;
//        boolean done;
//    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyBaseServer.class);
    WrbNode rmfServer;
    private RBrodcastService panicRB;
    private RBrodcastService syncRB;
    final BaseBlockchain bc;
    int currHeight;
    protected int f;
    protected int n;
    int currLeader;
    final Object forkLock = new Object();
    protected AtomicBoolean stopped = new AtomicBoolean(false);
    BaseBlock currBlock;
    private final Object newBlockNotifyer = new Object();
    private int maxTransactionInBlock;
    private Thread mainThread;
    private Thread panicThread;
    int cid = 0;
    int cidSeries = 0;
//    private final HashMap<Integer, List<ForkProof>> fp;
    private final HashMap<Integer, Boolean> fp;
    private final ConcurrentHashMap<Integer, ArrayList<subChainVersion>> scVersions;
    private final ConcurrentLinkedQueue<Transaction> transactionsPool = new ConcurrentLinkedQueue<>();
    boolean configuredFastMode;
    boolean fastMode;
    int channel;
    boolean testing = Config.getTesting();
    int txPoolMax = 100000;
    int bareTxSize = Transaction.newBuilder()
            .setClientID(0)
            .setId(txID.newBuilder().setTxID(UUID.randomUUID().toString()).build())
            .build().getSerializedSize() + 8;
    int txSize = 0;
    int cID = new Random().nextInt(10000);
    Path sPath = Paths.get("disk", String.valueOf(channel));
//    boolean skipCurrentBlock = false;
    ExecutorService storageWorker = Executors.newSingleThreadExecutor();
//    Semaphore sem = new Semaphore(1);
//    private Block recBlock;
    int syncEvents = 0;



    public ToyBaseServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                         int maxTx, boolean fastMode, ArrayList<Node> cluster,
                         WrbNode rmf, RBrodcastService panic, RBrodcastService sync) {
        super(addr, rmfPort, id);
        this.f = f;
        this.n = 3*f + 1;
        this.panicRB = panic;
        this.syncRB = sync;
        bc = initBC(id, channel);
        currBlock = null;
        currHeight = 1; // starts from 1 due to the genesis BaseBlock
        currLeader = 0;
        this.maxTransactionInBlock = maxTx;
        fp = new HashMap<>();
        scVersions = new ConcurrentHashMap<>();
        mainThread = new Thread(() -> {
            try {
                mainLoop();
            } catch (Exception ex) {
                logger.error(format("[#%d-C[%d]]", getID(), channel), ex);
                shutdown(true);
            }
        });
        panicThread = new Thread(() -> {
            try {
                mainFork();
            } catch (InterruptedException | IOException e) {
                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
            }
        });
        this.configuredFastMode = fastMode;
        this.fastMode = fastMode;
        this.channel = channel;
        rmfServer = rmf;
        currLeader = channel % n;

        if (testing) {
            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
        }
    }

    public ToyBaseServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                         int maxTx, boolean fastMode, ArrayList<Node> cluster,
                         String bbcConfig, String panicConfig, String syncConfig,
                         String serverCrt, String serverPrivKey, String caRoot) {

        super(addr, rmfPort, id);
        this.f = f;
        this.n = 3*f + 1;
        rmfServer = new WrbNode(1, id, addr, rmfPort, f, tmo, tmoInterval,
               cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        panicRB = new RBrodcastService(1, id, panicConfig);
        syncRB = new RBrodcastService(1, id, syncConfig);
        bc = initBC(id, channel);
        currBlock = null;
        currHeight = 1; // starts from 1 due to the genesis BaseBlock
        currLeader = 0;
//        this.tmo =  tmo;
//        this.tmoInterval = tmoInterval;
//        initTmo = tmo;
        this.maxTransactionInBlock = maxTx;
        fp = new HashMap<>();
        scVersions = new ConcurrentHashMap<>();
        mainThread = new Thread(this::mainLoop);
        panicThread = new Thread(() -> {
            try {
                mainFork();
            } catch (InterruptedException | IOException e) {
                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
            }
        });
        this.configuredFastMode = fastMode;
        this.fastMode = fastMode;
        currLeader = channel % n;

        if (testing) {
            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
        }

    }

    public void start(boolean group) {
        if (!group) {
            rmfServer.start();
            logger.debug(format("[#%d-C[%d]] wrb server is up", getID(), channel));
            syncRB.start();
            logger.debug(format("[#%d-C[%d]] sync server is up", getID(), channel));
            panicRB.start();
            logger.debug(format("[#%d-C[%d]] panic server is up", getID(), channel));
        }
        logger.info(format("[#%d-C[%d]] is up", getID(), channel));
    }

    public void serve() {
        panicThread.start();
        logger.debug(format("[#%d-C[%d]] starts panic thread", getID(), channel));
        mainThread.start();
        logger.debug(format("[#%d-C[%d]] starts main thread", getID(), channel));
//        gcThread.start();
//        logger.debug(format("[#%d-C[%d]] gc thread is up", getID(), channel));
        logger.info(format("[#%d-C[%d]] starts serving", getID(), channel));
    }

    public void shutdown(boolean group) {
        stopped.set(true);
        logger.debug(format("[#%d-C[%d]] interrupt main thread", getID(), channel));
//        AtomicBoolean joined = new AtomicBoolean(false);
//        Thread t = new Thread(() -> {
//            while (!joined.get())
//        });
//        t.start();
        AtomicBoolean j = new AtomicBoolean(false);
        long start = System.currentTimeMillis();
//            Object lock = new Object();
        Thread t = new Thread(() -> {
            while (!j.get()) {
                mainThread.interrupt();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            }
        });
        t.start();
        try {
            mainThread.join();
            j.set(true);
//            t.interrupt();
            t.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d-C[%d]]", getID(), channel), e);
        }

        if (!group) {
            if (rmfServer != null) rmfServer.stop();
            if (panicRB != null) panicRB.shutdown();
            if (syncRB != null) syncRB.shutdown();
        }
        logger.debug(format("[#%d-C[%d]] interrupt panic thread", getID(), channel));
        j.set(false);
        t = new Thread(() -> {
            while (!j.get()) {
                panicThread.interrupt();
                try {
                    Thread.sleep( 1000);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            }
        });
        t.start();

        try {
            panicThread.join();
            j.set(true);
            t.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d-C[%d]]", getID(), channel), e);
        }
//        txSem.release(txPoolMax);
        logger.info(format("[#%d-C[%d]] shutdown bc server", getID(), channel));
    }
    private void updateLeaderAndHeight() {
        currHeight = bc.getHeight() + 1;
        currLeader = (currLeader + 1) % n;
        cid++;
    }

     void gc(int index) throws IOException {
        logger.debug(format("[#%d-C[%d]] clear buffers of [height=%d]", getID(), channel, index));
//        if (bc.getBlock(index) == null) return;
        Meta key = bc.getBlock(index).getHeader().getM();
        Meta rKey = Meta.newBuilder()
                .setChannel(key.getChannel())
                .setCidSeries(key.getCidSeries())
                .setCid(key.getCid())
                .build();
        rmfServer.clearBuffers(rKey);
        syncRB.clearBuffers(rKey);
        panicRB.clearBuffers(rKey);
        bc.setBlock(index, null);
    }
    private void checkSyncEvent() {
        synchronized (fp) {
//            if (fp.containsKey(currHeight)
//                    && fp.get(currHeight).size() > 0
//                    && fp.get(currHeight).get(0).equals(ForkProof.getDefaultInstance())) {
//                scVersions.computeIfPresent(currHeight, (k, v) -> {
//                    if (v.size() > 0 && !v.get(0).equals(subChainVersion.getDefaultInstance())) {
//                        try {
//                            adoptSubVersion(currHeight);
//                        } catch (IOException e) {
//                            logger.error(String.format("[#%d-C[%d]] unable to sync", getID(), channel), e);
//                        }
//                    }
//                    return v;
//                });
//            }
            if (fp.containsKey(currHeight) && fp.get(currHeight)) {
                scVersions.computeIfPresent(currHeight, (k, v) -> {
                    if (v.size() > 0 && !v.get(0).equals(subChainVersion.getDefaultInstance())) {
                        try {
                            adoptSubVersion(currHeight);
                        } catch (IOException e) {
                            logger.error(String.format("[#%d-C[%d]] unable to sync", getID(), channel), e);
                        }
                    }
                    return v;
                });
            }
        }
    }
    private void mainLoop() throws RuntimeException {
//        boolean interrupted = false;
        while (!stopped.get()) {
            checkSyncEvent();
            while (!bc.validateCurrentLeader(currLeader, f)) {
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 2; // I think we have no reason to increase CID.
            }
            Block next;
            try {
                    next = leaderImpl();
            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on leader impl",
                        getID(), channel));
                return;
            }
            Block recBlock;
            try {
                long startTime = System.currentTimeMillis();
                recBlock = rmfServer.deliver(channel, cidSeries, cid, currHeight, currLeader, next);
                logger.debug(format("[#%d-C[%d]] deliver took about [%d] ms [cidSeries=%d ; cid=%d]",
                        getID(), channel, System.currentTimeMillis() - startTime, cidSeries, cid));
            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on wrb deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), channel, currHeight, cidSeries, cid));
                return;
            }

            fastMode = configuredFastMode;
            if (recBlock == null) {
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 2;
                continue;
            }

            if (currLeader == getID()) {
                logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
                        getID(), channel, recBlock.getHeader().getM().getSender(), currHeight, cidSeries, cid));
                currBlock = null;
            }
            if (!bc.validateBlockHash(recBlock)) {
                announceFork(recBlock);
                fastMode = false;
                continue;
            }

            synchronized (newBlockNotifyer) {
//                if (skipCurrentBlock) continue;
                recBlock = recBlock
                        .toBuilder()
                        .setSt(recBlock.getSt()
                                .toBuilder()
                                .setChannelDecided(System.currentTimeMillis()))
                        .build();
                bc.addBlock(recBlock);
                if (recBlock.getHeader().getHeight() - (f + 2) > 0) {
                    Block permanent = bc.getBlock(recBlock.getHeader().getHeight() - (f + 2));
                    permanent = permanent.toBuilder().setSt(permanent.getSt().toBuilder().setPd(System.currentTimeMillis())).build();
                    try {
                        bc.setBlock(recBlock.getHeader().getHeight() - (f + 2), permanent);
                    } catch (IOException e) {
                        logger.error(String.format("[#%d-C[%d]] unable to record permanent time for block [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
                                getID(), channel, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));
                    }
                }

                logger.debug(String.format("[#%d-C[%d]] adds new Block with [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
                        getID(), channel, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));
                newBlockNotifyer.notify();

            }
            // TODO: Improve mechanism
//              lastReceivedBlock[recBlock.getHeader().getM().getSender()] = recBlock.getHeader().getHeight();
//              gc();     lastReceivedBlock[recBlock.getHeader().getM().getSender()] = recBlock.getHeader().getHeight();
//                if (bc.getHeight() - f >= 0) {
////                    synchronized (approvedTx) {
////                        addApprovedTransactions(bc.getBlock(bc.getHeight() - f).getDataList());
////                    }
//                }

            if (currLeader == getID()) {
                removeFromProposed(recBlock.getDataList());
            }
            updateLeaderAndHeight();
        }
//        }
    }

    abstract Block leaderImpl() throws InterruptedException;

    abstract public BaseBlockchain initBC(int id, int channel);

    abstract public BaseBlockchain getBC(int start, int end);

    abstract public BaseBlockchain getEmptyBC();

    public int getTxPoolSize() {
        return transactionsPool.size();
    }

    public txID addTransaction(Transaction tx) {
        if (transactionsPool.size() > txPoolMax) return null;
        transactionsPool.add(tx);
        return tx.getId();
    }

    public String addTransaction(byte[] data, int clientID) {
        if (transactionsPool.size() > txPoolMax) return "";
        String txID = UUID.randomUUID().toString();
        Transaction t = Transaction.newBuilder()
                .setClientID(clientID)
                .setId(Types.txID.newBuilder().setTxID(txID).build())
                .setData(ByteString.copyFrom(data))
                .build();
        transactionsPool.add(t);
        return txID;
    }
    private void addApprovedTransactions(List<Transaction> txs) {
//        synchronized (approvedTx) {
//            for (Transaction tx :txs) {
//                approvedTx.put(tx.getTxID(), tx);
//            }
//        }

    }
    public int isTxPresent(String txID) {
//        synchronized (approvedTx) {
//            if (approvedTx.containsKey(txID)) {
//                return 2;
//            }
//        }
////        synchronized (transactionsPool) {
//            if (transactionsPool.stream().map(Transaction::getTxID).anyMatch(tid -> tid.equals(txID))) {
//                return 0;
//            }
//            if (proposedTx.contains(txID)) {
//                return 1;
//            }
////        }
//        ArrayList<Block> cbc;
//        synchronized (bc) {
//            cbc = new ArrayList<Block>(bc.getBlocks(bc.getHeight() - f + 1, bc.getHeight() + 1));
//        }
//        if (cbc.stream()
//                .map(Block::getDataList)
//                .anyMatch(tl -> tl
//                        .stream()
//                        .map(Transaction::getTxID)
//                        .anyMatch(tid -> tid.equals(txID)))) {
//            return 3;
//        }
        return -1;
    }

    void createTxToBlock() {
        while (currBlock.getTransactionCount() < maxTransactionInBlock) {
            long ts = System.currentTimeMillis();
            SecureRandom random = new SecureRandom();
            byte[] tx = new byte[txSize];
            random.nextBytes(tx);
            currBlock.addTransaction(Transaction.newBuilder()
                    .setId(txID.newBuilder().setTxID(UUID.randomUUID().toString()).build())
                    .setClientID(cID)
                    .setClientTs(ts)
                    .setServerTs(ts)
                    .setData(ByteString.copyFrom(tx))
                    .build());
        }

    }

    void addTransactionsToCurrBlock() {
        if (currBlock != null) {
            logger.debug(format("[#%d-C[%d]] block is already build",
                    getID(), channel));
            return;
        }
        currBlock = bc.createNewBLock();
        currBlock.blockBuilder.setSt(blockStatistics
                .newBuilder()
                .build());
            while ((!transactionsPool.isEmpty()) && currBlock.getTransactionCount() < maxTransactionInBlock) {
                Transaction t = transactionsPool.poll();
                if (!currBlock.validateTransaction(t)) {
                    logger.debug(format("[#%d-C[%d]] detects an invalid transaction from [client=%d]",
                            getID(), channel, t.getClientID()));
                    continue;
                }
                currBlock.addTransaction(t);
            }
            if (testing) {
                createTxToBlock();
            }
    }

    void removeFromProposed(List<Transaction> txs) {
//        proposedTx.removeAll(txs.stream().map(Transaction::getTxID).collect(Collectors.toList()));
    }

    private boolean validateForkProof(ForkProof p)  {
        logger.debug(format("[#%d-C[%d]] starts validating fp", getID(), channel));
        Block curr = p.getCurr();
        Block prev = p.getPrev();
        if (!blockDigSig.verify(curr.getHeader().getM().getSender(), curr)) {
                logger.debug(format("[#%d-C[%d]] invalid fork proof #3", getID(), channel));
                return false;
        }
        if (!blockDigSig.verify(prev.getHeader().getM().getSender(), prev)) {
            logger.debug(format("[#%d-C[%d]] invalid fork proof #4", getID(), channel));
            return false;
        }

        logger.debug(format("[#%d-C[%d]] panic for fork is valid [fp=%d]", getID(), channel, p.getCurr().getHeader().getHeight()));
        return true;
    }

    private void mainFork() throws InterruptedException, IOException {
        while (!stopped.get()) {
            ForkProof p;
            try {
                p = ForkProof.parseFrom(panicRB.deliver(channel));
            } catch (Exception e) {
                logger.error(format("[#%d-C[%d]] unable to parse fork message", getID(), channel), e);
                continue;
            }

            int forkPoint = p.getCurr().getHeader().getHeight();

//            if (fp.containsKey(forkPoint) && fp.get(forkPoint).size() > 0
//                    && fp.get(forkPoint)
//                    .get(0)
//                    .equals(ForkProof.getDefaultInstance()))
            if (fp.containsKey(forkPoint) && fp.get(forkPoint))
            {

                logger.debug(format("[#%d-C[%d]] already synchronized for this point [r=%d]",
                        getID(), channel, forkPoint));
                continue;
            }

            if (!validateForkProof(p)) {
                logger.debug(format("[#%d-C[%d]] invalid fork proof for [r=%d]"
                        , getID(), channel, forkPoint));
                continue;
            }

            if (forkPoint - (f + 1) <= currHeight) {
                logger.debug(format("[#%d-C[%d]] interrupting main thread [r=%d]"
                        , getID(),channel, forkPoint));
                mainThread.interrupt();
                mainThread.join();
            }
            synchronized (fp) {
                if (!fp.containsKey(forkPoint)) {
                    fp.put(forkPoint, false);
                }
//                if (fp.get(forkPoint).size() > 0) {
//                    logger.debug(format("[#%d-C[%d]] already has this proof [r=%d]",
//                            getID(), channel, forkPoint));
//                    continue;
//                }
//                fp.get(forkPoint).add(p);
                syncEvents++;
                sync(forkPoint);
            }
        }
    }
    private void announceFork(Block b) {
        logger.info(format("[#%d-C[%d]] possible fork! [height=%d]",
                getID(), channel, currHeight));
        ForkProof p = ForkProof.
                newBuilder().
                setCurr(b).
                setPrev(bc.getBlock(bc.getHeight())).
                setSender(getID()).
                build();
        panicRB.broadcast(p.toByteArray(), channel, getID());
    }

    public Block deliver(int index) throws InterruptedException {
        synchronized (newBlockNotifyer) {
            while (index >= bc.getHeight() - (f + 1)) {
                newBlockNotifyer.wait();
            }
        }
        return bc.getBlock(index);
    }

    public Block nonBlockingdeliver(int index) {
        synchronized (newBlockNotifyer) {
            if (index >= bc.getHeight() - (f + 1)) {
                return null;
            }
        }
        return bc.getBlock(index);
    }

    public int bcSize() {
        return max(bc.getHeight() - (f + 2), 0);
    }

//    private void handleFork(ForkProof p, int forkPoint) {
//        logger.debug(format("[#%d-C[%d]] handleFork invoked [r=%d]"
//                , getID(), channel, forkPoint));
//            try {
//                sync(forkPoint);
//            } catch (Exception e) {
//                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
//            }
//    }

    public int getSyncEvents() {
        return syncEvents;
    }
    private int disseminateChainVersion(int forkPoint) throws IOException {
        subChainVersion.Builder sv = subChainVersion.newBuilder();
        if (currHeight < forkPoint - 1) {
            logger.debug(format("[#%d-C[%d]] too far behind... [r=%d ; curr=%d]"
                    , getID(), channel, forkPoint, currHeight));
        } else {
            int low = max(forkPoint - (f + 1), 0);
            int high = bc.getHeight() + 1;
            logger.debug(format("[#%d-C[%d]] building a sub version [(inc) %d --> (exc) %d]"
                    , getID(), channel, low, high));
            for (Block b : bc.getBlocks(low, high)) {
                sv.addV(b);
            }
            sv.setSuggested(1);
            sv.setSender(getID());
            sv.setForkPoint(forkPoint);
        }
        return syncRB.broadcast(sv.build().toByteArray(), channel, getID());
    }

    /*
    The only thing this method do is to check that a given subversion
    is valid with respect to itself.
     */
    private boolean validateSubChainVersion(subChainVersion v, int forkPoint) {
        BaseBlockchain subV = getEmptyBC();
        subV.addBlock(v.getV(0));
        for (int i = 0 ; i < v.getVList().size() ; i++ ) {
            Block pb = v.getV(i);
            if (!blockDigSig.verify(pb.getHeader().getM().getSender(), pb)) {
                logger.debug(format("[#%d-C[%d] #1] invalid sub chain version, block [height=%d] digital signature is invalid " +
                        "[fp=%d ; sender=%d]", getID(),channel, pb.getHeader().getHeight(), forkPoint, v.getSender()));
                return false;
            }
            if (subV.getHeight() > 0) {
                if (!validateBlockHash(v.getV(i - 1), pb)) {
                    logger.debug(format("[#%d-C[%d] #1] invalid sub chain version, block hash is invalid [height=%d] [fp=%d]",
                            getID(),channel, pb.getHeader().getHeight(), forkPoint));
                    return false;
                }
            }
            if (subV.getHeight() >= f) {
                if (!subV.validateBlockCreator(pb, f)) {
                    logger.debug(format("[#%d-C[%d]] invalid invalid sub chain version, block creator is invalid [height=%d] [fp=%d]",
                            getID(),channel, pb.getHeader().getHeight(), forkPoint));
                    return false;
                }
            }
            subV.addBlock(pb);
        }
        return true;
    }

    private void syncBlockingPhase(int forkPoint) throws IOException {
        while ((!scVersions.containsKey(forkPoint)) ||
                scVersions.get(forkPoint).size() < n - f) {
           subChainVersion v;
            try {
                v = subChainVersion.parseFrom(syncRB.deliver(channel));
            } catch (InterruptedException e) {
                if (!stopped.get()) {
                    // ??? might interrupted if more then one panic message received.
                    logger.debug(format("[#%d-C[%d]] sync operation has been interrupted, try again...",
                            getID(), channel));
                    continue;
                } else {
                    return;
                }
            }
            if (v == null) {
                logger.debug(format("[#%d-C[%d]] Unable to parse sub chain version [fp=%d]]", getID(),channel, forkPoint));
                continue;
            }

            if (!validateSubChainVersion(v, v.getForkPoint())) {
                logger.debug(format("[#%d-C[%d]] sub chain version is invalid [r=%d]]", getID(), channel,v.getForkPoint()));
                continue;
            }
            scVersions.computeIfAbsent(v.getForkPoint(), k -> new ArrayList<>());
            scVersions.computeIfPresent(v.getForkPoint(), (k, v1)-> {
                if (v1.size() > 0 && v1.get(0).equals(subChainVersion.getDefaultInstance())) {
                    return v1;
                }
                v1.add(v);
                logger.debug(format("[#%d-C[%d]] add sub version for [r=%d] from [id=%d]",
                        getID(), channel, v.getForkPoint(), v.getSender()));
                return v1;
            });
//            if (!scVersions.containsKey(v.getForkPoint())) {
//                scVersions.put(v.getForkPoint(), new ArrayList<>());
//            }
//            scVersions.get(v.getForkPoint()).add(v);

        }
    }
    private void proposeVersion(int forkPoint) throws IOException {
        logger.debug(format("[#%d-C[%d]] proposeVersion for [r=%d]"
                , getID(), channel, forkPoint));
        if (scVersions.containsKey(forkPoint)) {
            int before = scVersions.get(forkPoint).size();
            scVersions.get(forkPoint).removeIf(sv -> !validateSubChainVersion(sv, forkPoint));
            logger.debug(format("[#%d-C[%d]] invalidate [%d] sub versions...",
                    getID(), channel, before - scVersions.get(forkPoint).size()));
        }
        if ((!scVersions.containsKey(forkPoint)) || scVersions.get(forkPoint).size() < n - f) {
            disseminateChainVersion(forkPoint);

        }
    }

    abstract void potentialBehaviourForSync() throws InterruptedException;

    private void sync(int forkPoint) throws IOException, InterruptedException {
        logger.debug(format("[#%d-C[%d]] start sync method with [r=%d]", getID(),channel, forkPoint));
        potentialBehaviourForSync();
        proposeVersion(forkPoint);
        syncBlockingPhase(forkPoint);
        if (stopped.get()) return;
//        fp.get(forkPoint).clear();
//        fp.get(forkPoint).add(ForkProof.getDefaultInstance());
        fp.replace(forkPoint, true);
        adoptSubVersion(forkPoint);
        logger.debug(format("[#%d-C[%d]] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), channel, currHeight, currLeader, cidSeries));
        mainThread = new Thread(this::mainLoop);
        mainThread.start();
    }

    private void adoptSubVersion(int forkPoint) throws IOException {
        if (forkPoint - (f + 1) > currHeight) {
            logger.debug(format("[#%d-C[%d]] is too far to participate [r=%d, curr=%d]"
                    , getID(),channel, forkPoint, currHeight));
            return;
        }
        final subChainVersion[] choosen = new subChainVersion[1];
        final int[] sPoint = {0};
        scVersions.computeIfPresent(forkPoint, (k, v1)-> {
            int max = v1.
                    stream().
                    mapToInt(v -> v.getV(v.getVCount() - 1).getHeader().getHeight()).
                    max().getAsInt();
            choosen[0] = v1.
                    stream().
                    filter(v -> v.getV(v.getVCount() - 1).getHeader().getHeight() == max
                            && BaseBlockchain.validateBlockHash(bc.getBlock(forkPoint - (f + 2)), v.getV(0))).
                    findFirst().get();
            sPoint[0] = choosen[0].getV(0).getHeader().getHeight();
            logger.info(format("[#%d-C[%d]] adopts sub chain version [length=%d] from [#%d] " +
                            "range [%d ---> %d] and is valid with respect to [%d]",
                    getID(),channel, choosen[0].getVList().size(),
                    choosen[0].getSender(), sPoint[0], max, forkPoint - (f + 2)));
            ArrayList<subChainVersion> ret = new ArrayList<>();
            ret.add(subChainVersion.getDefaultInstance());
            return ret;
        });

        synchronized (newBlockNotifyer) {
            for (int i = sPoint[0] ; i < bc.getHeight() ; i++) {
                bc.removeBlock(i);
            }

            bc.setBlocks(choosen[0].getVList(), sPoint[0]);
//            if (!bc.validateBlockHash(bc.getBlock(bc.getHeight()))) {
//            int newHeight = choosen[0].getV(choosen[0].getVCount() - 1).getHeader().getHeight();
//            if (bc.getHeight() > newHeight) {
//                logger.debug(format("[#%d-C[%d]] deletes a block [height=%d]", getID(),channel, bc.getHeight()));
////                skipCurrentBlock = true;
//                 // meant to handle the case in which the front is split between the to leading blocks
//            }
            newBlockNotifyer.notify();
        }

        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getM().getSender() + 2) % n;
        currHeight = bc.getHeight() + 1;
        cid = 0;
        cidSeries++;


    }

}
