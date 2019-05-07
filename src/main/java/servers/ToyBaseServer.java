package servers;

import blockchain.data.BCS;
import blockchain.validation.Tvalidator;
import blockchain.validation.Validator;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import communication.CommLayer;
import config.Config;
import das.ab.ABService;
import das.data.Data;
import das.ms.BFD;
import das.wrb.WRB;
import proto.Types;
import utils.CacheUtils;
import utils.DBUtils;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static blockchain.Utils.*;
import static java.lang.Math.max;
import static java.lang.String.format;
import static utils.statistics.Statistics.*;

import proto.Types.*;
import utils.statistics.Statistics;

public abstract class ToyBaseServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyBaseServer.class);

//    WrbNode wrbServer;
    int id;
//    final Blockchain bc;
    int currHeight;
    protected int f;
    protected int n;
    int currLeader;
    protected AtomicBoolean stopped = new AtomicBoolean(false);
//    private final Object newBlockNotifyer = new Object();
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
    private int txPoolMax = 10000;
    private int txSize = 0;
    private int cID = new Random().nextInt(10000);
//    Path sPath;
    private int bid = 0;
    private final Queue<Block> blocksForPropose = new LinkedList<>();
    private final Queue<Block> proposedBlocks = new LinkedList<>();
    private Block.Builder currBLock;
    private final Object cbl = new Object();
    private boolean intCatched = false;
    CommLayer comm;
    private Validator v = new Tvalidator();
    CountDownLatch startLatch = new CountDownLatch(3);

    void initThreads() {
        mainThread = new Thread(this::intMain);
        panicThread = new Thread(() -> {
            startLatch.countDown();
            try {
                mainFork();
            } catch (InterruptedException | IOException e) {
                logger.error(format("[#%d-C[%d]]", getID(), worker), e);
            }
        });
        commSendThread = new Thread(() -> {
            startLatch.countDown();
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
                if (b.getDataCount() == 0) continue;
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
//        sPath = Paths.get("blocks", String.valueOf(worker));
//        bc = initBC(id, worker);
//        bcs[worker] = bc;
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
            txSize = StrictMath.max(0, Config.getTxSize());
        }
        currBLock = configureNewBlock();
        try {
            DBUtils.initTables(worker);
        } catch (SQLException e) {
            logger.error(format("[#%d-C[%d]] unable to start DBUtils", getID(), worker), e);
            shutdown();
        }
//        logger.info(format("[#%d-C[%d]] is up", getID(), worker));
        logger.info(format("Initiated ToyBaseServer: [id=%d; n=%d; f=%d; worker=%d]", id, n, f, worker));
    }

    int getID() {
        return id;
    }

    Block.Builder configureNewBlock() {
        return Block.newBuilder()
                .setId(BlockID.newBuilder().setPid(getID()).setBid(++bid).build());
    }


    public void serve() throws InterruptedException {
        commSendThread.start();
//        while (commSendThread.getState() != Thread.State.RUNNABLE);
        logger.debug(format("[#%d-C[%d]] starts commSendThread thread", getID(), worker));
        panicThread.start();
//        while (panicThread.getState() != Thread.State.RUNNABLE);
        logger.debug(format("[#%d-C[%d]] starts panic thread", getID(), worker));
        mainThread.start();
//        while (mainThread.getState() != Thread.State.RUNNABLE);
        logger.debug(format("[#%d-C[%d]] starts main thread", getID(), worker));
        startLatch.await();
        logger.info(format("[#%d-C[%d]] starts serving", getID(), worker));
    }

    public void shutdown() {
        stopped.set(true);
//        storageWorker.shutdownNow();

        try {
            logger.info(format("[#%d-C[%d]] interrupt main thread", getID(), worker));
            mainThread.interrupt();
            mainThread.join();
            logger.info(format("[#%d-C[%d]] interrupt panic thread", getID(), worker));
            panicThread.interrupt();
            panicThread.join();
            logger.info(format("[#%d-C[%d]] interrupt commSendThread thread", getID(), worker));
            commSendThread.interrupt();
            commSendThread.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d-C[%d]]", getID(), worker), e);

        }


    }
    private void updateLeaderAndHeight() {
        currHeight = BCS.lastIndex(worker) + 1; //bc.getHeight() + 1;
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
        startLatch.countDown();
        if (mainLoop()) {
            synchronized (fp) {
                intCatched = true;
                fp.notifyAll();
            }
        }
    }


    Types.Block getBlockWRTheader(Types.BlockHeader h, int channel) throws InterruptedException {
        if (h.getEmpty()) {
            logger.debug(format("received an empty block, returning a match [w=%d ; " +
                            "cidSereis=%d ; cid=%d ; height=%d ; pid=%d ; bid=%d]",
                    channel, h.getM().getCidSeries(), h.getM().getCid(), h.getHeight(),
                    h.getBid().getPid(), h.getBid().getBid()));

            return Block.newBuilder().setBst(
                    blockStatistics.newBuilder()
                            .setProposeTime(System.currentTimeMillis())
                            .build()
            ).build();
        }
        return comm.recBlock(channel, h);
    }

    BlockHeader getHeaderForCurrentBlock(Types.BlockHeader prev,
                                         int height, int cidSeries, int cid) {
        synchronized (cbl) {
            synchronized (proposedBlocks) {
                if (proposedBlocks.isEmpty()) {
                    Block b = currBLock.build();
                    logger.debug(format("[#%d-C[%d]] creates an empty header [height=%d ; cidSeries=%d ; cid=%d ; bid=%d]",
                            getID(), worker, height, cidSeries, cid, b.getId().getBid()));
                    currBLock = configureNewBlock();
                    return createBlockHeader(b, prev, getID(), height, cidSeries, cid, worker, b.getId());
                }

                Block b = proposedBlocks.element();
                logger.debug(format("[#%d-C[%d]] creates header for [height=%d ; cidSeries=%d ; cid=%d ; bid=%d]",
                        getID(), worker, height, cidSeries, cid, b.getId().getBid()));
                return createBlockHeader(b, prev, getID(), height, cidSeries, cid, worker, b.getId());
            }
        }

    }

    private boolean mainLoop() {
//        if (testing) {
//            for (int i = 0 ; i < 10 ; i++) {
//                addTransactionsToCurrBlock();
//            }
//        }
        while (!stopped.get()) {
            if (Thread.interrupted()) return true;
            checkSyncEvent();
            while (!BCS.validateCurrentLeader(worker, currLeader, f)) {
                logger.debug(format("[#%d-C[%d]] invalid leader [l=%d, h=%d]",
                        getID(), worker, currHeight, currHeight));
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 1;
                BFD.deactivate(worker);
            }
//            long start = System.currentTimeMillis();
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
                logger.debug(format("[#%d-C[%d]] WRB deliver took about [%d] ms [cidSeries=%d ; cid=%d ; height=%d ; dec=%b]",
                        getID(), worker, System.currentTimeMillis() - startTime, cidSeries, cid, currHeight, recHeader != null));

            } catch (InterruptedException e) {
                logger.info(format("[#%d-C[%d]] main thread has been interrupted on wrb deliver " +
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
                logger.info(format("[#%d-C[%d]] main thread has been interrupted on block deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), worker, currHeight, cidSeries, cid));
                return true;
            }

            recBlock = recBlock.toBuilder().setHeader(recHeader).build();


            removeFromPendings(recHeader, recBlock);

            if (!BCS.validateBlockHash(worker, recBlock)) {
                announceFork(recBlock);
                fastMode = false;
                return true; // TODO: Check it very carefully! (maybe continue is better)
            }

//            synchronized (newBlockNotifyer) {
//                recBlock = recBlock
//                        .toBuilder()
//                        .setSt(recBlock.getSt()
//                                .toBuilder()
//                                .setChannelDecided(System.currentTimeMillis()))
//                        .build();

            BCS.addBlock(worker, recBlock.toBuilder()
                        .setHeader(recBlock.getHeader().toBuilder()
                                    .setHst(recBlock.getHeader().getHst().toBuilder()
                                            .setTentativeTime(System.currentTimeMillis())
                                            .setTentative(true)))
                    .build());
//            bc.addBlock(recBlock);

            if (BCS.height(worker) > 0) {

                Block permanent = BCS.nbGetBlock(worker, BCS.height(worker));
                BCS.setBlock(worker, BCS.height(worker), permanent.toBuilder()
                            .setHeader(permanent.getHeader().toBuilder()
                                        .setHst(permanent.getHeader().getHst().toBuilder()
                                                .setDefiniteTime(System.currentTimeMillis())
                                                .setTentative(false)))
                .build());
                if (permanent.getHeader().getHeight() % 1000 == 0) {
                    logger.info(format("[#%d-C[%d]] Deliver [[height=%d], [sender=%d], [channel=%d], [size=%d]]",
                            getID(), worker, permanent.getHeader().getHeight(), permanent.getHeader().getBid().getPid(),
                            permanent.getHeader().getM().getChannel(),
                            permanent.getDataCount()));
                }


                DBUtils.writeBlockToTable(permanent);
                Data.evacuateOldData(worker, permanent.getHeader().getM());
                communication.data.Data.evacuateOldData(worker, permanent.getId());

                if (currHeight % 10 == 0) {
                    Data.evacuateAllOldData(worker, permanent.getHeader().getM());
                    communication.data.Data.evacuateAllOldData(worker, permanent.getId());
                }
                BCS.notifyOnNewDifiniteBlock(worker);
            }

            logger.debug(String.format("[#%d-C[%d]] adds new Block with [height=%d] [cidSeries=%d ;" +
                            " cid=%d] [size=%d]",
                    getID(), worker, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock
                            .getDataCount()));

//                newBlockNotifyer.notifyAll();


            updateLeaderAndHeight();
            BCS.writeNextToDiskAsync(worker);
        }
        return false;
    }

    void removeFromPendings(BlockHeader recHeader, Block recBlock) {
        if (currLeader == getID() && !recHeader.getEmpty()) {
            logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
                    getID(), worker, recBlock.getHeader().getBid().getPid(), currHeight, cidSeries, cid));
//                synchronized (blocksForPropose) {
//                    blocksForPropose.remove();
//                }
            synchronized (proposedBlocks) {
                proposedBlocks.remove();
            }
        }
    }

    abstract BlockHeader leaderImpl() throws InterruptedException;

//    abstract public Blockchain initBC(int id, int channel);

//    abstract public Blockchain getBC(int start, int end);
//
//    abstract public Blockchain getEmptyBC();

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
            if (currBLock.getDataCount() + 1 > maxTransactionInBlock) return null;
            int cbid = currBLock.getId().getBid();
            int txnum = currBLock.getDataCount();
            Transaction ntx = tx.toBuilder()
                    .setId(txID.newBuilder()
                            .setProposerID(getID())
                            .setBid(cbid)
                            .setTxNum(txnum)
                            .setChannel(worker))
//                    .setServerTs(System.currentTimeMillis())
                    .build();
            if (validateTransactionWRTBlock(currBLock, ntx, v)) {
                currBLock.addData(ntx);
            }
            if (currBLock.getDataCount() == maxTransactionInBlock) {
                synchronized (blocksForPropose) {
                    blocksForPropose.add(currBLock.build());
                    currBLock = configureNewBlock();
                    blocksForPropose.notifyAll();
                }
            }
            return ntx.getId();
        }

    }


    txID addTransaction(byte[] data, int clientID) {
        Transaction tx = Transaction.newBuilder()
                .setClientID(clientID)
                .setData(ByteString.copyFrom(data)).build();
        return addTransaction(tx);

    }

    int status(txID tid, boolean blocking) throws InterruptedException {
        if (getTx(tid, blocking) != null) return 0;
        return -1;
    }

