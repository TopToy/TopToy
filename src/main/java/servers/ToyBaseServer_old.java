//package servers;
//
//import blockchain.BaseBlock;
//import blockchain.BaseBlockchain;
//import com.google.protobuf.ByteString;
//import com.google.protobuf.InvalidProtocolBufferException;
//import config.Config;
//import config.Node;
//import das.RBroadcast.RBrodcastService;
//import crypto.blockDigSig;
//import proto.*;
//import das.wrb.WrbNode;
//import proto.Types.*;
//import utils.DiskUtils;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.security.SecureRandom;
//import java.util.*;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.stream.Collectors;
//
//import static java.lang.Math.max;
//import static java.lang.String.format;
//import static utils.DiskUtils.createStorageDir;
//
//public abstract class ToyBaseServer extends Node {
//    class fpEntry {
//        ForkProof fp;
//        boolean done;
//    }
//    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ToyBaseServer.class);
//    WrbNode rmfServer;
//    private RBrodcastService panicRB;
//    private RBrodcastService syncRB;
//    final BaseBlockchain bc;
//    int currHeight;
//    protected int f;
//    protected int n;
//    int currLeader;
//    protected AtomicBoolean stopped = new AtomicBoolean(false);
////    final Object blockLock = new Object();
//    BaseBlock currBlock;
//    private final Object newBlockNotifyer = new Object();
//    private int maxTransactionInBlock;
////    private int tmo;
////    private int tmoInterval;
////    private int initTmo;
//    private Thread mainThread;
//    private Thread panicThread;
////    private Thread gcThread;
//    int cid = 0;
//    int cidSeries = 0;
//    private final HashMap<Integer, fpEntry> fp;
//    private HashMap<Integer, ArrayList<Types.subChainVersion>> scVersions;
////    private final ArrayList<Types.Transaction> transactionsPool = new ArrayList<>();
//    private final ConcurrentLinkedQueue<Transaction> transactionsPool = new ConcurrentLinkedQueue<>();
////    private final ConcurrentLinkedQueue<String> proposedTx = new ConcurrentLinkedQueue<>();
////    private final HashMap<String, Transaction> approvedTx = new HashMap<>();
//    boolean configuredFastMode;
//    boolean fastMode;
//    int channel;
////    final Integer[] lastReceivedBlock;
////    int lastGc = 0;
//    boolean testing = Config.getTesting();
//    int txPoolMax = 100000;
//    int bareTxSize = Transaction.newBuilder()
//            .setClientID(0)
//            .setId(txID.newBuilder().setTxID(UUID.randomUUID().toString()).build())
//            .build().getSerializedSize() + 8;
//    int txSize = 0;
//    int cID = new Random().nextInt(10000);
//    Path sPath = Paths.get("disk", String.valueOf(channel));
//
//    ExecutorService storageWorker = Executors.newSingleThreadExecutor();
////    Semaphore txSem = new Semaphore(txPoolMax);
//
//
//    public ToyBaseServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
//                         int maxTx, boolean fastMode, ArrayList<Node> cluster,
//                         WrbNode rmf, RBrodcastService panic, RBrodcastService sync) {
//        super(addr, rmfPort, id);
//        this.f = f;
//        this.n = 3*f + 1;
//        this.panicRB = panic;
//        this.syncRB = sync;
//        bc = initBC(id, channel);
//        currBlock = null;
//        currHeight = 1; // starts from 1 due to the genesis BaseBlock
//        currLeader = 0;
////        this.tmo = tmo;
////        this.tmoInterval = tmoInterval;
////        initTmo = tmo;
//        this.maxTransactionInBlock = maxTx;
//        fp = new HashMap<>();
//        scVersions = new HashMap<>();
//        mainThread = new Thread(() -> {
//            try {
//                mainLoop();
//            } catch (Exception ex) {
//                logger.error(format("[#%d-C[%d]]", getID(), channel), ex);
//                shutdown(true);
//            }
//        });
//        panicThread = new Thread(this::deliverForkAnnounce);
////        gcThread = new Thread(() -> {
////            try {
////                gc();
////            } catch (InterruptedException e) {
////                logger.error("", e);
////            }
////        });
//        this.configuredFastMode = fastMode;
//        this.fastMode = fastMode;
//        this.channel = channel;
//        rmfServer = rmf;
////        this.lastReceivedBlock = new Integer[n];
////        Arrays.fill(lastReceivedBlock, 0);
//        currLeader = channel % n;
//
//        if (testing) {
//            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
//        }
//    }
//
//    public ToyBaseServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
//                         int maxTx, boolean fastMode, ArrayList<Node> cluster,
//                         String bbcConfig, String panicConfig, String syncConfig,
//                         String serverCrt, String serverPrivKey, String caRoot) {
//
//        super(addr, rmfPort, id);
//        this.f = f;
//        this.n = 3*f + 1;
//        rmfServer = new WrbNode(1, id, addr, rmfPort, f, tmo, tmoInterval,
//               cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
//        panicRB = new RBrodcastService(1, id, panicConfig);
//        syncRB = new RBrodcastService(1, id, syncConfig);
//        bc = initBC(id, channel);
//        currBlock = null;
//        currHeight = 1; // starts from 1 due to the genesis BaseBlock
//        currLeader = 0;
////        this.tmo =  tmo;
////        this.tmoInterval = tmoInterval;
////        initTmo = tmo;
//        this.maxTransactionInBlock = maxTx;
//        fp = new HashMap<>();
//        scVersions = new HashMap<>();
//        mainThread = new Thread(this::mainLoop);
//        panicThread = new Thread(this::deliverForkAnnounce);
////        gcThread = new Thread(() -> {
////            try {
////                gc();
////            } catch (InterruptedException e) {
////                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
////            }
////        });
//        this.configuredFastMode = fastMode;
//        this.fastMode = fastMode;
////        this.lastReceivedBlock = new Integer[n];
////        Arrays.fill(lastReceivedBlock, 0);
//        currLeader = channel % n;
//
//        if (testing) {
//            txSize = StrictMath.max(0, Config.getTxSize() - bareTxSize);
//        }
//
//    }
//
//    public void start(boolean group) {
//        if (!group) {
//            rmfServer.start();
//            logger.debug(format("[#%d-C[%d]] wrb server is up", getID(), channel));
//            syncRB.start();
//            logger.debug(format("[#%d-C[%d]] sync server is up", getID(), channel));
//            panicRB.start();
//            logger.debug(format("[#%d-C[%d]] panic server is up", getID(), channel));
//        }
//        logger.info(format("[#%d-C[%d]] is up", getID(), channel));
//    }
//
//    public void serve() {
//        panicThread.start();
//        logger.debug(format("[#%d-C[%d]] starts panic thread", getID(), channel));
//        mainThread.start();
//        logger.debug(format("[#%d-C[%d]] starts main thread", getID(), channel));
////        gcThread.start();
////        logger.debug(format("[#%d-C[%d]] gc thread is up", getID(), channel));
//        logger.info(format("[#%d-C[%d]] starts serving", getID(), channel));
//    }
//
//    public void shutdown(boolean group) {
//        stopped.set(true);
//        logger.debug(format("[#%d-C[%d]] interrupt main thread", getID(), channel));
////        AtomicBoolean joined = new AtomicBoolean(false);
////        Thread t = new Thread(() -> {
////            while (!joined.get())
////        });
////        t.start();
//        AtomicBoolean j = new AtomicBoolean(false);
//        long start = System.currentTimeMillis();
////            Object lock = new Object();
//        Thread t = new Thread(() -> {
//            while (!j.get()) {
//                mainThread.interrupt();
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            }
//        });
//        t.start();
//        try {
//            mainThread.join();
//            j.set(true);
////            t.interrupt();
//            t.join();
//        } catch (InterruptedException e) {
//            logger.error(format("[#%d-C[%d]]", getID(), channel), e);
//        }
//
//        if (!group) {
//            if (rmfServer != null) rmfServer.stop();
//            if (panicRB != null) panicRB.shutdown();
//            if (syncRB != null) syncRB.shutdown();
//        }
//        logger.debug(format("[#%d-C[%d]] interrupt panic thread", getID(), channel));
//        j.set(false);
//        t = new Thread(() -> {
//            while (!j.get()) {
//                panicThread.interrupt();
//                try {
//                    Thread.sleep( 1000);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            }
//        });
//        t.start();
//
//        try {
//            panicThread.join();
//            j.set(true);
//            t.join();
//        } catch (InterruptedException e) {
//            logger.error(format("[#%d-C[%d]]", getID(), channel), e);
//        }
////        txSem.release(txPoolMax);
//        logger.info(format("[#%d-C[%d]] shutdown bc server", getID(), channel));
//    }
//    private void updateLeaderAndHeight() {
//        currHeight = bc.getHeight() + 1;
//        currLeader = (currLeader + 1) % n;
//        cid++;
//    }
//
//     void gc(int index) throws IOException {
//        logger.debug(format("[#%d-C[%d]] clear buffers of [height=%d]", getID(), channel, index));
////        if (bc.getBlock(index) == null) return;
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
////
//    }
//
////    void clearBuffers(int index) {
////
////    }
//    private void mainLoop() throws RuntimeException {
//        boolean interrupted = false;
//        while (!stopped.get()) {
////            synchronized (bc) {
//                synchronized (fp) {
//                    for (Integer key : fp.keySet().stream().filter(k -> k < currHeight).collect(Collectors.toList())) {
//                        fpEntry pe = fp.get(key);
//                        if (!pe.done) {
//                            logger.info(format("[#%d-C[%d]] have found panic message [height=%d] [fp=%d]",
//                                    getID(), channel, currHeight, key));
//                            handleFork(pe.fp);
//                            fp.get(key).done = true;
//                            fastMode = false;
//                        }
//
//                    }
//                }
//                while (!bc.validateCurrentLeader(currLeader, f)) {
//                    currLeader = (currLeader + 1) % n;
//                    fastMode = false;
//                    cid += 2; // I think we have no reason to increase CID.
//                }
//                Block next = null;
//
//                try {
//                    if (!interrupted) {
//                        next = leaderImpl();
//                        interrupted = false;
//                    }
//                } catch (InterruptedException e) {
//                    interrupted = true;
//                    logger.debug(format("[#%d-C[%d]] main thread has been interrupted on leader impl",
//                            getID(), channel));
//                    continue;
//                }
//                Block recBlock;
//                try {
//                    long startTime = System.currentTimeMillis();
//                    recBlock = rmfServer.deliver(channel, cidSeries, cid, currHeight, currLeader, next);
//                    logger.debug(format("[#%d-C[%d]] deliver took about [%d] ms [cidSeries=%d ; cid=%d]",
//                            getID(), channel, System.currentTimeMillis() - startTime, cidSeries, cid));
//                } catch (InterruptedException e) {
//                    logger.debug(format("[#%d-C[%d]] main thread has been interrupted on wrb deliver " +
//                                    "[height=%d, cidSeries=%d ; cid=%d]",
//                            getID(), channel, currHeight, cidSeries, cid));
////                    if (stopped) return;
//                    continue;
//                }
//
//                fastMode = configuredFastMode;
//                if (recBlock == null) {
//                    currLeader = (currLeader + 1) % n;
//                    fastMode = false;
//                    cid += 2;
//                    continue;
//                }
//
//                if (currLeader == getID()) {
//                    logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
//                            getID(), channel, recBlock.getHeader().getM().getSender(), currHeight, cidSeries, cid));
//                    currBlock = null;
//                }
//                if (!bc.validateBlockHash(recBlock)) {
////                    bc.validateBlockHash(recBlock);
//                    announceFork(recBlock);
//                    fastMode = false;
//                    continue;
//                }
//
//                synchronized (newBlockNotifyer) {
//
//                    recBlock = recBlock
//                            .toBuilder()
//                            .setSt(recBlock.getSt()
//                                    .toBuilder()
//                                    .setChannelDecided(System.currentTimeMillis()))
//                            .build();
//                    bc.addBlock(recBlock);
//                    if (recBlock.getHeader().getHeight() - (f + 2) > 0) {
//                        Block permanent = bc.getBlock(recBlock.getHeader().getHeight() - (f + 2));
//                        permanent = permanent.toBuilder().setSt(permanent.getSt().toBuilder().setPd(System.currentTimeMillis())).build();
//                        try {
//                            bc.setBlock(recBlock.getHeader().getHeight() - (f + 2), permanent);
//                        } catch (IOException e) {
//                            logger.error(String.format("[#%d-C[%d]] unable to record permanent time for block [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
//                                    getID(), channel, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));
//                        }
////                        permanent = bc.getBlock(recBlock.getHeader().getHeight() - (f + 2));
//
//
//                    }
//
//                    logger.debug(String.format("[#%d-C[%d]] adds new Block with [height=%d] [cidSeries=%d ; cid=%d] [size=%d]",
//                            getID(), channel, recBlock.getHeader().getHeight(), cidSeries, cid, recBlock.getDataCount()));
//                    newBlockNotifyer.notify();
//
//                }
//                // TODO: Improve mechanism
////              lastReceivedBlock[recBlock.getHeader().getM().getSender()] = recBlock.getHeader().getHeight();
////              gc();     lastReceivedBlock[recBlock.getHeader().getM().getSender()] = recBlock.getHeader().getHeight();
////                if (bc.getHeight() - f >= 0) {
//////                    synchronized (approvedTx) {
//////                        addApprovedTransactions(bc.getBlock(bc.getHeight() - f).getDataList());
//////                    }
////                }
//
//                if (currLeader == getID()) {
//                    removeFromProposed(recBlock.getDataList());
//                }
//                updateLeaderAndHeight();
//            }
////        }
//    }
//
//    private void writeExec(Block b) throws IOException {
//
//    }
//
//    abstract Block leaderImpl() throws InterruptedException;
//
//    abstract public BaseBlockchain initBC(int id, int channel);
//
//    abstract public BaseBlockchain getBC(int start, int end);
//
//    public int getTxPoolSize() {
//        return transactionsPool.size();
//    }
//
//    public Types.txID addTransaction(Transaction tx) {
//        if (transactionsPool.size() > txPoolMax) return null;
//        transactionsPool.add(tx);
//        return tx.getId();
//    }
//
//    public String addTransaction(byte[] data, int clientID) {
////        txSem.acquire();
//        if (transactionsPool.size() > txPoolMax) return "";
//        String txID = UUID.randomUUID().toString();
//        Transaction t = Transaction.newBuilder()
//                .setClientID(clientID)
//                .setId(Types.txID.newBuilder().setTxID(txID).build())
//                .setData(ByteString.copyFrom(data))
//                .build();
////        synchronized (transactionsPool) {
////            logger.debug(format("[#%d-C[%d]] adds transaction from [client=%d ; txID=%s]", getID(), channel, t.getClientID(), t.getTxID()));
//            transactionsPool.add(t);
////        }
//        return txID;
//    }
//    private void addApprovedTransactions(List<Transaction> txs) {
////        synchronized (approvedTx) {
////            for (Transaction tx :txs) {
////                approvedTx.put(tx.getTxID(), tx);
////            }
////        }
//
//    }
//    public int isTxPresent(String txID) {
////        synchronized (approvedTx) {
////            if (approvedTx.containsKey(txID)) {
////                return 2;
////            }
////        }
//////        synchronized (transactionsPool) {
////            if (transactionsPool.stream().map(Transaction::getTxID).anyMatch(tid -> tid.equals(txID))) {
////                return 0;
////            }
////            if (proposedTx.contains(txID)) {
////                return 1;
////            }
//////        }
////        ArrayList<Block> cbc;
////        synchronized (bc) {
////            cbc = new ArrayList<Block>(bc.getBlocks(bc.getHeight() - f + 1, bc.getHeight() + 1));
////        }
////        if (cbc.stream()
////                .map(Block::getDataList)
////                .anyMatch(tl -> tl
////                        .stream()
////                        .map(Transaction::getTxID)
////                        .anyMatch(tid -> tid.equals(txID)))) {
////            return 3;
////        }
//        return -1;
//    }
//
//    void createTxToBlock() {
//        while (currBlock.getTransactionCount() < maxTransactionInBlock) {
//            long ts = System.currentTimeMillis();
//            SecureRandom random = new SecureRandom();
//            byte[] tx = new byte[txSize];
//            random.nextBytes(tx);
//            currBlock.addTransaction(Transaction.newBuilder()
//                    .setId(txID.newBuilder().setTxID(UUID.randomUUID().toString()).build())
//                    .setClientID(cID)
//                    .setClientTs(ts)
//                    .setServerTs(ts)
//                    .setData(ByteString.copyFrom(tx))
//                    .build());
//        }
//
//    }
//
//    void addTransactionsToCurrBlock() {
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
//            if (testing) {
//                createTxToBlock();
//            }
//    }
//
//    void removeFromProposed(List<Transaction> txs) {
////        proposedTx.removeAll(txs.stream().map(Transaction::getTxID).collect(Collectors.toList()));
//    }
//
//    private int validateForkProof(ForkProof p)  {
//        logger.debug(format("[#%d-C[%d]] starts validating fp", getID(), channel));
//        Block curr = p.getCurr();
//        Block prev = p.getPrev();
//        int prevBlockH = prev.getHeader().getHeight();
//
//        if (bc.getBlock(prevBlockH).getHeader().getM().getSender() != prev.getHeader().getM().getSender()) {
//            logger.debug(format("[#%d-C[%d]] invalid fork proof #1", getID(), channel));
//            return -1;
//        }
//
//        int currCreator = currLeader;
//        if (bc.getHeight() >= curr.getHeader().getHeight()) {
//            currCreator = bc.getBlock(curr.getHeader().getHeight()).getHeader().getM().getSender();
//        }
//        if (currCreator != curr.getHeader().getM().getSender()) {
//            logger.debug(format("[#%d-C[%d]] invalid fork proof #2", getID(), channel));
//            return -1;
//        }
//        if (!blockDigSig.verify(curr.getHeader().getM().getSender(), curr)) {
//                logger.debug(format("[#%d-C[%d]] invalid fork proof #3", getID(), channel));
//                return -1;
//        }
////        if (!curr.getFooter().getOrig().isEmpty()) {
////            Data asInRmf = Data
////                    .newBuilder()
////                    .setMeta(Meta
////                            .newBuilder()
////                          //  .setHeight(curr.getHeader().getHeight())
////                            .setSender(curr.getHeader().getCreatorID())
////                            .setCidSeries(curr.getHeader().getCidSeries())
////                            .setCid(curr.getHeader().getCid())
////                            .setChannel(channel)
////                            .build())
////                    .setData(curr.getFooter().getOrig())
////                    .setSig(curr.getFooter().getRmfProof())
////                    .build();
////            if (!rmfDigSig.verify(curr.getHeader().getCreatorID(), asInRmf)) {
////                logger.debug(format("[#%d-C[%d]] invalid fork proof #6", getID(), channel));
////                return -1;
////            }
////        } else {
////            Block.Builder dataAsInRmf = Block.newBuilder()
////                    .setHeader(curr.getHeader());
////            for (int i = 0 ; i < curr.getDataList().size() ; i++ ) {
////                dataAsInRmf.addData(i, curr.getData(i));
////            }
////            Data asInRmf = Data
////                    .newBuilder()
////                    .setMeta(Meta
////                            .newBuilder()
////                            //  .setHeight(curr.getHeader().getHeight())
////                            .setSender(curr.getHeader().getCreatorID())
////                            .setCidSeries(curr.getHeader().getCidSeries())
////                            .setCid(curr.getHeader().getCid())
////                            .setChannel(channel)
////                            .build())
////                    .setData(dataAsInRmf.build().toByteString())
////                    .setSig(curr.getFooter().getRmfProof())
////                    .build();
////            if (!rmfDigSig.verify(curr.getHeader().getCreatorID(), asInRmf)) {
////                logger.debug(format("[#%d-C[%d]] invalid fork proof #3", getID(), channel));
////                return -1;
////            }
////        }
////        Block.Builder dataAsInRmf = Block.newBuilder()
////                .setHeader(prev.getHeader());
////        for (int i = 0 ; i < prev.getDataList().size() ; i++ ) {
////            dataAsInRmf.addData(i, prev.getData(i));
////        }
////        Data asInRmf = Data
////                .newBuilder()
////                .setMeta(Meta
////                        .newBuilder()
////                        //  .setHeight(curr.getHeader().getHeight())
////                        .setSender(prev.getHeader().getCreatorID())
////                        .setCidSeries(prev.getHeader().getCidSeries())
////                        .setCid(prev.getHeader().getCid())
////                        .setChannel(channel)
////                        .build())
////                .setData(dataAsInRmf.build().toByteString())
////                .setSig(prev.getFooter().getRmfProof())
////                .build();
////        if (!rmfDigSig.verify(prev.getHeader().getCreatorID(), asInRmf)) {
////            logger.debug(format("[#%d-C[%d]] invalid fork proof #4", getID(), channel));
////            return -1;
////        }
//        if (!blockDigSig.verify(prev.getHeader().getM().getSender(), prev)) {
//            logger.debug(format("[#%d-C[%d]] invalid fork proof #4", getID(), channel));
//            return -1;
//        }
//
//        logger.debug(format("[#%d-C[%d]] panic for fork is valid [fp=%d]", getID(), channel, p.getCurr().getHeader().getHeight()));
//        return prev.getHeader().getHeight();
//    }
//
//    private void deliverForkAnnounce() {
//        while (!stopped.get()) {
//            ForkProof p;
//            try {
//                p = ForkProof.parseFrom(panicRB.deliver(channel));
//            } catch (Exception e) {
//                logger.error(format("[#%d-C[%d]]", getID(), channel), e);
//                continue;
//            }
//            synchronized (fp) {
//                int pHeight = p.getCurr().getHeader().getHeight();
//                if (!fp.containsKey(pHeight)) {
//                    fpEntry fpe = new fpEntry();
//                    fpe.done = false;
//                    fpe.fp = p;
//                    fp.put(pHeight, fpe);
//                    logger.debug(format("[#%d-C[%d]] interrupts the main thread panic from [#%d]",
//                            getID(), channel, p.getSender()));
//                    mainThread.interrupt();
//                }
//            }
//        }
//    }
//    private void announceFork(Block b) {
//        logger.info(format("[#%d-C[%d]] possible fork! [height=%d]",
//                getID(), channel, currHeight));
//        ForkProof p = ForkProof.
//                newBuilder().
//                setCurr(b).
//                setPrev(bc.getBlock(bc.getHeight())).
//                setSender(getID()).
//                build();
//        synchronized (fp) {
//            int pHeight = p.getCurr().getHeader().getHeight();
//            if (!fp.containsKey(pHeight)) {
//                fpEntry fpe = new fpEntry();
//                fpe.done = false;
//                fpe.fp = p;
//                fp.put(pHeight, fpe);
//            }
//        }
//        panicRB.broadcast(p.toByteArray(), channel, getID());
//        handleFork(p);
//    }
//
//    public Block deliver(int index) throws InterruptedException {
//        synchronized (newBlockNotifyer) {
//            while (index >= bc.getHeight() - (f + 1)) {
//                newBlockNotifyer.wait();
//            }
//        }
//        return bc.getBlock(index);
//    }
//
//    public Block nonBlockingdeliver(int index) {
//        synchronized (newBlockNotifyer) {
//            if (index >= bc.getHeight() - (f + 1)) {
//                return null;
//            }
//        }
//        return bc.getBlock(index);
//    }
//
//    public int bcSize() {
//        return max(bc.getHeight() - (f + 2), 0);
//    }
//
//    private void handleFork(ForkProof p) {
//        if (validateForkProof(p) == -1) return;
//        int fpoint = p.getCurr().getHeader().getHeight();
//        logger.debug(format("[#%d-C[%d]] handleFork has been called [r=%d]", getID(), channel, fpoint));
//        try {
//            sync(fpoint);
//            synchronized (fp) {
//                fp.get(fpoint).done = true;
//            }
//        } catch (Exception e) {
//            logger.error(format("[#%d-C[%d]]", getID(), channel), e);
//        }
//
//    }
//
//    private int disseminateChainVersion(int forkPoint) throws IOException {
//        subChainVersion.Builder sv = subChainVersion.newBuilder();
//            int low = max(forkPoint - (f + 1), 0);
//            int high = bc.getHeight() + 1;
//            logger.debug(format("[#%d-C[%d]] building a sub version [(inc) %d --> (exc) %d]"
//                    , getID(), channel, low, high));
//            for (Block b : bc.getBlocks(low, high)) {
//                sv.addV(b);
//            }
//            sv.setSuggested(1);
//            sv.setSender(getID());
//            sv.setForkPoint(forkPoint);
//        return syncRB.broadcast(sv.build().toByteArray(), channel, getID());
//    }
//
//    private boolean validateSubChainVersion(subChainVersion v, int forkPoint) {
//        int lowIndex = v.getV(0).getHeader().getHeight();
//        int sPoint = max(forkPoint - (f + 1), 0);
//        if (lowIndex != sPoint) {//(lowIndex != forkPoint - (f + 1) && forkPoint >= (f + 1)) {
//            logger.debug(format("[#%d-C[%d]] invalid sub chain version, [lowIndex=%d != " +
//                            "r - (f + 1)=%d] [r=%d ; sender=%d]",
//                    getID(),channel, lowIndex, forkPoint - (f + 1), forkPoint, v.getSender()));
//            return false;
//        }
//
////        if (v.getVList().size() < f + 1 && forkPoint >= f) {
////            logger.debug(format("[#%d-C[%d]] invalid sub chain version, Block list size is smaller then f [size=%d] [fp=%d]",
////                    getID(), channel,v.getVList().size(), forkPoint));
////            return false;
////        }
//
//        BaseBlockchain lastSyncBC = getBC(max(lowIndex - (f + 1), 0), lowIndex);
//        for (Block pb : v.getVList()) {
//            if (!blockDigSig.verify(pb.getHeader().getM().getSender(), pb)) {
//                logger.debug(format("[#%d-C[%d]] invalid sub chain version, block [height=%d] digital signature is invalid " +
//                        "[fp=%d ; sender=%d]", getID(),channel, pb.getHeader().getHeight(), forkPoint, v.getSender()));
//                return false;
//            }
//            if (!BaseBlockchain.validateBlockHash(lastSyncBC.getBlock(lastSyncBC.getHeight()), pb)) {
//                logger.debug(format("[#%d-C[%d]] invalid sub chain version, block hash is invalid [height=%d] [fp=%d]",
//                        getID(),channel, pb.getHeader().getHeight(), forkPoint));
//                return false;
//            }
//            if (!lastSyncBC.validateBlockCreator(pb, f)) {
//                logger.debug(format("[#%d-C[%d]] invalid invalid sub chain version, block creator is invalid [height=%d] [fp=%d]",
//                        getID(),channel, pb.getHeader().getHeight(), forkPoint));
//                return false;
//            }
//            lastSyncBC.addBlock(pb);
//        }
//        return true;
//    }
//
//    private void sync(int forkPoint) throws IOException {
//        logger.info(format("[#%d-C[%d]] start sync method with [r=%d]", getID(),channel, forkPoint));
//        disseminateChainVersion(forkPoint);
//        if (scVersions.containsKey(forkPoint)) {
//            scVersions.get(forkPoint).removeIf(sv -> !validateSubChainVersion(sv, forkPoint));
//            logger.debug(format("[#%d-C[%d]] invalidated subversions...",
//                    getID(), channel));
//        }
//        while (!scVersions.containsKey(forkPoint) || scVersions.get(forkPoint).size() < 2*f + 1) {
//            subChainVersion v;
//            try {
//                v = subChainVersion.parseFrom(syncRB.deliver(channel));
//            } catch (InterruptedException e) {
//                if (!stopped.get()) {
//                    // might interrupted if more then one panic message received.
//                    logger.debug(format("[#%d-C[%d]] sync operation has been interrupted, try again...",
//                            getID(), channel));
//                    continue;
//                } else {
//                    return;
//                }
//            }
//            if (v == null) {
//                logger.debug(format("[#%d-C[%d]] Unable to parse sub chain version [fp=%d]]", getID(),channel, forkPoint));
//                continue;
//            }
//
//            if (v.getForkPoint() <= currHeight && !validateSubChainVersion(v, v.getForkPoint())) {
//                logger.debug(format("[#%d-C[%d]] Sub chain version is invalid [r=%d]]", getID(), channel,v.getForkPoint()));
//                continue;
//            }
//            if (!scVersions.containsKey(v.getForkPoint())) {
//                scVersions.put(v.getForkPoint(), new ArrayList<>());
//            }
//            scVersions.get(v.getForkPoint()).add(v);
//        }
//        int max = scVersions.get(forkPoint).
//                stream().
//                mapToInt(v -> v.getV(v.getVCount() - 1).getHeader().getHeight()).
//                max().getAsInt();
//        subChainVersion choosen = scVersions.
//                get(forkPoint).
//                stream().
//                filter(v -> v.getV(v.getVCount() - 1).getHeader().getHeight() == max).
//                findFirst().get();
//        int sPoint = max(forkPoint - (f + 1), 0);
//        logger.info(format("[#%d-C[%d]] adopts sub chain version [length=%d] from [#%d] " +
//                "range [%d ---> %d]",
//                getID(),channel, choosen.getVList().size(), choosen.getSender(), sPoint, max));
//        synchronized (newBlockNotifyer) {
//            bc.setBlocks(choosen.getVList(), sPoint);
////            bc.setBlocks(choosen.getVList(), forkPoint - 1);
//            if (!bc.validateBlockHash(bc.getBlock(bc.getHeight()))) { //||
////                    !bc.validateBlockData(bc.getBlock(bc.getHeight()))) {
//                logger.debug(format("[#%d-C[%d]] deletes a block [height=%d]", getID(),channel, bc.getHeight()));
//                bc.removeBlock(bc.getHeight()); // meant to handle the case in which the front is split between the to leading blocks
//            }
//            newBlockNotifyer.notify();
//        }
//        currLeader = (bc.getBlock(bc.getHeight()).getHeader().getM().getSender() + 2) % n;
//        currHeight = bc.getHeight() + 1;
//        cid = 0;
//        cidSeries++;
//        logger.debug(format("[#%d-C[%d]] post sync: [cHeight=%d] [cLeader=%d] [cidSeries=%d]"
//                , getID(), channel, currHeight, currLeader, cidSeries));
//    }
//
//}
