package servers;

import blockchain.data.BCS;
import blockchain.validation.Tvalidator;
import blockchain.validation.Validator;
import com.google.protobuf.ByteString;
import communication.CommLayer;
import utils.config.Config;
import das.ab.ABService;
import das.data.Data;
import das.ms.BFD;
import das.wrb.WRB;
import proto.types.client;
import utils.cache.TxCache;
import utils.DBUtils;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import proto.types.block.*;
import proto.types.transaction.*;
import proto.types.meta.*;
import proto.types.forkproof.*;
import proto.types.version.*;

import static blockchain.Utils.*;
import static java.lang.Math.max;
import static java.lang.String.format;
import static utils.commons.createMeta;

import utils.statistics.Statistics;

public abstract class ToyBaseServer {
    enum syncStates {
        ACCEPTED,
        RECEIVED_VERSIONS,
        DONE
    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyBaseServer.class);
    private int id;
    int currHeight;
    protected int f;
    protected int n;
    int currLeader;
    int version = 0;
    protected AtomicBoolean stopped = new AtomicBoolean(false);
    private int maxTransactionInBlock;
    private Thread mainThread;
    private Thread panicThread;
    private Thread commSendThread;
    int cid = 0;
    int cidSeries = 0;
    private final HashMap<Integer, syncStates> fp;
    boolean fastMode = false;
    int worker;
    private boolean testing = Config.getTesting();
    private int txPoolMax = 10000;
    private int txSize = 0;
    private int cID = new Random().nextInt(10000);
    private int bid = 0;
    final Queue<Block> blocksForPropose = new LinkedList<>();
    final Queue<Block> proposedBlocks = new LinkedList<>();
    private Block.Builder currBLock;
    private final Object cbl = new Object();
    private boolean intCatched = false;
    CommLayer comm;
    private Validator v = new Tvalidator();
    private CountDownLatch startLatch = new CountDownLatch(3);