//    Transaction getTx(txID tid, boolean blocking) throws InterruptedException {
//        synchronized (newBlockNotifyer) {
//            Transaction tx = getTx(tid);
//            while (blocking && (tx == null)) {
//                newBlockNotifyer.wait();
//                tx = getTx(tid);
//            }
//            return tx;
//        }
//
//    }

    public Transaction getTx(txID tid, boolean blocking) throws InterruptedException {
        if (CacheUtils.contains(tid)) {
            return CacheUtils.get(tid);
        }
        int height = DBUtils.getBlockRecord(tid.getChannel(), tid.getProposerID(), tid.getBid(), blocking);
        if (height == -1) return null;
        Block b;
        if (blocking) {
            b = BCS.bGetBlock(worker, height);
        } else {
            b = BCS.nbGetBlock(worker, height);
        }

        if (b == null) return null;
        Transaction tx = b.getData(tid.getTxNum());
        if (tx.getId().equals(tid)) {
            CacheUtils.add(tx);
            return tx;
        } else {
            logger.error(format("Invalid tx [w=%d ; pid=%d ; bid=%d ; tid=%d]",
                    tid.getChannel(), tid.getProposerID(), tid.getBid(), tid.getTxNum()));
        }
        return null;
    }

    void createTxToBlock() {
        int missingTxs = -1;
        synchronized (proposedBlocks) {
            if (proposedBlocks.size() > 5) {
                return;
            }
        }
        synchronized (cbl) {
//            synchronized (blocksForPropose) {
//                if (blocksForPropose.isEmpty()) {
                    missingTxs = maxTransactionInBlock - currBLock.getDataCount();
//                }
//            }
        }
        if (missingTxs >  0) {
            for (int i = 0 ; i < missingTxs ; i++) {
//                long ts = System.currentTimeMillis();
                SecureRandom random = new SecureRandom();
                byte[] tx = new byte[txSize];
                random.nextBytes(tx);
                addTransaction(Transaction.newBuilder()
                        .setClientID(cID)
//                        .setClientTs(ts)
//                        .setServerTs(ts)
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

    void sendCurrentBlockIfNeeded() {
        synchronized (cbl) {
            if (currBLock.getDataCount() == 0) return;
            synchronized (proposedBlocks) {
                if (proposedBlocks.size() > 0) return;
            }
            synchronized (blocksForPropose) {
                if (blocksForPropose.size() > 0) return;
                logger.debug(format("Sending block [bid=%d:%d ; size=%d]",
                        currBLock.getId().getPid(), currBLock.getId().getBid(), currBLock.getDataCount()));
                blocksForPropose.add(currBLock.build());
                currBLock = configureNewBlock();
                blocksForPropose.notifyAll();
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

            Statistics.updateSyncs();
            sync(forkPoint);

        }
    }
    private void announceFork(Block b) {
        logger.info(format("[#%d-C[%d]] possible fork! [height=%d]",
                getID(), worker, currHeight));
        ForkProof p = ForkProof.
                newBuilder().
                setCurr(b).
                setPrev(BCS.nbGetBlock(worker, BCS.lastIndex(worker))).
                setSender(getID()).
                build();
        Meta key = Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        ABService.broadcast(p.toByteArray(), key, Data.RBTypes.FORK);
    }

    Block deliver(int index) throws InterruptedException {
//        synchronized (newBlockNotifyer) {
//            while (index >= bc.getHeight() - (f + 1)) {
//                newBlockNotifyer.wait();
//            }
//        }
        Block b = BCS.bGetBlock(worker, index);
        CacheUtils.addBlock(b);
        return b;
    }

    Block nonBlockingdeliver(int index) {
//        synchronized (newBlockNotifyer) {
//            if (index >= bc.getHeight() - (f + 1)) {
//                return null;
//            }
//        }
        Block b = BCS.nbGetBlock(worker, index);
        CacheUtils.addBlock(b);
        return b;
    }

//    int bcSize() {
//        return max(bc.getHeight() - (f + 2), 0);
//    }

    private void disseminateChainVersion(int forkPoint) {
        subChainVersion.Builder sv = subChainVersion.newBuilder();
        if (currHeight < forkPoint - 1) {
            logger.debug(format("[#%d-C[%d]] too far behind... [r=%d ; curr=%d]"
                    , getID(), worker, forkPoint, currHeight));
        } else {
            int low = max(forkPoint - (f + 1), 0);
            int high = BCS.lastIndex(worker) + 1;
            logger.debug(format("[#%d-C[%d]] building a sub version [(inc) %d --> (exc) %d]"
                    , getID(), worker, low, high));
            for (Block b : BCS.getBlocks(worker, low, high)) {
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
                                && validateBlockHash(BCS.nbGetBlock(worker, forkPoint - (f + 2)), v.getV(0))).collect(Collectors.toList());
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
//        synchronized (newBlockNotifyer) {
        for (int i = sPoint[0] ; i <= BCS.lastIndex(worker) ; i++) {
            BCS.removeBlock(worker, i);
        }

        BCS.setBlocks(worker, choosen[0].getVList(), sPoint[0]);
        BCS.notifyOnNewDifiniteBlock(worker);
//            newBlockNotifyer.notify();

//        }

        currLeader = (BCS.nbGetBlock(worker, BCS.lastIndex(worker)).getHeader().getBid().getPid() + 1) % n;
        currHeight = BCS.lastIndex(worker) + 1;
        cid = 0;
        cidSeries++;
        logger.debug(format("[#%d-C[%d]] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), worker, currHeight, currLeader, cidSeries));


    }

//    boolean isBcValid() {
//        return bc.isValid();
//    }

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
