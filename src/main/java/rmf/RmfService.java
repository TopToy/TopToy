package rmf;

import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import consensus.bbc.bbcService;
import crypto.DigestMethod;
import crypto.blockDigSig;
import crypto.sslUtils;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;

import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import proto.Types.*;
import proto.*;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.String.format;

public class RmfService extends RmfGrpc.RmfImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfService.class);
    class authInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                     Metadata metadata,
                                                                     ServerCallHandler<ReqT, RespT> serverCallHandler) {
            ServerCall.Listener res = new ServerCall.Listener() {};
//            try {
//                String peerDomain = serverCall.getAttributes()
//                        .get(Grpc.TRANSPORT_ATTR_SSL_SESSION).getPeerPrincipal().getName().split("=")[1];
////                int peerId = Integer.parseInt(Objects.requireNonNull(metadata.get
//                        (Metadata.Key.of("id", ASCII_STRING_MARSHALLER))));
                String peerIp = Objects.requireNonNull(serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)).
                        toString().split(":")[0].replace("/", "");
                int peerId = Integer.parseInt(Objects.requireNonNull(metadata.get
                        (Metadata.Key.of("id", ASCII_STRING_MARSHALLER))));
//                logger.debug(format("[#%d] peerDomain=%s", id, peerIp));
//                if (nodes.get(peerId).getAddr().equals(peerIp)) {
                    return serverCallHandler.startCall(serverCall, metadata);
//                }
//            } catch (SSLPeerUnverifiedException e) {
//                logger.error(format("[#%d]", id), e);
//            } finally {
//                return res;
//            }
//            return res;

        }
    }

    class clientTlsIntercepter implements ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
                                                                   CallOptions callOptions,
                                                                   Channel channel) {
            return new ForwardingClientCall.
                    SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {
                        @Override
                        public void start(Listener<RespT> responseListener, Metadata headers) {
                            headers.put( Metadata.Key.of("id", ASCII_STRING_MARSHALLER), String.valueOf(id));
                            super.start(responseListener, headers);
                }
            };
        }
    }
    class fvote {
        int cid;
        int cidSeries;
        ArrayList<Integer> voters;
        BbcDecision.Builder dec = BbcDecision.newBuilder();
    }
    class peer {
        ManagedChannel channel;
        RmfGrpc.RmfStub stub;

        peer(Node node) {
            try {
                channel = sslUtils.buildSslChannel(node.getAddr(), node.getRmfPort(),
                        sslUtils.buildSslContextForClient(Config.getCaRootPath(),
                                Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath())).
                        intercept(new clientTlsIntercepter()).build();
            } catch (SSLException e) {
                logger.fatal(format("[#%d]", id), e);
            }
//            channel = ManagedChannelBuilder.
//                    forAddress(node.getAddr(), node.getRmfPort()).
//                    usePlaintext().
//                    build();
            stub = RmfGrpc.newStub(channel);
        }

        void shutdown() {
//            try {
                channel.shutdown();//. awaitTermination(1, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                logger.fatal(format("[#%d]", id), e);
//            }
        }
    }
//    int channel;
    protected int id;
    private int n;
    protected int f;
    private bbcService bbcService;
//    private final Object globalLock = new Object();

    private final ConcurrentHashMap<Meta, Block>[] recMsg;

    private final ConcurrentHashMap<Meta, fvote>[] fVotes;
    private final ConcurrentHashMap<Meta, Block>[] pendingMsg;
    private final ConcurrentHashMap<Meta, BbcDecision>[] fastBbcCons;
    Object[] fastVoteNotifyer;
    Object[] msgNotifyer;
    Map<Integer, peer> peers;
    private List<Node> nodes;
    private Thread bbcServiceThread;
//    private Thread[] bbcMissedConsensusThreads;
//    protected Server server;
    private String bbcConfig;
    private final ConcurrentHashMap<Meta, BbcDecision>[] regBbcCons;
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private Server rmfServer;
    int channels;
    Executor executor;
//    EventLoopGroup beg;
    EventLoopGroup weg;
    int[] alive;
