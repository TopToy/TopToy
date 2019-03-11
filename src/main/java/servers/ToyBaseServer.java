package servers;

import blockchain.Blockchain;
import blockchain.validation.Tvalidator;
import blockchain.validation.Validator;
import com.google.protobuf.ByteString;
import communication.CommLayer;
import communication.overlays.Clique;
import config.Config;
import config.Node;
import das.RBroadcast.RBrodcastService;
import das.data.GlobalData;
import das.wrb.WrbNode;
import proto.Types;
import utils.CacheUtils;
import utils.DBUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static blockchain.Utils.*;
import static crypto.blockDigSig.verfiyBlockWRTheader;
import static java.lang.Math.max;
import static java.lang.String.format;
import proto.Types.*;

public abstract class ToyBaseServer extends Node {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyBaseServer.class);
    WrbNode rmfServer;
    private RBrodcastService RBService;
    final Blockchain bc;
    int currHeight;
    protected int f;
    protected int n;
    int currLeader;
    protected AtomicBoolean stopped = new AtomicBoolean(false);
    private final Object newBlockNotifyer = new Object();
    private int maxTransactionInBlock;
    private Thread mainThread;
    private Thread panicThread;
    private Thread commSendThread;
    int cid = 0;
    int cidSeries = 0;
    private final HashMap<Integer, Boolean> fp;
    boolean configuredFastMode;
    boolean fastMode;
    int channel;
    private boolean testing = Config.getTesting();
    private int txPoolMax = 100000;
    private int bareTxSize = Transaction.newBuilder()
            .setClientID(0)
            .setId(txID.newBuilder().setProposerID(0).setTxNum(0).build())
            .build().getSerializedSize() + 8;
    private int txSize = 0;
    private int cID = new Random().nextInt(10000);
    Path sPath;
    private ExecutorService storageWorker = Executors.newSingleThreadExecutor();
    private int syncEvents = 0;
    private int bid = 0;
    final Queue<Block> blocksForPropose = new LinkedList<>();
    final Queue<Block> proposedBlocks = new LinkedList<>();
    Block.Builder currBLock;
    final Object cbl = new Object();
    private CacheUtils txCache = new CacheUtils(0);
    private boolean intCatched = false;
    private CommLayer comm;
    private Validator v = new Tvalidator();

    void initThreads() {
        mainThread = new Thread(this::intMain);
        panicThread = new Thread(() -> {
            try {
                mainFork();
            } catch (InterruptedException | IOException e) {
                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
            }
        });
        commSendThread = new Thread(() -> {
            try {
                commSendLogic();
            } catch (InterruptedException e) {
                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
            }
        });
    }

    void commSendLogic() throws InterruptedException {
        while (!stopped.get()) {
            Block b;
            synchronized (blocksForPropose) {
                while (blocksForPropose.isEmpty()) {
                    blocksForPropose.wait();
                }
                b = blocksForPropose.poll();
            }
            synchronized (proposedBlocks) {
                comm.broadcast(channel, b);
                proposedBlocks.add(b);

            }
        }
    }

    public ToyBaseServer(String addr, int wrbPort, int id, int channel, int f, int maxTx, boolean fastMode,
                         WrbNode wrb, CommLayer comm, RBrodcastService rb) {
        super(addr, wrbPort, id);
        this.f = f;
        this.n = 3*f + 1;
        this.comm = comm;
        this.RBService = rb;
        sPath = Paths.get("blocks", String.valueOf(channel));
        bc = initBC(id, channel);
        currHeight = 1; // starts from 1 due to the genesis BaseBlock
        currLeader = 0;
        this.maxTransactionInBlock = maxTx;
        fp = new HashMap<>();
        initThreads();
        this.configuredFastMode = fastMode;
        this.fastMode = fastMode;
        this.channel = channel;
        rmfServer = wrb;
        currLeader = channel % n;
        if (testing) {
            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
        }
        currBLock = configureNewBlock();
    }

    public ToyBaseServer(String addr, int wrbPort, int commPort, int id, int channel, int f, int tmo, int tmoInterval,
                         int maxTx, boolean fastMode, ArrayList<Node> wrbCluster, ArrayList<Node> commCluster,
                         String bbcConfig, String rbConfigPath, String serverCrt, String serverPrivKey, String caRoot) {

        super(addr, wrbPort, id);
        this.f = f;
        this.n = 3*f + 1;
        rmfServer = new WrbNode(1, id, addr, wrbPort, f, tmo, tmoInterval,
                wrbCluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        comm = new Clique(id, addr, commPort, 1, commCluster);
        RBService = new RBrodcastService(id, n, f, rbConfigPath);
        bc = initBC(id, channel);
        currHeight = 1; // starts from 1 due to the genesis BaseBlock
        currLeader = 0;
        this.maxTransactionInBlock = maxTx;
        fp = new HashMap<>();
        initThreads();
        this.configuredFastMode = fastMode;
        this.fastMode = fastMode;
        currLeader = channel % n;
        if (testing) {
            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
        }
        currBLock = configureNewBlock();

    }

    Block.Builder configureNewBlock() {
        return Block.newBuilder().setHeader(BlockHeader.newBuilder())
                .setId(BlockID.newBuilder().setPid(getID()).setBid(++bid).build());
    }

    public void start(boolean group) {
        if (!group) {
            comm.join();
            logger.debug(format("[#%d-C[%d]] joined to layout", getID(), channel));
            rmfServer.start();
            logger.debug(format("[#%d-C[%d]] wrb server is up", getID(), channel));
            RBService.start();
            logger.debug(format("[#%d-C[%d]] RB service is up", getID(), channel));


        }
        new DBUtils();
        try {
            DBUtils.initTables(channel);
        } catch (SQLException e) {
            logger.error(format("[#%d-C[%d]] unable to start DBUtils", getID(), channel), e);
            shutdown(group);
        }
        logger.info(format("[#%d-C[%d]] is up", getID(), channel));
    }


    public void serve() {
        panicThread.start();
        logger.debug(format("[#%d-C[%d]] starts panic thread", getID(), channel));
        mainThread.start();
        logger.debug(format("[#%d-C[%d]] starts main thread", getID(), channel));
        commSendThread.start();
        logger.debug(format("[#%d-C[%d]] starts commSendThread thread", getID(), channel));
        logger.info(format("[#%d-C[%d]] starts serving", getID(), channel));
    }

    public void shutdown(boolean group) {
        stopped.set(true);
        storageWorker.shutdownNow();

        try {
            logger.debug(format("[#%d-C[%d]] interrupt main thread", getID(), channel));
            mainThread.interrupt();
            mainThread.join();
            logger.debug(format("[#%d-C[%d]] interrupt panic thread", getID(), channel));
            panicThread.interrupt();
            panicThread.join();
            logger.debug(format("[#%d-C[%d]] interrupt commSendThread thread", getID(), channel));
            commSendThread.interrupt();
            commSendThread.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d-C[%d]]", getID(), channel), e);

        }


        if (!group) {
            if (rmfServer != null) rmfServer.stop();
            if (comm != null) comm.leave();
            if (RBService != null) RBService.shutdown();
        }


    }
    private void updateLeaderAndHeight() {
        currHeight = bc.getHeight() + 1;
        currLeader = (currLeader + 1) % n;
        cid++;
    }

