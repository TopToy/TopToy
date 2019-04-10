package servers;

import blockchain.Blockchain;
import blockchain.validation.Tvalidator;
import blockchain.validation.Validator;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import communication.CommLayer;
import config.Config;
import config.Node;
import das.ab.ABService;
import das.data.Data;
//import das.wrb.WrbNode;
import das.wrb.WRB;
import proto.Types;
import utils.CacheUtils;
import utils.DBUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static blockchain.Utils.*;
import static blockchain.data.BCS.bcs;
import static java.lang.Math.max;
import static java.lang.String.format;
import proto.Types.*;

public abstract class ToyBaseServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyBaseServer.class);
//    WrbNode wrbServer;
    int id;
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
    int worker;
    private boolean testing = Config.getTesting();
    private int txPoolMax = 100000;
    private int bareTxSize = Transaction.newBuilder()
            .setClientID(0)
            .setId(txID.newBuilder().setProposerID(0).setTxNum(0).build())
            .build().getSerializedSize() + 8;
    private int txSize = 0;
    private int cID = new Random().nextInt(10000);
    Path sPath;
//    private ExecutorService storageWorker = Executors.newSingleThreadExecutor();
    private int syncEvents = 0;
    private int bid = 0;
    final Queue<Block> blocksForPropose = new LinkedList<>();
    final Queue<Block> proposedBlocks = new LinkedList<>();
    Block.Builder currBLock;
    final Object cbl = new Object();
    private CacheUtils txCache = new CacheUtils(0);
    private boolean intCatched = false;
    CommLayer comm;
    private Validator v = new Tvalidator();

    void initThreads() {
        mainThread = new Thread(this::intMain);
        panicThread = new Thread(() -> {
            try {
                mainFork();
            } catch (InterruptedException | IOException e) {
                logger.error(format("[#%d-C[%d]]", getID(), worker), e);
            }
        });
        commSendThread = new Thread(() -> {
            try {
                commSendLogic();
            } catch (InterruptedException e) {
                logger.error(format("[#%d-C[%d]]", getID(), worker), e);
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
                synchronized (proposedBlocks) {
                    comm.broadcast(worker, b);
                    proposedBlocks.add(b);

                }
            }

        }
    }

    public ToyBaseServer(int id, int worker, int n, int f, int maxTx, boolean fastMode,
                         CommLayer comm) {
        this.id = id;
        this.f = f;
        this.n = n;
        this.comm = comm;
        sPath = Paths.get("blocks", String.valueOf(worker));
        bc = initBC(id, worker);
        bcs[worker] = bc;
        currHeight = 1; // starts from 1 due to the genesis BaseBlock
        currLeader = 0;
        this.maxTransactionInBlock = maxTx;
        fp = new HashMap<>();
        initThreads();
        this.configuredFastMode = n != 1 && fastMode;
        this.fastMode = fastMode;
        this.worker = worker;
//        wrbServer = wrb;
        currLeader = worker % n;
        if (testing) {
            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
        }
        currBLock = configureNewBlock();
        logger.info(format("Initiated ToyBaseServer: [id=%d; n=%d; f=%d; worker=%d]", id, n, f, worker));
    }

    int getID() {
        return id;
    }

    Block.Builder configureNewBlock() {
        return Block.newBuilder().setHeader(BlockHeader.newBuilder())
                .setId(BlockID.newBuilder().setPid(getID()).setBid(++bid).build());
    }

    public void start() {

        new DBUtils();
        try {
            DBUtils.initTables(worker);
        } catch (SQLException e) {
            logger.error(format("[#%d-C[%d]] unable to start DBUtils", getID(), worker), e);
            shutdown();
        }
        logger.info(format("[#%d-C[%d]] is up", getID(), worker));
    }


    public void serve() {
        panicThread.start();
        logger.debug(format("[#%d-C[%d]] starts panic thread", getID(), worker));
        mainThread.start();
        logger.debug(format("[#%d-C[%d]] starts main thread", getID(), worker));
        commSendThread.start();
        logger.debug(format("[#%d-C[%d]] starts commSendThread thread", getID(), worker));
        logger.info(format("[#%d-C[%d]] starts serving", getID(), worker));
    }

    public void shutdown() {
        stopped.set(true);
//        storageWorker.shutdownNow();

        try {
            logger.debug(format("[#%d-C[%d]] interrupt main thread", getID(), worker));
            mainThread.interrupt();
            mainThread.join();
            logger.debug(format("[#%d-C[%d]] interrupt panic thread", getID(), worker));
            panicThread.interrupt();
            panicThread.join();
            logger.debug(format("[#%d-C[%d]] interrupt commSendThread thread", getID(), worker));
            commSendThread.interrupt();
            commSendThread.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d-C[%d]]", getID(), worker), e);

        }


    }
    private void updateLeaderAndHeight() {
        currHeight = bc.getHeight() + 1;
        currLeader = (currLeader + 1) % n;
        cid++;
    }

    private boolean checkSyncEvent() {
        synchronized (fp) {
            if (fp.containsKey(currHeight) && fp.get(currHeight)) {
                try {
                    adoptSubVersion(currHeight);
                    return true;
                } catch (IOException e) {
                    logger.error(String.format("[#%d-C[%d]] unable to sync", getID(), worker), e);
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
        return comm.recBlock(channel, bid, h);
    }

    BlockHeader getHeaderForCurrentBlock(Types.BlockHeader prev,
                                         int height, int cidSeries, int cid) {
        synchronized (proposedBlocks) {
            if (proposedBlocks.isEmpty()) {
                logger.debug(format("[#%d-C[%d]] header for an empty block [height=%d ; cidSeries=%d ; cid=%d] has been created",
                        getID(), worker, height, cidSeries, cid));
                return createBlockHeader(Block.getDefaultInstance(),
                        prev, getID(), height, cidSeries, cid, worker, -1);
            }

            Block b = proposedBlocks.element();
            logger.debug(format("[#%d-C[%d]] header for [height=%d ; cidSeries=%d ; cid=%d ; bid=%d] has been created",
                    getID(), worker, height, cidSeries, cid, b.getId().getBid()));
            return createBlockHeader(b, prev, getID(), height, cidSeries, cid, worker, b.getId().getBid());
        }
    }

    private boolean mainLoop() {
        if (testing) {
            for (int i = 0 ; i < 10 ; i++) {
                addTransactionsToCurrBlock();
            }
        }
        while (!stopped.get()) {
            if (Thread.interrupted()) return true;
            checkSyncEvent();
            while (!bc.validateCurrentLeader(currLeader, f)) {
                logger.debug(format("[#%d-C[%d]] invalid leader [l=%d, h=%d]",
                        getID(), worker, currHeight, currHeight));
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 2; // I think we have no reason to increase CID.
            }
            BlockHeader next; // = leaderImpl();
            try {
                next = leaderImpl();
            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on leader impl",
                        getID(), worker));
                return true;
            }

            BlockHeader recHeader;

            try {
                long startTime = System.currentTimeMillis();
                recHeader = WRB.WRBDeliver(worker, cidSeries, cid, currLeader, currHeight, next);
                logger.debug(format("[#%d-C[%d]] deliver took about [%d] ms [cidSeries=%d ; cid=%d]",
                        getID(), worker, System.currentTimeMillis() - startTime, cidSeries, cid));

            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on wrb deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), worker, currHeight, cidSeries, cid));
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
                recBlock = getBlockWRTheader(recHeader, worker);
            } catch (InterruptedException e) {
                logger.debug(format("[#%d-C[%d]] main thread has been interrupted on block deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), worker, currHeight, cidSeries, cid));
            }
            recBlock = recBlock.toBuilder().setHeader(recHeader).build();
            removeFromPendings(recHeader, recBlock);

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
                        logger.info(format("[#%d-C[%d]] Deliverd [[height=%d], [sender=%d], [channel=%d], [size=%d]]",
                            getID(), worker, permanent.getHeader().getHeight(), permanent.getHeader().getM().getSender(),
                                permanent.getHeader().getM().getChannel(),
                                permanent.getDataCount()));
                    } catch (IOException e) {
                        logger.error(String.format("[#%d-C[%d]] unable to record permanent time for block [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
                                getID(), worker, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));
                    }
                    DBUtils.writeBlockToTable(permanent);
                }

                logger.debug(String.format("[#%d-C[%d]] adds new Block with [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
                        getID(), worker, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));

                newBlockNotifyer.notify();


            }
            updateLeaderAndHeight();
            bc.writeNextToDisk();