    private void initThreads() {
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

    ToyBaseServer(int id, int worker, int n, int f, int maxTx, CommLayer comm) {
        this.id = id;
        this.f = f;
        this.n = n;
        this.comm = comm;
        currHeight = 1; // starts from 1 due to the genesis BaseBlock
        currLeader = 0;
        this.maxTransactionInBlock = maxTx;
        fp = new HashMap<>();
        initThreads();
        this.fastMode = false;
        this.worker = worker;
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
        logger.info(format("Initiated ToyBaseServer: [id=%d; n=%d; f=%d; worker=%d]", id, n, f, worker));
    }

    int getID() {
        return id;
    }

    private Block.Builder configureNewBlock() {
        return Block.newBuilder()
                .setId(BlockID.newBuilder().setPid(getID()).setBid(++bid).build());
    }


    void serve() throws InterruptedException {
        commSendThread.start();
        logger.debug(format("[#%d-C[%d]] starts commSendThread thread", getID(), worker));
        panicThread.start();
        logger.debug(format("[#%d-C[%d]] starts panic thread", getID(), worker));
        mainThread.start();
        logger.debug(format("[#%d-C[%d]] starts main thread", getID(), worker));
        startLatch.await();
        logger.info(format("[#%d-C[%d]] starts serving", getID(), worker));
    }

    void shutdown() {
        stopped.set(true);
        try {
            logger.info(format("[#%d-C[%d]] interrupt panic thread", getID(), worker));
            panicThread.interrupt();
            panicThread.join();
            logger.info(format("[#%d-C[%d]] interrupt commSendThread thread", getID(), worker));
            commSendThread.interrupt();
            commSendThread.join();
            logger.info(format("[#%d-C[%d]] interrupt main thread", getID(), worker));
            if (!mainThread.isInterrupted()) {
                mainThread.interrupt();
                mainThread.join();
            }
        } catch (InterruptedException e) {
            logger.error(format("[#%d-C[%d]]", getID(), worker), e);

        }

    }

    private void updateLeaderAndHeight() {
        currHeight = BCS.lastIndex(worker) + 1;
        currLeader = (currLeader + 1) % n;
        cid++;
    }

    private boolean checkSyncEvent() {
        synchronized (fp) {
            if (fp.containsKey(currHeight) && fp.get(currHeight) == syncStates.RECEIVED_VERSIONS) {
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
            fastMode = false;
            synchronized (fp) {
                intCatched = true;
                fp.notifyAll();
            }
        }
    }


    private Block getBlockWRTheader(BlockHeader h, int channel) throws InterruptedException {
        if (h.getEmpty()) {
            logger.debug(format("received an empty block, returning a match [w=%d ; " +
                            "cidSereis=%d ; cid=%d ; height=%d ; pid=%d ; bid=%d]",
                    channel, h.getM().getCidSeries(), h.getM().getCid(), h.getHeight(),
                    h.getBid().getPid(), h.getBid().getBid()));

            return Block.newBuilder().setBst(
                    BlockStatistics.newBuilder()
                            .setProposeTime(System.currentTimeMillis())
                            .build()
            ).build();
        }
        return comm.recBlock(channel, h);
    }

    BlockHeader getHeaderForCurrentBlock(BlockHeader prev,
                                         int height, int cidSeries, int cid) {
        synchronized (cbl) {
            synchronized (proposedBlocks) {
                if (proposedBlocks.isEmpty()) {
                    Block b = configureNewBlock().build();
                    logger.debug(format("[#%d-C[%d]] creates an empty header [height=%d ; cidSeries=%d ; cid=%d ; bid=%d]",
                            getID(), worker, height, cidSeries, cid, b.getId().getBid()));
//                    currBLock = configureNewBlock();
                    return createBlockHeader(b, prev, getID(), height, cidSeries, cid, worker, b.getId(), version);
                }

                Block b = proposedBlocks.element();
                logger.debug(format("[#%d-C[%d]] creates header for [height=%d ; cidSeries=%d ; cid=%d ; bid=%d]",
                        getID(), worker, height, cidSeries, cid, b.getId().getBid()));
                return createBlockHeader(b, prev, getID(), height, cidSeries, cid, worker, b.getId(), version);
            }
        }

    }

    private boolean mainLoop() {

        while (!stopped.get()) {
            if (Thread.interrupted()) return true;
            checkSyncEvent();
            while (!BCS.validateCurrentLeader(worker, currLeader, f)) {
                logger.info(format("[#%d-C[%d]] invalid leader [l=%d, h=%d]",
                        getID(), worker, currLeader, currHeight));
                currLeader = (currLeader + 1) % n;
                fastMode = false;
                cid += 1;
                BFD.deactivate(worker);
            }
            BlockHeader next;
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
                recHeader = WRB.WRBDeliver(worker, cidSeries, cid, currLeader, currHeight, version, next);
                logger.debug(format("[#%d-C[%d]] WRB deliver took about [%d] ms [cidSeries=%d ; cid=%d ; height=%d ; dec=%b]",
                        getID(), worker, System.currentTimeMillis() - startTime, cidSeries, cid, currHeight, recHeader != null));

            } catch (InterruptedException e) {
                logger.info(format("[#%d-C[%d]] main thread has been interrupted on wrb deliver " +
                                "[height=%d, cidSeries=%d ; cid=%d]",
                        getID(), worker, currHeight, cidSeries, cid));
                return true;
            }

            fastMode = true;
            if (recHeader == null) {
                logger.info(format("C[%d] unable to deliver header " +
                        "[height=%d; leader=%d; cidSeries=%d ; cid=%d]", worker, currHeight, currLeader, cidSeries, cid));
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

            BCS.addBlock(worker, recBlock.toBuilder()
                        .setHeader(recBlock.getHeader().toBuilder()
                                    .setHst(recBlock.getHeader().getHst().toBuilder()
                                            .setTentativeTime(System.currentTimeMillis())
                                            .setTentative(true)))
                    .build());

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
                BCS.notifyOnNewDefiniteBlock();
            }

            logger.debug(String.format("[#%d-C[%d]] adds new Block with [height=%d] [cidSeries=%d ;" +
                            " cid=%d] [size=%d]",
                    getID(), worker, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock
                            .getDataCount()));

            updateLeaderAndHeight();
            BCS.writeNextToDiskAsync(worker);
        }
        return false;
    }

    void removeFromPendings(BlockHeader recHeader, Block recBlock) {
        if (currLeader == getID() && !recHeader.getEmpty()) {
            logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
                    getID(), worker, recBlock.getHeader().getBid().getPid(), currHeight, cidSeries, cid));
            synchronized (proposedBlocks) {
                proposedBlocks.poll();
            }
        }
    }

    abstract BlockHeader leaderImpl() throws InterruptedException;

    int getTxPoolSize() {
        synchronized (cbl) {
            synchronized (blocksForPropose) {
                if (blocksForPropose.size() == 0) return currBLock.getDataCount();
                int ret = 0;
                for (Block b : blocksForPropose) {
                    ret += b.getDataCount();
                }
                return ret + currBLock.getDataCount();
            }
        }

    }

    int getPendingSize() {
        synchronized (proposedBlocks) {
            int ret = 0;
            for (Block b : proposedBlocks) {
                ret += b.getDataCount();
            }
            return ret;
        }
    }

    TxID addTransaction(Transaction tx) {
        if (getTxPoolSize() > txPoolMax) return TxID.getDefaultInstance();
        synchronized (cbl) {
            if (currBLock.getDataCount() + 1 > maxTransactionInBlock) return TxID.getDefaultInstance();
            int cbid = currBLock.getId().getBid();
            int txnum = currBLock.getDataCount();
            Transaction ntx = tx.toBuilder()
                    .setId(TxID.newBuilder()
                            .setProposerID(getID())
                            .setBid(cbid)
                            .setTxNum(txnum)
                            .setChannel(worker))
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


    TxID addTransaction(byte[] data, int clientID) {
        Transaction tx = Transaction.newBuilder()
                .setClientID(clientID)
                .setData(ByteString.copyFrom(data)).build();
        return addTransaction(tx);

    }

    client.TxState status(TxID tid, boolean blocking) throws InterruptedException {
        if (getTx(tid, blocking) != null) return client.TxState.COMMITTED;
        synchronized (cbl) {
            if (currBLock.getDataList().stream().anyMatch(tx -> tx.getId().equals(tid))) return client.TxState.PENDING;
        }
        synchronized (blocksForPropose) {
            if (blocksForPropose.stream().anyMatch(
                    b -> b.getDataList().stream().anyMatch(tx -> tx.getId().equals(tid)))
            ) return client.TxState.PENDING;
        }
        synchronized (proposedBlocks) {
            if (proposedBlocks.stream().anyMatch(
                    b -> b.getDataList().stream().anyMatch(tx -> tx.getId().equals(tid)))
            ) return client.TxState.PROPOSED;
        }
        return client.TxState.UNKNOWN;
    }

    public Transaction getTx(TxID tid, boolean blocking) throws InterruptedException {
        if (TxCache.contains(tid)) {
            return TxCache.get(tid);
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
            TxCache.add(tx);
            return tx;
        } else {
            logger.error(format("Invalid tx [w=%d ; pid=%d ; bid=%d ; tid=%d]",
                    tid.getChannel(), tid.getProposerID(), tid.getBid(), tid.getTxNum()));
        }
        return null;
    }

    private void createTxToBlock() {
        int missingTxs = -1;
        synchronized (proposedBlocks) {
            if (proposedBlocks.size() > 5) {
                return;
            }
        }
        synchronized (cbl) {
             missingTxs = maxTransactionInBlock - currBLock.getDataCount();
        }
        if (missingTxs >  0) {
            for (int i = 0 ; i < missingTxs ; i++) {
                SecureRandom random = new SecureRandom();
                byte[] tx = new byte[txSize];
                random.nextBytes(tx);
                addTransaction(Transaction.newBuilder()
                        .setClientID(cID)
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
            if (fp.containsKey(forkPoint) && fp.get(forkPoint) == syncStates.DONE)
            {
                logger.debug(format("[#%d-C[%d]] already synchronized for this point [r=%d]",
                        getID(), worker, forkPoint));
                return;
            }
            logger.info(format("C[%d] starting handle fork [fp=%d]", worker, forkPoint));

            if (forkPoint - (f + 1) <= currHeight) {
                logger.info(format("[#%d-C[%d]] interrupting main thread [r=%d]"
                        , getID(),worker, forkPoint));
                mainThread.interrupt();
                while (!intCatched) { // TODO: deadlock alert!!
                    fp.wait();
                }
                mainThread.join();
                intCatched = false;
            }

            if (!fp.containsKey(forkPoint)) {
                fp.put(forkPoint, syncStates.ACCEPTED);
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
        ABService.broadcast(p.toByteArray(), b.getHeader().getM(), Data.RBTypes.FORK);
        logger.info(format("[#%d-C[%d]] done announce fork [height=%d]",
                getID(), worker, currHeight));
    }

    Block deliver(int index) throws InterruptedException {
        Block b = BCS.bGetBlock(worker, index);
        TxCache.addBlock(b);
        return b;
    }

    Block nonBlockingdeliver(int index) {
        Block b = BCS.nbGetBlock(worker, index);
        TxCache.addBlock(b);
        return b;
    }

    private void disseminateChainVersion(int forkPoint) {
        SubChainVersion.Builder sv = SubChainVersion.newBuilder();
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
        Meta key = createMeta(worker, cid, cidSeries, version);
        ABService.broadcast(sv.build().toByteArray(), key, Data.RBTypes.SYNC);
    }

    abstract void potentialBehaviourForSync() throws InterruptedException;

    private void sync(int forkPoint) throws InterruptedException, IOException {
        logger.info(format("[#%d-C[%d]] starts sync method with [r=%d]", getID(),worker, forkPoint));
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
        logger.info(format("C[%d] done waiting to sub versions", worker));
        fp.replace(forkPoint, syncStates.RECEIVED_VERSIONS);

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
        final SubChainVersion[] choosen = new SubChainVersion[1];
        final int[] sPoint = {0};
        AtomicBoolean noMatch = new AtomicBoolean(false);
        synchronized (Data.syncRBData[worker]) {
            Data.syncRBData[worker].computeIfPresent(forkPoint, (k, v1)-> {
                ArrayList<SubChainVersion> ret = new ArrayList<>();
                ret.add(SubChainVersion.getDefaultInstance());
                if (v1.stream().noneMatch(v -> v.getVCount() > 0)) {
                    // A Byzantine nodes may trigger fork while all correct nodes are unable to send
                    // a valid versions.
                    noMatch.set(true);
                    logger.error(format("all versions are empties) [fp=%d] ", forkPoint));
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
                    // A Byzantine nodes may trigger fork while all correct nodes are unable to send
                    // a valid versions.
                    noMatch.set(true);
                    return ret;
                }
                choosen[0] = (SubChainVersion) valid.stream().
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
        for (int i = sPoint[0] ; i <= BCS.lastIndex(worker) ; i++) {
            BCS.removeBlock(worker, i);
        }

        BCS.setBlocks(worker, choosen[0].getVList(), sPoint[0]);
        BCS.notifyOnNewDefiniteBlock();

        currLeader = (BCS.nbGetBlock(worker, BCS.lastIndex(worker)).getHeader().getBid().getPid() + 1) % n;
        currHeight = BCS.lastIndex(worker) + 1;
        cid = 0;
        cidSeries++;
        logger.debug(format("[#%d-C[%d]] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
                , getID(), worker, currHeight, currLeader, cidSeries));

        fp.replace(forkPoint, syncStates.DONE);
        synchronized (Data.syncRBData[worker]) {
            for (Iterator<Map.Entry<Integer, List<SubChainVersion>>> it =
                 Data.syncRBData[worker].entrySet().iterator();
                 it.hasNext(); ) {
                int p = it.next().getKey();
                if (fp.containsKey(p) && fp.get(p) == syncStates.DONE) {
                    logger.info(format("C[%d] Removing versions of fp=%d]", worker, p));
                    it.remove();
                }
            }
        }
        resetState();
    }

    void resetState() {
        logger.debug(format("C[%d] - removing old data", worker));
        Data.evacuateAllOldData(worker, createMeta(worker, cid, cidSeries, version));
        BlockID bid;
        synchronized (cbl) {
            bid = currBLock.getId();
            currBLock = configureNewBlock();
        }
        communication.data.Data.evacuateAllOldData(worker, bid);
        synchronized (blocksForPropose) {
            blocksForPropose.clear();
            synchronized (proposedBlocks) {
                proposedBlocks.clear();
            }
        }
    }

    void reconfigure() {

    }


}