//     void gc(int index) throws IOException {
//        logger.debug(format("[#%d-C[%d]] clear buffers of [height=%d]", getID(), channel, index));
//        Meta key = bc.getBlock(index).getHeader().getM();
//        Meta rKey = Meta.newBuilder()
//                .setChannel(key.getChannel())
//                .setCidSeries(key.getCidSeries())
//                .setCid(key.getCid())
//                .build();
//        rmfServer.clearBuffers(rKey);
//        syncRB.clearBuffers(rKey);
//        panicRB.clearBuffers(rKey);
//        bc.setBlock(index, null);
//    }

    private boolean checkSyncEvent() {
        synchronized (fp) {
            if (fp.containsKey(currHeight) && fp.get(currHeight)) {
                try {
                    adoptSubVersion(currHeight);
                    return true;
                } catch (IOException e) {
                    logger.error(String.format("[#%d-C[%d]] unable to sync", getID(), channel), e);
                }

            }
        }
        return false;
    }

    private void intMain() {
        if (mainLoop()) {
            synchronized (fp) {
                intCatched = true;
                fp.notifyAll();
            }
        }
    }


    Types.Block getBlockWRTheader(Types.BlockHeader h, int channel) throws InterruptedException {
        Types.BlockID bid = Types.BlockID.newBuilder().setBid(h.getBid()).setPid(h.getM().getSender()).build();
        Block b = comm.recMsg(channel, bid);
        while (!verfiyBlockWRTheader(b, h)) {
            b = comm.recMsg(channel, bid);
        }
        return b;
    }

    BlockHeader getHeaderForCurrentBlock(Types.BlockHeader prev,
                                         int height, int cidSeries, int cid) {
        BlockHeader h;
        synchronized (cbl) {
            synchronized (blocksForPropose.element()) {
                if (blocksForPropose.isEmpty()) {
                    blocksForPropose.add(currBLock.build());
                    currBLock = configureNewBlock();
                }
                Block b = blocksForPropose.element();
                h = createBlockHeader(b, prev,getID(), height, cidSeries, cid, channel, b.getId().getBid());
//                sealedBlock = blocksForPropose
//                        .element()
//                        .construct(getID(), currHeight, cidSeries, cid, channel, bc.getBlock(currHeight - 1).getHeader());
            }
        }
        return h;
    }

    private boolean mainLoop() {
        while (!stopped.get()) {
            if (Thread.interrupted()) return true;
            checkSyncEvent();
            while (!bc.validateCurrentLeader(currLeader, f)) {
                logger.debug(format("[#%d-C[%d]] invalid leader [l=%d, h=%d]",
                        getID(), channel, currHeight, currHeight));
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 2; // I think we have no reason to increase CID.
            }
            BlockHeader next; // = leaderImpl();
            try {
                next = leaderImpl();
            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on leader impl",
                        getID(), channel));
                return true;
            }

            BlockHeader recHeader;

            try {
                long startTime = System.currentTimeMillis();
                recHeader = rmfServer.deliver(channel, cidSeries, cid, currHeight, currLeader, next);
                logger.debug(format("[#%d-C[%d]] deliver took about [%d] ms [cidSeries=%d ; cid=%d]",
                        getID(), channel, System.currentTimeMillis() - startTime, cidSeries, cid));

            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on wrb deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), channel, currHeight, cidSeries, cid));
                return true;
            }

            fastMode = configuredFastMode;
            if (recHeader == null) {
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 2;
                continue;
            }
            Block recBlock = null;
            try {
                recBlock = getBlockWRTheader(recHeader, channel);
            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on block deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), channel, currHeight, cidSeries, cid));
            }
            recBlock = recBlock.toBuilder().setHeader(recHeader).build();
            if (currLeader == getID()) {
                logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
                        getID(), channel, recBlock.getHeader().getM().getSender(), currHeight, cidSeries, cid));