//    Object[] aliveLock;
    int tmo;
    int tmoInterval;
    int[] currentTmo;
    AtomicInteger totalDeliveredTries = new AtomicInteger(0);
    AtomicInteger optimialDec = new AtomicInteger(0);;
//    ReentrantLock mutex = new ReentrantLock(true);

//    public RmfService(int channels, int id, int f, ArrayList<Node> nodes, bbcService bbc) {
//        this.channels = channels;
//        this.globalLock = new Object[channels];
//        this.fVotes = new HashBasedTable[channels];
//        this.pendingMsg =  new HashBasedTable[channels];
//        this.recMsg =  new HashBasedTable[channels];
//        this.bbcService = bbc;
//        this.peers = new HashMap<>();
//        this.f = f;
//        this.n = 3*f +1;
//        this.id = id;
//        this.nodes = nodes;
//        this.fastBbcCons = new HashBasedTable[channels];
//        this.regBbcCons = new HashBasedTable[channels];
//        for (int i = 0 ; i < channels ; i++) {
//            globalLock[i] = new Object();
//            recMsg[i] = HashBasedTable.create();
//            pendingMsg[i] = HashBasedTable.create();
//            this.fVotes[i] = HashBasedTable.create();
//            fastBbcCons[i] = HashBasedTable.create();
//            regBbcCons[i] = HashBasedTable.create();
//        }
//        startGrpcServer();
//    }

    public RmfService(int channels, int id, int f, int tmo, int tmoInterval, ArrayList<Node> nodes, String bbcConfig,
                      String serverCrt, String serverPrivKey, String caRoot) {
        this.channels = channels;
        this.tmo = tmo;
        this.tmoInterval = tmoInterval;
        this.currentTmo = new int[channels];
//        this.globalLock = new Object[channels];
        this.bbcConfig = bbcConfig;
        this.fVotes = new ConcurrentHashMap[channels];
        this.pendingMsg =  new ConcurrentHashMap[channels];
        this.recMsg =  new ConcurrentHashMap[channels];

        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.id = id;
        this.nodes = nodes;
        this.fastBbcCons = new ConcurrentHashMap[channels];
        this.regBbcCons = new ConcurrentHashMap[channels];
//        this.bbcMissedConsensusThreads = new Thread[channels];
        this.alive = new int[channels];
//        this.aliveLock = new Object[channels];
        fastVoteNotifyer = new Object[channels];
        msgNotifyer = new Object[channels];
        for (int i = 0 ; i < channels ; i++) {
            fastVoteNotifyer[i] = new Object();
            msgNotifyer[i] = new Object();
            recMsg[i] = new ConcurrentHashMap<>();
            pendingMsg[i] = new ConcurrentHashMap<>();
            fVotes[i] = new ConcurrentHashMap<>();
            fastBbcCons[i] = new ConcurrentHashMap<>();
            regBbcCons[i] = new ConcurrentHashMap<>();
            alive[i] = tmo;
//            aliveLock[i] = new Object();
            this.currentTmo[i] = tmo;

        }
        startGrpcServer(serverCrt, serverPrivKey, caRoot);
    }


    private void startGrpcServer(String serverCrt, String serverPrivKey, String caRoot) {
        try {
//            rmfServer = ServerBuilder.forPort(nodes.get(id).getRmfPort())
//                    .addService(this)
//                    .build()
//                    .start();
//           executor = new ThreadPoolExecutor(1, // core size
//                   100, // max size
//                   10*60, // idle timeout
//                   TimeUnit.SECONDS,
//                   new ArrayBlockingQueue<Runnable>(20));
            int cores = Runtime.getRuntime().availableProcessors();
            logger.debug(format("[#%d] There are %d CPU's in the system", id, cores));
            executor = Executors.newFixedThreadPool(channels * n);
//            beg = new NioEventLoopGroup(cores + 1);
            weg = new NioEventLoopGroup( 1);
            rmfServer = NettyServerBuilder.
                    forPort(nodes.get(id).getRmfPort()).
                    executor(executor)
                    .workerEventLoopGroup(weg)
                    .bossEventLoopGroup(weg)
                    .sslContext(sslUtils.buildSslContextForServer(serverCrt,
                            caRoot, serverPrivKey)).
                    addService(this).
                    intercept(new authInterceptor()).
                    build().
                    start();
        } catch (IOException e) {
            logger.fatal(format("[#%d]", id), e);
        }

    }

    public void start() {
        CountDownLatch latch = new CountDownLatch(1);
//        if (!group)  {
            this.bbcService = new bbcService(channels, id, 2*f + 1, bbcConfig);
            bbcServiceThread = new Thread(() -> {
                /*
                    Note that in case that there is more than one bbc server this call blocks until all servers are up.
                 */
                this.bbcService.start();
                latch.countDown();
            }
            );
            bbcServiceThread.start();
//        }

//        for (int i = 0 ; i < channels ; i++) {
//            int finalI = i;
//            bbcMissedConsensusThreads[i] = new Thread(() ->
//            {
//                try {
//                    bbcMissedConsensus(finalI);
//                } catch (InterruptedException e) {
//                    logger.error(format("[#%d-C[%d]]", id, finalI), e);
//                }
//            });
//            bbcMissedConsensusThreads[i].start();
//        }

        try {
//            if (!group)
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
        logger.debug(format("[#%d] initiates grpc clients", id));
        logger.debug(format("[#%d] starting rmf service", id));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (stopped.get()) return;
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.warn(format("[#%d] *** shutting down rmf service since JVM is shutting down", id));
            shutdown();
        }));
    }

    long getTotalDeliveredTries() {
        return totalDeliveredTries.get();
    }

    long getOptimialDec() {
        return optimialDec.get();
    }
    private void bbcMissedConsensus(int channel) throws InterruptedException {
//        int sleepFor = tmo;
//        while (!stopped) {
//            synchronized (aliveLock[channel]) {
//                alive[channel] = currentTmo[channel]; // TODO: Hard coded time out
//                sleepFor = alive[channel];
//                while (sleepFor > 0) {
//                    alive[channel] = 0;
//                    logger.debug(format("[#%d]-C[%d]] will sleep for [%d] ms", id, channel, sleepFor));
//                    aliveLock[channel].wait(sleepFor);
//                    sleepFor = alive[channel];
//                }
//            }
//
//            if (fastBbcCons[channel].isEmpty()) continue;
////            synchronized (fastBbcCons[channel]) {
////                if (fastBbcCons[channel].rowKeySet().isEmpty()) {
//////                    logger.debug(format("[#%d] There are no fast bbc", id));
////                    continue;
////                }
//
//            try {
//                int fcidSeries = Collections.max(fastBbcCons[channel]
//                        .keySet()
//                        .stream()
//                       // .filter(m -> m.getChannel() == channel)
//                        .map(Meta::getCidSeries)
//                        .collect(Collectors.toList()));
//                int fcid = Collections.max(fastBbcCons[channel]
//                        .keySet()
//                        .stream()
//                        .filter(m -> m.getCidSeries() == fcidSeries)
//                        .map(Meta::getCid)
//                        .collect(Collectors.toList()));
//                Meta key = Meta.newBuilder()
//                        .setChannel(channel)
//                        .setCidSeries(fcidSeries)
//                        .setCid(fcid)
//                        .build();
//                logger.debug(format("[#%d-C[%d]] starting missed consensus for [cidSeries=%d ; cid=%d]",
//                        id, channel, fcidSeries, fcid));
//
////                logger.debug(format("[#%d-C[%d]] Trying re-participate [cidSeries=%d ; cid=%d]", id, channel, fcidSeries, fcid));
//                bbcService.periodicallyVoteMissingConsensus(fastBbcCons[channel].get(key));
//            } catch (Exception e) {
//                logger.error("", e);
//            }

//        }
//

//        }
    }

    public void shutdown() {
        stopped.set(true);
        for (peer p : peers.values()) {
            p.shutdown();
        }

        logger.debug(format("[#%d] shutting down rmf clients", id));
//        if (!group) {
            if (bbcService != null) bbcService.shutdown();
//        }

        try {
            if (bbcServiceThread != null) {
                bbcServiceThread.interrupt();
                bbcServiceThread.join();
            }
//            if (bbcMissedConsensusThreads != null) {
//                for (int i = 0 ; i < channels ; i++) {
//                    if (bbcMissedConsensusThreads[i] != null) {
//                        bbcMissedConsensusThreads[i].interrupt();
//                        bbcMissedConsensusThreads[i].join();
//                    }
//                }
//            }

        } catch (InterruptedException e) {
            logger.error("", e);
        }
        if (rmfServer != null) {
//            beg.shutdownNow();
//            weg.shutdownNow();
            rmfServer.shutdown();
        }
        logger.info(format("[#%d] shutting down rmf service", id));
    }

    void sendDataMessage(RmfGrpc.RmfStub stub, Block msg) {
        stub.disseminateMessage(msg, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty ans) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }

    private void sendFastVoteMessage(RmfGrpc.RmfStub stub, BbcMsg v) {
        stub.fastVote(v, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty empty) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }
    private void sendReqMessage(RmfGrpc.RmfStub stub, Req req, int channel, int cidSeries, int cid, int sender, int height) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                Meta key = Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .build();
                if (res.getData().getHeader().getM().getCid() == cid &&
                        res.getData().getHeader().getM().getCidSeries() == cidSeries
                        && res.getM().getChannel() == channel &&
                        res.getData().getHeader().getM().getChannel() == channel) {
//                    synchronized (recMsg[channel]) {
                        if (recMsg[channel].containsKey(key)) return;
                        if (pendingMsg[channel].containsKey(key)) return;
//                    }
//                    synchronized (pendingMsg[channel]) {
//                        if (!pendingMsg[channel].contains(cidSeries, cid)) {
                            if (!blockDigSig.verify(sender, res.getData())) {
                                logger.debug(format("[#%d-C[%d]] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                        id, channel, res.getM().getSender(), cidSeries, cid));
                                return;
                            }
                            logger.debug(format("[#%d-C[%d]] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, channel, res.getM().getSender(), cidSeries,  cid));
                            synchronized (msgNotifyer[channel]) {
                                pendingMsg[channel].put(key, res.getData());
                                msgNotifyer[channel].notify();
                            }
//                        }
//                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }
    private void broadcastReqMsg(Req req, int channel, int cidSeries, int cid, int sender, int height) {
        logger.debug(format("[#%d-C[%d]] broadcasts request message [cidSeries=%d ; cid=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender, height);
        }
    }
    private void broadcastFastVoteMessage(BbcMsg v) {
        for (int p : peers.keySet()) {
            sendFastVoteMessage(peers.get(p).stub, v);
        }
    }

    void rmfBroadcast(Block msg) {
        for (peer p : peers.values()) {
            sendDataMessage(p.stub, msg);
        }
    }
    private void addToPendings(Block request) {
        int sender = request.getHeader().getM().getSender();
        if (!blockDigSig.verify(sender, request)) return;
        int cid = request.getHeader().getM().getCid();
        int channel = request.getHeader().getM().getChannel();
        int cidSeries = request.getHeader().getM().getCidSeries();
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
//        synchronized (recMsg[channel]) {
          if (recMsg[channel].containsKey(key)) return;
//        }
//        synchronized (pendingMsg[channel]) {
        synchronized (msgNotifyer[channel]) {
            pendingMsg[channel].putIfAbsent(key, request);
            msgNotifyer[channel].notify();
        }

//        if (pendingMsg.containsKey(key)) return;
//        pendingMsg[channel].put(cidSeries, cid, request);
        logger.debug(format("[#%d-C[%d]] has received data message from [%d], [cidSeries=%d ; cid=%d]"
                , id, channel, sender, cidSeries, cid));
//        msgNotifyer[channel].release();
//        }
    }

    @Override
    public void disseminateMessage(Block request, StreamObserver<Empty> responseObserver) {

//        int cid = request.getMeta().getCid();
//        int cidSeries = request.getMeta().getCidSeries();
//        synchronized (globalLock) {
//            synchronized (fastBbcCons) {
//                if (fastBbcCons.contains(cidSeries, cid) || regBbcCons.contains(cidSeries, cid)) return;
//            }
            addToPendings(request);
//            globalLock.notify();
//        }
    }

    @Override
    public void fastVote(BbcMsg request, StreamObserver<Empty> responeObserver) {
//        long start = System.currentTimeMillis();
        if (request.hasNext()) {
            addToPendings(request.getNext());
        }
        int cid = request.getM().getCid();
        int cidSeries = request.getM().getCidSeries();
        int channel = request.getM().getChannel();
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();


        fVotes[channel].computeIfAbsent(key, k -> {
            fvote v = new fvote();
            v.dec.setM(Meta.newBuilder()
                    .setChannel(channel)
                    .setCidSeries(cidSeries)
                    .setCid(cid));
            v.cid = cid;
            v.cidSeries = cidSeries;
            v.voters = new ArrayList<>();
            return v;
        });

        if (fVotes[channel].get(key).voters.size() + 1 == n) {
            synchronized (fastVoteNotifyer[channel]) {
                fVotes[channel].computeIfPresent(key, (k, v) -> {
                    if (regBbcCons[channel].containsKey(key)) return v;
                    if (fastBbcCons[channel].containsKey(key)) return v;
                    int sender = request.getM().getSender();
                    if (v.voters.contains(sender)) {
                        logger.debug(format("[#%d-C[%d]] has received duplicate vote from [%d] for [channel=%d ; cidSeries=%d ; cid=%d]",
                                id, channel, sender, channel, cidSeries, cid));
                        return v;
                    }
                    v.voters.add(sender);
                    if (v.voters.size() == n) {
                        bbcService.updateFastVote(v.dec.setDecosion(1).build());
                        fastBbcCons[channel].put(k, v.dec.setDecosion(1).build());
                        fastVoteNotifyer[channel].notify();
                        logger.debug(format("[#%d-C[%d]] has received n fast votes for [channel=%d ; cidSeries=%d ; cid=%d]",
                                id, channel, channel, cidSeries, cid));
                    }
                    return v;
                });
            }
        } else if (fVotes[channel].get(key).voters.size() + 1 < n){
                fVotes[channel].computeIfPresent(key, (k, v) -> {
                    if (regBbcCons[channel].containsKey(key)) return v;
                    if (fastBbcCons[channel].containsKey(key)) return v;
                    int sender = request.getM().getSender();
                    if (v.voters.contains(sender)) {
                        logger.debug(format("[#%d-C[%d]] has received duplicate vote from [%d] for [channel=%d ; cidSeries=%d ; cid=%d]",
                                id, channel, sender, channel, cidSeries, cid));
                        return v;
                    }
                    v.voters.add(sender);
                    return v;
                });
            }

//        }
//        if (fVotes[channel].get(key).voters.size() == n) {
//            synchronized (fastVoteNotifyer[channel]) {
//
//            }
//        }
//        logger.debug(format("[#%d-C[%d]] fastVote took about %d ms on %d [cidSeries=%d ; cid=%d]",
//                id, channel, System.currentTimeMillis() - start, fVotes.get(key).voters.size(), cidSeries, cid));
    }

    @Override
    public void reqMessage(Req request,  StreamObserver<Res> responseObserver)  {
        Block msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        Meta key =  Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        msg = recMsg[channel].get(key);
//        synchronized (recMsg[channel]) {
//            msg = recMsg[channel].get(cidSeries, cid);
//        }
        if (msg == null) {
            msg = pendingMsg[channel].get(key);
//            synchronized (pendingMsg[channel]) {
//                msg = pendingMsg[channel].get(cidSeries, cid);
//            }
        }
        if (msg != null) {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d]",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));
            Meta meta = Meta.newBuilder().
                    setSender(id).
//                    setHeight(msg.getMeta().getHeight()).
                    setChannel(channel).
                    setCid(cid).
                    setCidSeries(cid).
                    build();
            responseObserver.onNext(Res.newBuilder().
                    setData(msg).
                    setM(meta).
                    build());
        } else {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));
        }
    }

    public Block deliver(int channel, int cidSeries, int cid, int sender, int height, Block next)
            throws InterruptedException
    {
//        mutex.lock();
        totalDeliveredTries.getAndIncrement();
        long startTime = System.currentTimeMillis();
        long estimatedTime;
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        synchronized (msgNotifyer[channel]) {
            if (!pendingMsg[channel].containsKey(key)) {
//                mutex.unlock();
                msgNotifyer[channel].wait(currentTmo[channel]);
//                mutex.lock();
            }
        }

        estimatedTime = System.currentTimeMillis() - startTime;
        logger.debug(format("[#%d-C[%d]] have waited [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                id, channel, estimatedTime, cidSeries, cid));
        pendingMsg[channel].computeIfPresent(key, (k, val) -> {
            long start = System.currentTimeMillis();
            if (val.getHeader().getM().getSender() != sender ||
                    val.getHeader().getM().getCidSeries() != cidSeries ||
                    val.getHeader().getM().getCid() != cid ||
                    val.getHeader().getM().getChannel() != channel) return val;
            BbcMsg.Builder bv = BbcMsg
                    .newBuilder()
                    .setM(Meta.newBuilder()
                            .setChannel(channel)
                            .setCidSeries(cidSeries)
                            .setCid(cid)
                            .setSender(id)
                            .build())
                    .setVote(1);
            if (next != null) {
                logger.debug(format("[#%d-C[%d]] broadcasts [cidSeries=%d ; cid=%d] via fast mode",
                        id, channel, next.getHeader().getM().getCidSeries(), next.getHeader().getM().getCid()));
                long st = System.currentTimeMillis();
                bv.setNext(setFastModeData(val, next));
                logger.debug(format("[#%d-C[%d]] creating new block of [cidSeries=%d ; cid=%d] took about [%d] ms",
                        id, channel, next.getHeader().getM().getCidSeries(), next.getHeader().getM().getCid(),
                        System.currentTimeMillis() - st));
            }
            broadcastFastVoteMessage(bv.build());
            logger.debug(format("[#%d-C[%d]] sending fast vote [cidSeries=%d ; cid=%d] took about[%d] ms",
                    id, channel, cidSeries, cid, System.currentTimeMillis() - start));
            return val;
        });

//        startTime = System.currentTimeMillis();
//        long waitStartTime = System.currentTimeMillis();
//        fvote fv = fVotes.get(key);
//        while (fv == null || fv.voters.size() < n) {
//            if (System.currentTimeMillis() - startTime >= tmo) break;
//            fv = fVotes.get(key);
//        }
//        logger.debug(format("[#%d-C[%d]] have waited for more [%d] ms for fast bbc [cidSeries=%d ; cid=%d]", id, channel,
//                    System.currentTimeMillis() - waitStartTime, cidSeries, cid));
        startTime = System.currentTimeMillis();
        synchronized (fastVoteNotifyer[channel]) {
            fvote fv = fVotes[channel].get(key);
            if (currentTmo[channel] - estimatedTime > 0 && (fv == null || fv.voters.size() < n)) {
                logger.debug(format("[#%d-C[%d]] will wait at most [%d] ms for fast bbc", id, channel,
                        max(currentTmo[channel] - estimatedTime, 1)));
//                mutex.unlock();
                fastVoteNotifyer[channel].wait(currentTmo[channel] - estimatedTime);
//                mutex.lock();
            }
            logger.debug(format("[#%d-C[%d]] have waited for more [%d] ms for fast bbc", id, channel,
                    System.currentTimeMillis() - startTime));
        }

        currentTmo[channel] += tmoInterval;

        fVotes[channel].computeIfPresent(key, (k, val) -> {
            int vote = 0;
//            regBbcCons[channel].remove(k);

            if (pendingMsg[channel].containsKey(k)) {
                currentTmo[channel] = tmo;
                vote = 1;
            }
            int votes = val.voters.size();
            if (votes < n) {
                regBbcCons[channel].put(k, BbcDecision.newBuilder().setDecosion(vote).buildPartial());
                return val;
            }
            logger.debug(format("[#%d-C[%d]] deliver by fast vote [cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
            return val;
        });

        fVotes[channel].computeIfAbsent(key, k -> {
            fvote vi = new fvote();
            vi.dec.setM(Meta.newBuilder()
                    .setChannel(channel)
                    .setCidSeries(cidSeries)
                    .setCid(cid));
            vi.cid = cid;
            vi.cidSeries = cidSeries;
            vi.voters = new ArrayList<>();
            regBbcCons[channel].put(k, BbcDecision.newBuilder().setDecosion(0).buildPartial());
//            fullBbcConsensus(channel, 0, cidSeries, cid);
            return vi;
        });

        if (currentTmo[channel] != tmo) {
            logger.debug(format("[#%d-C[%d]] unable to receive message, timeout increased to [%d] ms", id, channel, currentTmo[channel]));
        }
        if (regBbcCons[channel].containsKey(key)) {
            int v = regBbcCons[channel].get(key).getDecosion();
            int dec = fullBbcConsensus(channel, v, cidSeries, cid);
            logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 0) {
                pendingMsg[channel].remove(key);
//                mutex.unlock();
//                synchronized (aliveLock[channel]) {
//                    alive[channel] = currentTmo[channel];
//                }
                return null;
            }
        }  else {
            optimialDec.getAndIncrement();
        }

//        synchronized (aliveLock[channel]) {
//            alive[channel] = currentTmo[channel];
//        }

        requestData(channel, cidSeries, cid, sender, height);
        Block msg = pendingMsg[channel].get(key);
        pendingMsg[channel].remove(key);
        recMsg[channel].put(key, msg);
//        mutex.unlock();
        return msg;
    }

    private int fullBbcConsensus(int channel, int vote, int cidSeries, int cid) throws InterruptedException {
        logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, vote));
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        bbcService.propose(vote, channel, cidSeries, cid);
//        mutex.unlock();
        BbcDecision dec = bbcService.decide(channel, cidSeries, cid);
//        mutex.lock();
        regBbcCons[channel].remove(key);
        regBbcCons[channel].put(key, dec);
        return dec.getDecosion();
    }

    private void requestData(int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (pendingMsg[channel].containsKey(key) &&
                pendingMsg[channel].get(key).getHeader().getM().getSender() == sender) return; //&&
//                pendingMsg.get(cidSeries, cid).getMeta().getHeight() == height) return;
        pendingMsg[channel].remove(key);
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setChannel(channel)
                .setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req,channel,  cidSeries, cid, sender, height);
        synchronized (msgNotifyer[channel]) {
            Block msg = pendingMsg[channel].get(key);
            while ( msg == null ||
                   msg.getHeader().getM().getSender() != sender) { // ||
//                pendingMsg.get(cidSeries, cid).getMeta().getHeight() != height) {
                pendingMsg[channel].remove(key);
                msg = pendingMsg[channel].get(key);
//                mutex.unlock();
                msgNotifyer[channel].wait();
//                mutex.lock();
            }
        }

    }

//    String getMessageSig(int channel, int cidSeries, int cid) {
//        Meta key = Meta.newBuilder()
//                .setChannel(channel)
//                .setCidSeries(cidSeries)
//                .setCid(cid)
//                .build();
//        if (recMsg.containsKey(key)) {
//            return recMsg.get(key).getSig();
//        }
//        return null;
//    }


    public void clearBuffers(Meta key) {
        int channel = key.getChannel();

        recMsg[channel].remove(key);
        fVotes[channel].remove(key);
        pendingMsg[channel].remove(key);
        fastBbcCons[channel].remove(key);
        regBbcCons[channel].remove(key);

        bbcService.clearBuffers(key);
    }

    private Block setFastModeData(Block curr, Block next) {
        Block.Builder nextBuilder = next
                    .toBuilder()
                    .setHeader(next.getHeader().toBuilder()
                    .setPrev(ByteString
                            .copyFrom(DigestMethod
                                    .hash(curr.getHeader().toByteArray())))
                            .build());

        return nextBuilder.setHeader(nextBuilder.getHeader().toBuilder()
                .setProof(blockDigSig.sign(next.getHeader())).build()).build();

    }
    
}
