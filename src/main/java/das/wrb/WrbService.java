package das.wrb;

import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import das.bbc.BbcService;
import crypto.DigestMethod;
import crypto.blockDigSig;
import crypto.sslUtils;
import das.data.BbcDecData;
import das.data.GlobalData;
import das.data.VoteData;
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

import static java.lang.Math.max;
import static java.lang.String.format;

public class WrbService extends RmfGrpc.RmfImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WrbService.class);
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
            stub = RmfGrpc.newStub(channel);
        }

        void shutdown() {
            channel.shutdown();
        }
    }
    protected int id;
    private int n;
    protected int f;
    private BbcService bbcService;
//    private final Object globalLock = new Object();

//    private final ConcurrentHashMap<Meta, Block>[] recMsg;
    private final ConcurrentHashMap<Meta, List<Integer>>[] preConsVote;
    private final ConcurrentHashMap<Meta, VoteData>[] fvData;
    private final ArrayList<Meta>[] preConsDone;
//    private final ConcurrentHashMap<Meta, Block>[] pendingMsg;
//    private final ConcurrentHashMap<Meta, BbcDecision>[] fastBbcCons;
//    private final ConcurrentHashMap<Meta, BbcDecision>[] regBbcCons;
    private Object[] fastVoteNotifyer;
    private Object[] msgNotifyer;
    private final Object[] preConsNotifyer;
    Map<Integer, peer> peers;
    private List<Node> nodes;
    private Thread bbcServiceThread;
    private String bbcConfig;

    private AtomicBoolean stopped = new AtomicBoolean(false);
    private Server rmfServer;
    private int channels;
    private int tmo;
    private int tmoInterval;
    private int[] currentTmo;
    private AtomicInteger totalDeliveredTries = new AtomicInteger(0);
    private AtomicInteger optimialDec = new AtomicInteger(0);


    public WrbService(int channels, int id, int f, int tmo, int tmoInterval, ArrayList<Node> nodes, String bbcConfig,
                      String serverCrt, String serverPrivKey, String caRoot) {
        this.channels = channels;
        this.tmo = tmo;
        this.tmoInterval = tmoInterval;
        this.currentTmo = new int[channels];
//        this.globalLock = new Object[channels];
        this.bbcConfig = bbcConfig;
        this.fvData = new ConcurrentHashMap[channels];
//        this.pendingMsg =  new ConcurrentHashMap[channels];
//        this.recMsg =  new ConcurrentHashMap[channels];
        this.preConsVote = new ConcurrentHashMap[channels];
        this.preConsDone = new ArrayList[channels];
        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.id = id;
        this.nodes = nodes;
//        this.fastBbcCons = new ConcurrentHashMap[channels];
//        this.regBbcCons = new ConcurrentHashMap[channels];
//        this.bbcMissedConsensusThreads = new Thread[channels];
//        this.aliveLock = new Object[channels];
        this.fastVoteNotifyer = new Object[channels];
        this.msgNotifyer = new Object[channels];
        this.preConsNotifyer = new Object[channels];
        for (int i = 0 ; i < channels ; i++) {
            fastVoteNotifyer[i] = new Object();
            msgNotifyer[i] = new Object();
            preConsNotifyer[i] = new Object();
//            recMsg[i] = new ConcurrentHashMap<>();
//            pendingMsg[i] = new ConcurrentHashMap<>();
//            fVotes[i] = new ConcurrentHashMap<>();
//            fastBbcCons[i] = new ConcurrentHashMap<>();
//            regBbcCons[i] = new ConcurrentHashMap<>();
            fvData[i] = new ConcurrentHashMap<>();
            preConsVote[i] = new ConcurrentHashMap<>();
            preConsDone[i] = new ArrayList<>();
//            aliveLock[i] = new Object();
            this.currentTmo[i] = tmo;

        }
        new GlobalData(channels);
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
            Executor executor = Executors.newFixedThreadPool(channels * n);
//            beg = new NioEventLoopGroup(cores + 1);
            EventLoopGroup weg = new NioEventLoopGroup(cores);
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
            this.bbcService = new BbcService(channels, id, 2*f + 1, bbcConfig);
            bbcServiceThread = new Thread(() -> {
                /*
                    Note that in case that there is more than one bbc server this call blocks until all servers are up.
                 */
                this.bbcService.start();
                latch.countDown();
            });
            bbcServiceThread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
        logger.debug(format("[#%d] initiates grpc clients", id));
        logger.debug(format("[#%d] starting wrb service", id));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (stopped.get()) return;
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.warn(format("[#%d] *** shutting down wrb service since JVM is shutting down", id));
            shutdown();
        }));
    }

    long getTotalDeliveredTries() {
        return totalDeliveredTries.get();
    }

    long getOptimialDec() {
        return optimialDec.get();
    }
    public void shutdown() {
        stopped.set(true);
        for (peer p : peers.values()) {
            p.shutdown();
        }

        logger.debug(format("[#%d] shutting down wrb clients", id));
        if (bbcService != null) bbcService.shutdown();
        try {
            if (bbcServiceThread != null) {
                bbcServiceThread.interrupt();
                bbcServiceThread.join();
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        if (rmfServer != null) {
            rmfServer.shutdown();
        }
        logger.info(format("[#%d] shutting down wrb service", id));
    }

    void sendDataMessage(RmfGrpc.RmfStub stub, Block msg) {
        stub.disseminateMessage(msg, new StreamObserver<>() {
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
        stub.fastVote(v, new StreamObserver<>() {
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
    private void sendReqMessage(RmfGrpc.RmfStub stub, Req req, int channel, int cidSeries, int cid, int sender) {
        stub.reqMessage(req, new StreamObserver<>() {
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
                    if (GlobalData.received[channel].containsKey(key) ||
                            GlobalData.pending[channel].containsKey(key)) return;
                    if (!blockDigSig.verify(sender, res.getData())) {
                        logger.debug(format("[#%d-C[%d]] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                id, channel, res.getM().getSender(), cidSeries, cid));
                        return;
                    }
                    logger.debug(format("[#%d-C[%d]] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                            id, channel, res.getM().getSender(), cidSeries, cid));
                    synchronized (msgNotifyer[channel]) {
                        GlobalData.pending[channel].putIfAbsent(key, res.getData());
                        msgNotifyer[channel].notify();
                    }
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

    private void sendPreConsReqMessage(RmfGrpc.RmfStub stub, PreConsReq req, int channel, int cidSeries, int cid, int sender, int height) {
        stub.preConsReqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                Meta key = Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .build();
                if (res.getM().getChannel() != channel
                    || res.getM().getCidSeries() != cidSeries
                    || res.getM().getCid() != cid) return;
                preConsVote[channel].computeIfAbsent(key, k -> new ArrayList<>());
                if (preConsVote[channel].get(key).contains(res.getM().getSender())) return;
                preConsVote[channel].computeIfPresent(key, (k, val) -> {
                    if (val.size() > n - f) return val;
                    val.add(res.getM().getSender());
                    logger.debug(format("[#%d-C[%d]] received preCons response from [%d] [cidSeries=%d ; cid=%d]",
                            id, channel, res.getM().getSender(), cidSeries, cid));

                    if ((!res.getData().equals(Block.getDefaultInstance()))
                            && res.getData().getHeader().getM().getCid() == cid
                            && res.getData().getHeader().getM().getCidSeries() == cidSeries
                            && res.getData().getHeader().getM().getChannel() == channel
                            && res.getData().getHeader().getM().getSender() == sender) {
                        if (!blockDigSig.verify(sender, res.getData())) {
                            logger.debug(format("[#%d-C[%d]] has pre cons received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, channel, res.getM().getSender(), cidSeries, cid));
                            return val;
                        }
                        if (!GlobalData.pending[channel].containsKey(key) &&
                                !GlobalData.received[channel].containsKey(key)) {
                            logger.debug(format("[#%d-C[%d]] has pre das received message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, channel, res.getM().getSender(), cidSeries, cid));
                            GlobalData.pending[channel].putIfAbsent(key, res.getData());
                        }
                    } else if (res.getData().equals(Block.getDefaultInstance())) {
                        logger.debug(format("[#%d-C[%d]] has pre das received NULL message from [#%d] for [cidSeries=%d ; cid=%d]",
                                id, channel, res.getM().getSender(), cidSeries,  cid));
                    }

                    if (val.size() == n - f) {
                        synchronized (preConsNotifyer[channel]) {
                            preConsDone[channel].add(key);
                            logger.debug(format("[#%d-C[%d]] notify on preCons for [cidSeries=%d ; cid=%d]",
                                    id, channel, cidSeries,  cid));
                            preConsNotifyer[channel].notify();
                        }
                    }
                    return val;
                });




            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }

    private void broadcastReqMsg(Req req, int channel, int cidSeries, int cid, int sender) {
        logger.debug(format("[#%d-C[%d]] broadcasts request message [cidSeries=%d ; cid=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender);
        }
    }

    private void broadcastPreConsReqMsg(PreConsReq req, int channel, int cidSeries, int cid, int sender, int height) {
        logger.debug(format("[#%d-C[%d]] broadcasts pre cons request message [cidSeries=%d ; cid=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid));
        for (int p : peers.keySet()) {
//            if (p == id) continue;
            sendPreConsReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender, height);
        }
    }

    private void broadcastFastVoteMessage(BbcMsg v) {
        for (int p : peers.keySet()) {
            sendFastVoteMessage(peers.get(p).stub, v);
        }
    }

    void rmfBroadcast(Block msg) {
        msg = msg
                .toBuilder()
                .setSt(msg.getSt()
                        .toBuilder()
                        .setProposed(System.currentTimeMillis()))
                .build();
        for (peer p : peers.values()) {
            sendDataMessage(p.stub, msg);
        }
    }
    private void addToPendings(Block request) {
        int sender = request.getHeader().getM().getSender();
        long start = System.currentTimeMillis();
        if (!blockDigSig.verify(sender, request)) return;
        request = request.toBuilder()
                .setSt(request.getSt().toBuilder().setVerified(System.currentTimeMillis() - start)
                .setProposed(System.currentTimeMillis()))
                .build();
        int cid = request.getHeader().getM().getCid();
        int channel = request.getHeader().getM().getChannel();
        int cidSeries = request.getHeader().getM().getCidSeries();
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (GlobalData.received[channel].containsKey(key)) return;
        synchronized (msgNotifyer[channel]) {
            GlobalData.pending[channel].putIfAbsent(key, request);
            msgNotifyer[channel].notify();
        }
        logger.debug(format("[#%d-C[%d]] has received data message from [%d], [cidSeries=%d ; cid=%d]"
                , id, channel, sender, cidSeries, cid));
    }

    @Override
    public void disseminateMessage(Block request, StreamObserver<Empty> responseObserver) {
            addToPendings(request);
    }

    @Override
    public void fastVote(BbcMsg request, StreamObserver<Empty> responeObserver) {
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
        fvData[channel].putIfAbsent(key, new VoteData());
//        fastVoteV1(request, key, channel, cidSeries, cid);
        fastVoteV2(request, key, channel, cidSeries, cid);


    }

    void fastVoteV1(BbcMsg request, Meta key, int channel, int cidSeries, int cid) {
        fvData[channel].computeIfPresent(key, (k, v) ->{
            if (v.getVotersNum() == n) return v;
            if (!v.addVote(request.getM().getSender(), request.getVote())) return v;
            if (v.getVotersNum() == n) {
                synchronized (fastVoteNotifyer[channel]) {
                    GlobalData.votes[channel].computeIfAbsent(k, k1 -> {
                        GlobalData.bbcDec[channel].computeIfAbsent(k1, k2 -> {
                            fastVoteNotifyer[channel].notifyAll();
                            return new BbcDecData(v.getVoteReasult(), true);
                        });
                        return null;
                    });
                    GlobalData.votes[channel].computeIfPresent(k, (k1, v1) -> {
                        GlobalData.bbcDec[channel].computeIfAbsent(k1, k2 -> {
                            if (v.isPosRes(n)) {
                                logger.debug(format("[#%d-C[%d]] decides fast, but re-participate consensus " +
                                                "[channel=%d ; cidSeries=%d ; cid=%d]",
                                        id, channel, channel, cidSeries, cid));
                                bbcService.propose(1, channel, cidSeries, cid);
                                fastVoteNotifyer[channel].notifyAll();
                            }
                            return new BbcDecData(v.getVoteReasult(), true);
                        });


                        return v1;
                    });


                }
            }
            return v;
        });
    }

    void fastVoteV2(BbcMsg request, Meta key, int channel, int cidSeries, int cid) {
        fvData[channel].computeIfPresent(key, (k, v) -> {
            if (v.getVotersNum() == n - f) return v;
            if (!v.addVote(request.getM().getSender(), request.getVote())) return v;
            logger.debug(format("[#%d-C[%d]] received fv message [cidSeries=%d ; cid=%d ; sender=%d, v=%b]",
                    id, channel, cidSeries, cid, request.getM().getSender(), request.getVote()));
            if (v.getVotersNum() == n - f) {
                synchronized (fastVoteNotifyer[channel]) {
                    GlobalData.votes[channel].computeIfAbsent(k, k1 -> {
                        GlobalData.bbcDec[channel].computeIfAbsent(k1, k2 -> {
                            fastVoteNotifyer[channel].notifyAll();
                            return new BbcDecData(v.getVoteReasult(), true);
                        });
                        return null;
                    });

                    GlobalData.votes[channel].computeIfPresent(k, (k1, v1) -> {
                        GlobalData.bbcDec[channel].computeIfAbsent(k1, k2 -> {
                            if (v.isPosRes(n - f)) {
                                logger.debug(format("[#%d-C[%d]] decides fast, but re-participate consensus [channel=%d ; cidSeries=%d ; cid=%d]",
                                        id, channel, channel, cidSeries, cid));
                                bbcService.propose(1, channel, cidSeries, cid);
                            }
                            fastVoteNotifyer[channel].notifyAll();
                            return new BbcDecData(v.getVoteReasult(), true);
                        });
                        return v1;
                    });

                }
            }
            return v;
        });
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
        msg = GlobalData.received[channel].get(key);
        if (msg == null) {
            msg = GlobalData.pending[channel].get(key);
        }
        if (msg != null) {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d]",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));
            Meta meta = Meta.newBuilder().
                    setSender(id).
                    setChannel(channel).
                    setCid(cid).
                    setCidSeries(cidSeries).
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

    @Override
    public void preConsReqMessage(PreConsReq request,  StreamObserver<Res> responseObserver)  {
        Block msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        Meta key =  Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        msg = GlobalData.received[channel].get(key);
        if (msg == null) {
            msg = GlobalData.pending[channel].get(key);
        }
        if (msg == null) {
            msg = Block.getDefaultInstance();
            logger.debug(format("[#%d-C[%d]] has received pre das request message" +
                            " from [#%d] of [cidSeries=%d ; cid=%d] response with NULL",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));

        } else {
            logger.debug(format("[#%d-C[%d]] has received pre das request message from [#%d] of [cidSeries=%d ; cid=%d]" +
                            " responses with a value",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));
        }

        Meta meta = Meta.newBuilder().
                setSender(id).
        setChannel(channel).
                        setCid(cid).
                        setCidSeries(cidSeries).
                        build();
        responseObserver.onNext(Res.newBuilder().
                setData(msg).
                setM(meta).
                build());
    }

    private void fastBbcVoteV1(Meta key, int channel, int sender, int cidSeries, int cid, Block next) {
        GlobalData.pending[channel].computeIfPresent(key, (k, val) -> {
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
                    .setVote(true);
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

    }

    private void fastBbcVoteV2(Meta key, int channel, int sender, int cidSeries, int cid, Block next) {
        BbcMsg.Builder bv = BbcMsg
                .newBuilder()
                .setM(Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .setSender(id)
                        .build())
                .setVote(false);
        GlobalData.pending[channel].computeIfPresent(key, (k, val) -> {
            if (val.getHeader().getM().getSender() != sender ||
                    val.getHeader().getM().getCidSeries() != cidSeries ||
                    val.getHeader().getM().getCid() != cid ||
                    val.getHeader().getM().getChannel() != channel) return val;
                bv.setVote(true);
            if (next != null) {
                logger.debug(format("[#%d-C[%d]] broadcasts [cidSeries=%d ; cid=%d] via fast mode",
                        id, channel, next.getHeader().getM().getCidSeries(), next.getHeader().getM().getCid()));
                bv.setNext(setFastModeData(val, next));
            }
                       return val;
        });
        broadcastFastVoteMessage(bv.build());
        logger.debug(format("[#%d-C[%d]] sending fast vote [cidSeries=%d ; cid=%d ; val=%b]",
                id, channel, cidSeries, cid, bv.getVote()));
    }

    private void wait4FastVoteV1(Meta key, int channel, long estimatedTime) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        synchronized (fastVoteNotifyer[channel]) {
            while (currentTmo[channel] - estimatedTime > 0 &&
                    !GlobalData.bbcDec[channel].containsKey(key)) {
                logger.debug(format("[#%d-C[%d]] will wait at most [%d] ms for fast bbc", id, channel,
                        max(currentTmo[channel] - estimatedTime, 1)));
                fastVoteNotifyer[channel].wait(currentTmo[channel] - estimatedTime);
            }
            logger.debug(format("[#%d-C[%d]] have waited for more [%d] ms for fast bbc", id, channel,
                    System.currentTimeMillis() - startTime));
        }
    }

    private void wait4FastVoteV2(Meta key, int channel) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        synchronized (fastVoteNotifyer[channel]) {
            // Now we assume that a process always gets n-f messages!
            while (!GlobalData.bbcDec[channel].containsKey(key)) {
                fastVoteNotifyer[channel].wait();
            }
            logger.debug(format("[#%d-C[%d]] have waited for more [%d] ms for fast bbc", id, channel,
                    System.currentTimeMillis() - startTime));
        }
    }

    public Block deliver(int channel, int cidSeries, int cid, int sender, int height, Block next)
            throws InterruptedException {
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();

        preDeliverLogic(key, channel, cidSeries, cid);
//        long start = System.currentTimeMillis();
//        long est = System.currentTimeMillis() - start;
//        if (!deliverV1(key, channel, cidSeries, cid, sender, height, next, est)) {
//            return null;
//        }

        if (!deliverV2(key, channel, cidSeries, cid, sender, height, next)) {
            logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, 0, cidSeries, cid));
            return null;
        }
        logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, 1, cidSeries, cid));
        if (GlobalData.bbcDec[channel].get(key).fv) {
            optimialDec.getAndIncrement();
        }

        return postDeliverLogic(key, channel, cidSeries, cid, sender, height);
    }

    private boolean deliverV1(Meta key, int channel, int cidSeries, int cid, int sender, int height, Block next, long estimatedTime)
            throws InterruptedException
    {
        totalDeliveredTries.getAndIncrement();


        fastBbcVoteV1(key, channel, sender, cidSeries, cid, next);

        if (GlobalData.bbcDec[channel].containsKey(key)
                && GlobalData.bbcDec[channel].get(key).getDec() != -1) {
            int dec = GlobalData.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] already has a decision!" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }

        wait4FastVoteV1(key, channel, estimatedTime);



        if (GlobalData.bbcDec[channel].containsKey(key)
                && GlobalData.bbcDec[channel].get(key).getDec() != -1) {
            int dec = GlobalData.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] deliver probably by fast vote" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }
        AtomicBoolean prop = new AtomicBoolean(false);

        GlobalData.pending[channel].computeIfAbsent(key, k -> {
            currentTmo[channel] += tmoInterval;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 0));
            bbcService.propose(0, channel, cidSeries, cid);
            prop.set(true);
            return null;
        });

        GlobalData.pending[channel].computeIfPresent(key, (k, v) -> {
            if (prop.get()) return v;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 1));
            bbcService.propose(1, channel, cidSeries, cid);
            return v;
        });

        return bbcService.decide(channel, cidSeries, cid);
    }

    private void preConsReq(Meta key, int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        if (GlobalData.pending[channel].containsKey(key)) return;
        logger.debug(format("[#%d-C[%d]] starts preConsReq", id, channel));
        PreConsReq req = PreConsReq.newBuilder()
                .setMeta(Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .setSender(id)
                        .build())
                .build();
        broadcastPreConsReqMsg(req, channel, cidSeries, cid, sender, height);
        synchronized (preConsNotifyer[channel]) {
            while (!preConsDone[channel].contains(key)) {
                preConsNotifyer[channel].wait();
            }
        }

        logger.debug(format("[#%d-C[%d]] finishes preConsReq", id, channel));
    }

    private boolean deliverV2(Meta key, int channel, int cidSeries, int cid, int sender, int height, Block next)
            throws InterruptedException
    {
        totalDeliveredTries.getAndIncrement();

        fastBbcVoteV2(key, channel, sender, cidSeries, cid, next);

        if (GlobalData.bbcDec[channel].containsKey(key)
                && GlobalData.bbcDec[channel].get(key).getDec() != -1) {

            int dec = GlobalData.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] already has a decision!" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }

        wait4FastVoteV2(key, channel);



        if (GlobalData.bbcDec[channel].containsKey(key)
                && GlobalData.bbcDec[channel].get(key).getDec() != -1) {
            int dec = GlobalData.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] deliver probably by fast vote" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }

        preConsReq(key, channel, cidSeries, cid, sender, height);

        AtomicBoolean prop = new AtomicBoolean(false);

        GlobalData.pending[channel].computeIfAbsent(key, k -> {
            currentTmo[channel] += tmoInterval;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 0));
            bbcService.propose(0, channel, cidSeries, cid);
            prop.set(true);
            return null;
        });

        GlobalData.pending[channel].computeIfPresent(key, (k, v) -> {
            if (prop.get()) return v;
            currentTmo[channel] = tmo;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 1));
            bbcService.propose(1, channel, cidSeries, cid);
            return v;
        });

        return bbcService.decide(channel, cidSeries, cid);
    }

    private void preDeliverLogic(Meta key, int channel, int cidSeries, int cid) throws InterruptedException {
        int realTmo = currentTmo[channel];
        long startTime = System.currentTimeMillis();
        synchronized (msgNotifyer[channel]) {
            while (realTmo > 0 && !GlobalData.pending[channel].containsKey(key)) {
                logger.debug(format("[#%d-C[%d]] going to sleep for at most [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                        id, channel, realTmo, cidSeries, cid));
                msgNotifyer[channel].wait(realTmo);

                realTmo -= (System.currentTimeMillis() - startTime);
                logger.debug(format("[#%d-C[%d]] real TMO is [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                        id, channel, realTmo, cidSeries, cid));
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.debug(format("[#%d-C[%d]] have waited [%d] ms for data msg [cidSeries=%d ; cid=%d]",
                id, channel, estimatedTime, cidSeries, cid));
    }
    private Block postDeliverLogic(Meta key, int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        requestData(channel, cidSeries, cid, sender);
        Block msg = GlobalData.pending[channel].get(key);
        GlobalData.pending[channel].remove(key);
        GlobalData.received[channel].put(key, msg);
        return msg;
    }

    private void requestData(int channel, int cidSeries, int cid, int sender) throws InterruptedException {
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (GlobalData.pending[channel].containsKey(key)) return;
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setChannel(channel)
                .setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req,channel, cidSeries, cid, sender);
        synchronized (msgNotifyer[channel]) {
            while (!GlobalData.pending[channel].containsKey(key)) {
                msgNotifyer[channel].wait();
            }
        }

    }


//    public void clearBuffers(Meta key) {
//        int channel = key.getChannel();
//
//        recMsg[channel].remove(key);
//        fVotes[channel].remove(key);
//        pendingMsg[channel].remove(key);
//        fastBbcCons[channel].remove(key);
//        regBbcCons[channel].remove(key);
//        preConsVote[channel].remove(key);
//        bbcService.clearBuffers(key);
//    }

    private Block setFastModeData(Block curr, Block next) {
        Block.Builder nextBuilder = next
                    .toBuilder()
                    .setHeader(next.getHeader().toBuilder()
                    .setPrev(ByteString
                            .copyFrom(DigestMethod
                                    .hash(curr.getHeader().toByteArray())))
                            .build());
        String signature = blockDigSig.sign(next.getHeader());
        return nextBuilder.setHeader(nextBuilder.getHeader().toBuilder()
                .setProof(signature))
                .build();
    }
    
}