//                synchronized (blocksForPropose) {
//                    blocksForPropose.remove();
//                }
                synchronized (proposedBlocks) {
                    proposedBlocks.remove();
                }
            }
            if (!bc.validateBlockHash(recBlock)) {
                announceFork(recBlock);
                fastMode = false;
                return true; // TODO: Check it very carefully! (maybe continue is better)
            }

            synchronized (newBlockNotifyer) {
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

                    int pid = permanent.getHeader().getM().getSender();
                    int bid = permanent.getId().getBid();
                    int height = permanent.getHeader().getHeight();
                    storageWorker.execute(() ->
                            DBUtils.writeBlockToTable(channel, pid, bid, height));
                }

                logger.debug(String.format("[#%d-C[%d]] adds new Block with [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
                        getID(), channel, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));

                newBlockNotifyer.notify();


            }
            updateLeaderAndHeight();
            storageWorker.execute(bc::writeNextToDisk);
        }
        return false;
    }

    abstract BlockHeader leaderImpl() throws InterruptedException;

    abstract public Blockchain initBC(int id, int channel);

    abstract public Blockchain getBC(int start, int end);

    abstract public Blockchain getEmptyBC();

    public int getTxPoolSize() {
        synchronized (cbl) {
            synchronized (blocksForPropose) {
                if (blocksForPropose.size() == 0) return currBLock.getDataCount();
                return (blocksForPropose.size() * maxTransactionInBlock) +
                        currBLock.getDataCount();
            }
        }

    }

    public txID addTransaction(Transaction tx) {
        if (getTxPoolSize() > txPoolMax) return null;
        synchronized (cbl) {
            int cbid = currBLock.getId().getBid();
            int txnum = currBLock.getDataCount();
            Transaction ntx = tx.toBuilder()
                    .setId(txID.newBuilder()
                            .setProposerID(getID())
                            .setBid(cbid)
                            .setTxNum(txnum)
                            .setChannel(channel))
                    .setServerTs(System.currentTimeMillis())
                    .build();
            if (validateTransactionWRTBlock(currBLock, ntx, v)) {
                currBLock.addData(ntx);
            }
            if (currBLock.getDataCount() >= maxTransactionInBlock) {
                synchronized (blocksForPropose) {
                    blocksForPropose.add(currBLock.build());
                    currBLock = configureNewBlock();
                    blocksForPropose.notifyAll();
                }
            }
            return ntx.getId();
        }

    }


    public txID addTransaction(byte[] data, int clientID) {
        Transaction tx = Transaction.newBuilder()
                .setClientID(clientID)
                .setData(ByteString.copyFrom(data)).build();
        return addTransaction(tx);
//        if (transactionsPool.size() > txPoolMax) return null;
//    //        String txID = UUID.randomUUID().toString();
//        long currTx = txNum.getAndIncrement();
//        Transaction t = Transaction.newBuilder()
//                .setClientID(clientID)
//                .setId(Types.txID.newBuilder()
//                        .setTxNum(currTx)
//                        .setProposerID(getID())
//                        .setChannel(channel))
//                .setData()
//                .build();
//        transactionsPool.add(t);
//        return t.getId();
    }
    private void addApprovedTransactions(List<Transaction> txs) {
//        synchronized (approvedTx) {
//            for (Transaction tx :txs) {
//                approvedTx.put(tx.getTxID(), tx);
//            }
//        }

    }
    public int isTxPresent(txID tid) {
         if (txCache.contains(tid)) {
             return 0;
        }
        int height = DBUtils.getBlockRecord(tid.getChannel(), tid.getProposerID(), tid.getBid());

        if (height == -1) return -1;
        Block b = bc.getBlock(height);
        if (b == null) return -1;
        Transaction tx = b.getData(tid.getTxNum());
         if (tx.getId().equals(tid)) {
            txCache.add(tx);
              return 0;
        }
        return -1;
    }

    void createTxToBlock() {
        int missingTxs = -1;
        synchronized (cbl) {
            synchronized (blocksForPropose) {
                if (blocksForPropose.isEmpty()) {
                    missingTxs = maxTransactionInBlock - currBLock.getDataCount();
                }
            }
        }

        if (missingTxs !=  -1) {
            for (int i = 0 ; i < missingTxs ; i++) {
                long ts = System.currentTimeMillis();
                SecureRandom random = new SecureRandom();
                byte[] tx = new byte[txSize];
                random.nextBytes(tx);
                addTransaction(Transaction.newBuilder()
                        .setClientID(cID)
                        .setClientTs(ts)
                        .setServerTs(ts)
                        .setData(ByteString.copyFrom(tx))
                        .build());
            }
        }

//        while (currBlock.getTransactionCount() < maxTransactionInBlock) {
//            long ts = System.currentTimeMillis();
//            SecureRandom random = new SecureRandom();
//            byte[] tx = new byte[txSize];
//            random.nextBytes(tx);
//            long currTx = txNum.getAndIncrement();
//            currBlock.addTransaction(Transaction.newBuilder()
//                    .setId(txID.newBuilder()
//                            .setProposerID(getID())
//                            .setTxNum(currTx)
//                            .setChannel(channel))
//                    .setClientID(cID)
//                    .setClientTs(ts)
//                    .setServerTs(ts)
//                    .setData(ByteString.copyFrom(tx))
//                    .build());
//        }

    }

    void addTransactionsToCurrBlock() {
//        if (currBlock != null) {
//            logger.debug(format("[#%d-C[%d]] block is already build",
//                    getID(), channel));
//            return;
//        }
//        currBlock = bc.createNewBLock();
//        currBlock.blockBuilder.setSt(blockStatistics
//                .newBuilder()
//                .build());
//            while ((!transactionsPool.isEmpty()) && currBlock.getTransactionCount() < maxTransactionInBlock) {
//                Transaction t = transactionsPool.poll();
//                if (!currBlock.validateTransaction(t)) {
//                    logger.debug(format("[#%d-C[%d]] detects an invalid transaction from [client=%d]",
//                            getID(), channel, t.getClientID()));
//                    continue;
//                }
//                currBlock.addTransaction(t);
//            }
            if (testing) {
                createTxToBlock();
            }
    }