//            storageWorker.execute(bc::writeNextToDisk);
        }
        return false;
    }

    void removeFromPendings(BlockHeader recHeader, Block recBlock) {
        if (currLeader == getID() && !recHeader.getEmpty()) {
            logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
                    getID(), worker, recBlock.getHeader().getM().getSender(), currHeight, cidSeries, cid));
//                synchronized (blocksForPropose) {
//                    blocksForPropose.remove();
//                }
            synchronized (proposedBlocks) {
                proposedBlocks.remove();
            }
        }
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
                            .setChannel(worker))
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
        synchronized (proposedBlocks) {
            if (proposedBlocks.size() > 5) {
                return;
            }
        }
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

    }

    void addTransactionsToCurrBlock() {
            if (testing) {
                createTxToBlock();
            }
    }

    void broadcastEmptyIfNeeded() {
        synchronized (cbl) {
            synchronized (blocksForPropose) {
                synchronized (proposedBlocks) {
                    if (blocksForPropose.size() == 0 && proposedBlocks.size() == 0) {
                        logger.debug(format("[#%d-C[%d]] broadcast empty block"
                                , getID(),worker));
                        blocksForPropose.add(currBLock.build());
                        currBLock = configureNewBlock();
                    }

                }
            }
        }


    }


    private void mainFork() throws InterruptedException, IOException {
        while (!stopped.get()) {
            int forkPoint;
            synchronized (Data.forksRBData[worker]) {
                while (Data.forksRBData[worker].isEmpty()) {
                    Data.forksRBData[worker].wait();
                }
                forkPoint = Objects.requireNonNull(Data.forksRBData[worker].poll())
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
                        getID(), worker, forkPoint));
                return;
            }

            if (forkPoint - (f + 1) <= currHeight) {
                logger.debug(format("[#%d-C[%d]] interrupting main thread [r=%d]"
                        , getID(),worker, forkPoint));
                mainThread.interrupt();
                while (!intCatched) { // TODO: deadlock alert!!
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
                getID(), worker, currHeight));
        ForkProof p = ForkProof.
                newBuilder().
                setCurr(b).
                setPrev(bc.getBlock(bc.getHeight())).
                setSender(getID()).
                build();
        Meta key = Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .setSender(getID())
                .build();
        ABService.broadcast(p.toByteArray(), key, Data.RBTypes.FORK);
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
                    , getID(), worker, forkPoint, currHeight));
        } else {
            int low = max(forkPoint - (f + 1), 0);
            int high = bc.getHeight() + 1;
            logger.debug(format("[#%d-C[%d]] building a sub version [(inc) %d --> (exc) %d]"
                    , getID(), worker, low, high));
            for (Block b : bc.getBlocks(low, high)) {
                sv.addV(b);
            }
            sv.setSuggested(1);
            sv.setSender(getID());
            sv.setForkPoint(forkPoint);

        }
        Meta key = Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .setSender(getID())
                .build();
        ABService.broadcast(sv.build().toByteArray(), key, Data.RBTypes.SYNC);
    }

    abstract void potentialBehaviourForSync() throws InterruptedException;

    private void sync(int forkPoint) throws InterruptedException, IOException {
        logger.debug(format("[#%d-C[%d]] start sync method with [r=%d]", getID(),worker, forkPoint));
        potentialBehaviourForSync();

        logger.debug(format("[#%d-C[%d]] proposeVersion for [r=%d]"
                , getID(), worker, forkPoint));
        synchronized (Data.syncRBData[worker]) {
            if ((!Data.syncRBData[worker].containsKey(forkPoint))
                    || Data.syncRBData[worker].get(forkPoint).size() < n - f) {
                disseminateChainVersion(forkPoint);

            }
        }


        synchronized (Data.syncRBData[worker]) {
            while (!Data.syncRBData[worker].containsKey(forkPoint) ||
                    Data.syncRBData[worker].get(forkPoint).size() < n - f) {
                Data.syncRBData[worker].wait();
            }
        }

        fp.replace(forkPoint, true);

        if (forkPoint - (f + 1) > currHeight) {
            logger.debug(format("[#%d-C[%d]] is too far to participate [r=%d, curr=%d]"
                    , getID(),worker, forkPoint, currHeight));
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
        synchronized (Data.syncRBData[worker]) {
            Data.syncRBData[worker].computeIfPresent(forkPoint, (k, v1)-> {
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
                        getID(),worker, choosen[0].getVList().size(),
                        choosen[0].getSender(), sPoint[0], max, forkPoint - (f + 2)));

                return ret;
            });
        }
        if (noMatch.get()) return;
        synchronized (newBlockNotifyer) {
            for (int i = sPoint[0] ; i <= bc.getHeight() ; i++) {
                bc.removeBlock(i);
            }

            bc.setBlocks(choosen[0].getVList(), sPoint[0]);
            newBlockNotifyer.notify();
        }

        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getM().getSender() + 1) % n;
        currHeight = bc.getHeight() + 1;
        cid = 0;
        cidSeries++;
        logger.debug(format("[#%d-C[%d]] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), worker, currHeight, currLeader, cidSeries));


    }

    boolean isBcValid() {
        return bc.isValid();
    }

    static public void addForkProof(RBMsg msg, int channel) throws InvalidProtocolBufferException {
        logger.debug(format("received FORK message on channel [%d]", channel));
        ForkProof p = ForkProof.parseFrom(msg.getData());
        if (!Data.validateForkProof(p)) return;
        synchronized (Data.forksRBData[channel]) {
            Data.forksRBData[channel].add(p);
            Data.forksRBData[channel].notifyAll();
        }
    }

    static public void addToSyncData(RBMsg msg, int channel, int n, int f) throws InvalidProtocolBufferException {
        logger.debug(format("received SYNC message on channel [%d]", channel));
        subChainVersion sbv = subChainVersion.parseFrom(msg.getData());
        int fp = sbv.getForkPoint();
        synchronized (Data.syncRBData[channel]) {
            if (Data.syncRBData[channel].containsKey(fp)
                    && Data.syncRBData[channel].get(fp).size() == n - f) return;
        }

        synchronized (Data.syncRBData[channel]) {
            if (!Data.validateSubChainVersion(sbv, f)) return;
            Data.syncRBData[channel].computeIfAbsent(fp, k -> new ArrayList<>());
            Data.syncRBData[channel].computeIfPresent(fp, (k, v) -> {
                v.add(sbv);
                return v;
            });
            if (Data.syncRBData[channel].get(fp).size() == n - f) {
                Data.syncRBData[channel].notifyAll();
            }
        }
    }

}