//    private boolean validateForkProof(ForkProof p)  {
//        logger.debug(format("[#%d-C[%d]] starts validating fp", getID(), channel));
//        Block curr = p.getCurr();
//        Block prev = p.getPrev();
//        if (!blockDigSig.verify(curr.getHeader().getM().getSender(), curr)) {
//                logger.debug(format("[#%d-C[%d]] invalid fork proof #3", getID(), channel));
//                return false;
//        }
//        if (!blockDigSig.verify(prev.getHeader().getM().getSender(), prev)) {
//            logger.debug(format("[#%d-C[%d]] invalid fork proof #4", getID(), channel));
//            return false;
//        }
//
//        logger.debug(format("[#%d-C[%d]] panic for fork is valid [fp=%d]", getID(), channel, p.getCurr().getHeader().getHeight()));
//        return true;
//    }

    private void mainFork() throws InterruptedException, IOException {
        while (!stopped.get()) {
            int forkPoint;
            synchronized (GlobalData.forksRBData[channel]) {
                while (GlobalData.forksRBData[channel].isEmpty()) {
                    GlobalData.forksRBData[channel].wait();
                }
                forkPoint = Objects.requireNonNull(GlobalData.forksRBData[channel].poll())
                        .getCurr().getHeader().getHeight();
            }
            handleFork(forkPoint);

        }
    }
    private void handleFork(int forkPoint) throws IOException, InterruptedException {
        synchronized (fp) {
            if (fp.containsKey(forkPoint) && fp.get(forkPoint))
            {
                logger.debug(format("[#%d-C[%d]] already synchronized for this point [r=%d]",
                        getID(), channel, forkPoint));
                return;
            }

            if (forkPoint - (f + 1) <= currHeight) {
                logger.debug(format("[#%d-C[%d]] interrupting main thread [r=%d]"
                        , getID(),channel, forkPoint));
                mainThread.interrupt();
                while (!intCatched) {
                    fp.wait();
                }
                mainThread.join();
                intCatched = false;
            }

            if (!fp.containsKey(forkPoint)) {
                fp.put(forkPoint, false);
            }

            syncEvents++;
            sync(forkPoint);

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
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .setSender(getID())
                .build();
        RBService.broadcast(p.toByteArray(), key, GlobalData.RBTypes.FORK);
//        handleFork(p.getCurr().getHeader().getHeight());
    }

    public Block deliver(int index) throws InterruptedException {
        synchronized (newBlockNotifyer) {
            while (index >= bc.getHeight() - (f + 1)) {
                newBlockNotifyer.wait();
            }
        }
        Block b = bc.getBlock(index);
        txCache.addBlock(b);
        return b;
    }

    public Block nonBlockingdeliver(int index) {
        synchronized (newBlockNotifyer) {
            if (index >= bc.getHeight() - (f + 1)) {
                return null;
            }
        }
        Block b = bc.getBlock(index);
        txCache.addBlock(b);
        return b;
    }

    public int bcSize() {
        return max(bc.getHeight() - (f + 2), 0);
    }

    public int getSyncEvents() {
        return syncEvents;
    }
    private void disseminateChainVersion(int forkPoint) {
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
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .setSender(getID())
                .build();
        RBService.broadcast(sv.build().toByteArray(), key, GlobalData.RBTypes.SYNC);
    }

    abstract void potentialBehaviourForSync() throws InterruptedException;

    private void sync(int forkPoint) throws InterruptedException, IOException {
        logger.debug(format("[#%d-C[%d]] start sync method with [r=%d]", getID(),channel, forkPoint));
        potentialBehaviourForSync();

        logger.debug(format("[#%d-C[%d]] proposeVersion for [r=%d]"
                , getID(), channel, forkPoint));
        synchronized (GlobalData.syncRBData[channel]) {
            if ((!GlobalData.syncRBData[channel].containsKey(forkPoint))
                    || GlobalData.syncRBData[channel].get(forkPoint).size() < n - f) {
                disseminateChainVersion(forkPoint);

            }
        }


        synchronized (GlobalData.syncRBData[channel]) {
            while (!GlobalData.syncRBData[channel].containsKey(forkPoint) ||
                    GlobalData.syncRBData[channel].get(forkPoint).size() < n - f) {
                GlobalData.syncRBData[channel].wait();
            }
        }

        fp.replace(forkPoint, true);

        if (forkPoint - (f + 1) > currHeight) {
            logger.debug(format("[#%d-C[%d]] is too far to participate [r=%d, curr=%d]"
                    , getID(),channel, forkPoint, currHeight));
            return;
        }
        adoptSubVersion(forkPoint);
        mainThread = new Thread(this::intMain);
        mainThread.start();
    }

    private void adoptSubVersion(int forkPoint) throws IOException {
        final subChainVersion[] choosen = new subChainVersion[1];
        final int[] sPoint = {0};
        AtomicBoolean noMatch = new AtomicBoolean(false);
        synchronized (GlobalData.syncRBData[channel]) {
            GlobalData.syncRBData[channel].computeIfPresent(forkPoint, (k, v1)-> {
                ArrayList<subChainVersion> ret = new ArrayList<>();
                ret.add(subChainVersion.getDefaultInstance());
                if (v1.stream().noneMatch(v -> v.getVCount() > 0)) {
                    noMatch.set(true);
                    logger.error(format("all versions are empties (might happen due to fast mode) [fp=%d] ", forkPoint));
                    return ret;
                }
                int max = v1.
                        stream().
                        filter(v -> v.getVCount() > 0).
                        mapToInt(v -> v.getV(v.getVCount() - 1).getHeader().getHeight()).
                        max().getAsInt();
                List valid = v1.
                        stream().
                        filter(v -> v.getVCount() > 0).
                        filter(v -> v.getV(v.getVCount() - 1).getHeader().getHeight() == max
                                && validateBlockHash(bc.getBlock(forkPoint - (f + 2)), v.getV(0))).collect(Collectors.toList());
                if (valid.size() == 0) {
                    noMatch.set(true);
                    logger.error(format("No version is valid (might happen due to fast mode) [fp=%d]", forkPoint));
                    return ret;
                }
                choosen[0] = (subChainVersion) valid.stream().
                        findFirst().get();
                sPoint[0] = choosen[0].getV(0).getHeader().getHeight();
                logger.info(format("[#%d-C[%d]] adopts sub chain version [length=%d] from [#%d] " +
                                "range [%d ---> %d] and is valid with respect to [%d]",
                        getID(),channel, choosen[0].getVList().size(),
                        choosen[0].getSender(), sPoint[0], max, forkPoint - (f + 2)));

                return ret;
            });
        }
        if (noMatch.get()) return;
        synchronized (newBlockNotifyer) {
            for (int i = sPoint[0] ; i <= bc.getHeight() ; i++) {
//                System.out.println("---" + i + "----");
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

        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getM().getSender() + 1) % n;
        currHeight = bc.getHeight() + 1;
        cid = 0;
        cidSeries++;
        logger.debug(format("[#%d-C[%d]] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), channel, currHeight, currLeader, cidSeries));


    }

}
