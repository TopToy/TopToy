package das.wrb;

import blockchain.Blockchain;
import com.google.protobuf.ByteString;
import communication.CommLayer;
import config.Config;
import config.Node;
import das.bbc.BbcService;
import crypto.DigestMethod;
import crypto.blockDigSig;
import crypto.sslUtils;
import das.data.BbcDecData;
import das.data.Data;
import das.data.VoteData;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;

import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import proto.Types.*;
import proto.WrbGrpc;

import static das.data.Data.*;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;
import static java.lang.String.format;

public class WrbService extends WrbGrpc.WrbImplBase {
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
        WrbGrpc.WrbStub stub;

        peer(Node node) {
            try {
                channel = sslUtils.buildSslChannel(node.getAddr(), node.getPort(),
                        sslUtils.buildSslContextForClient(Config.getCaRootPath(),
                                Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath())).
                        intercept(new clientTlsIntercepter()).build();
            } catch (SSLException e) {
                logger.fatal(format("[#%d]", id), e);
            }
            stub = WrbGrpc.newStub(channel);
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
//    private final ConcurrentHashMap<Meta, List<Integer>>[] preConsVote;
//    private final ConcurrentHashMap<Meta, VoteData>[] fvData;
//    private final ArrayList<Meta>[] preConsDone;
//    private final ConcurrentHashMap<Meta, Block>[] pendingMsg;
//    private final ConcurrentHashMap<Meta, BbcDecision>[] fastBbcCons;
//    private final ConcurrentHashMap<Meta, BbcDecision>[] regBbcCons;
    private Object[] fastVoteNotifyer;
    private Object[] msgNotifyer;
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
    CommLayer comm;
    Blockchain[] bcs;


    public WrbService(int channels, int id, int f, int tmo, int tmoInterval, ArrayList<Node> nodes,
                      String bbcConfig, String serverCrt, String serverPrivKey, String caRoot, CommLayer comm) {
        this.channels = channels;
        this.tmo = tmo;
        this.tmoInterval = tmoInterval;
        this.currentTmo = new int[channels];
        this.bbcConfig = bbcConfig;
//        this.fvData = new ConcurrentHashMap[channels];
//        this.preConsVote = new ConcurrentHashMap[channels];
//        this.preConsDone = new ArrayList[channels];
        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.id = id;
        this.nodes = nodes;
        this.fastVoteNotifyer = new Object[channels];
        this.msgNotifyer = new Object[channels];
//        this.preConsNotifyer = new Object[channels];
        this.comm = comm;
        this.bcs = new Blockchain[channels];
        for (int i = 0 ; i < channels ; i++) {
            fastVoteNotifyer[i] = new Object();
            msgNotifyer[i] = new Object();
//            preConsNotifyer[i] = new Object();
//            fvData[i] = new ConcurrentHashMap<>();
//            preConsVote[i] = new ConcurrentHashMap<>();
//            preConsDone[i] = new ArrayList<>();
            this.currentTmo[i] = tmo;
        }
        new Data(channels);
        startGrpcServer(serverCrt, serverPrivKey, caRoot);
    }

    public void setBcForChannel(int channel, Blockchain bc) {
        this.bcs[channel] = bc;
    }


    private void startGrpcServer(String serverCrt, String serverPrivKey, String caRoot) {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            logger.debug(format("[#%d] There are %d CPU's in the system", id, cores));
            Executor executor = Executors.newFixedThreadPool(channels * n);
            EventLoopGroup weg = new NioEventLoopGroup(cores);
            rmfServer = NettyServerBuilder.
                    forPort(nodes.get(id).getPort()).
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

    void sendDataMessage(WrbGrpc.WrbStub stub, BlockHeader msg) {
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

    private void sendFastVoteMessage(WrbGrpc.WrbStub stub, BbcMsg v) {
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
    private void sendReqMessage(WrbGrpc.WrbStub stub, WrbReq req, int channel,
                                int cidSeries, int cid, int sender) {
        stub.reqMessage(req, new StreamObserver<>() {
            @Override
            public void onNext(WrbRes res) {
                Meta key = Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .build();
                if (res.getData().getM().getCid() == cid &&
                        res.getData().getM().getCidSeries() == cidSeries
                        && res.getM().getChannel() == channel &&
                        res.getData().getM().getChannel() == channel) {
                    if (Data.pending[channel].containsKey(key) || bcs[channel].contains(req.getHeight())) return;
                    if (!blockDigSig.verifyHeader(sender, res.getData())) {
                        logger.debug(format("[#%d-C[%d]] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                id, channel, res.getM().getSender(), cidSeries, cid));
                        return;
                    }
                    logger.debug(format("[#%d-C[%d]] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                            id, channel, res.getM().getSender(), cidSeries, cid));
                    synchronized (msgNotifyer[channel]) {
                        Data.pending[channel].putIfAbsent(key, res.getData());
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

    private void sendPreConsReqMessage(WrbGrpc.WrbStub stub, WrbPreConsReq req, int channel, int cidSeries, int cid, int sender) {
        stub.preConsReqMessage(req, new StreamObserver<WrbRes>() {
            @Override
            public void onNext(WrbRes res) {
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

                    if ((!res.getData().equals(BlockHeader.getDefaultInstance()))
                            && res.getData().getM().getCid() == cid
                            && res.getData().getM().getCidSeries() == cidSeries
                            && res.getData().getM().getChannel() == channel
                            && res.getData().getM().getSender() == sender) {
                        if (!blockDigSig.verifyHeader(sender, res.getData())) {
                            logger.debug(format("[#%d-C[%d]] has pre cons received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, channel, res.getM().getSender(), cidSeries, cid));
                            return val;
                        }
                        if (!Data.pending[channel].containsKey(key) &&
                                !bcs[channel].contains(req.getHeight())) {
                            logger.debug(format("[#%d-C[%d]] has pre das received message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, channel, res.getM().getSender(), cidSeries, cid));
                            Data.pending[channel].putIfAbsent(key, res.getData());
                        }
                    } else if (res.getData().equals(BlockHeader.getDefaultInstance())) {
                        logger.debug(format("[#%d-C[%d]] has pre das received NULL message from [#%d] for [cidSeries=%d ; cid=%d]",
                                id, channel, res.getM().getSender(), cidSeries,  cid));
                    }

                    if (val.size() == n - f) {
                        synchronized (preConsDone[channel]) {
                            preConsDone[channel].add(key);
                            logger.debug(format("[#%d-C[%d]] notify on preCons for [cidSeries=%d ; cid=%d]",
                                    id, channel, cidSeries,  cid));
                            preConsDone[channel].notify();
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

    private void broadcastReqMsg(WrbReq req, int channel, int cidSeries, int cid, int sender) {
        logger.debug(format("[#%d-C[%d]] broadcasts request message [cidSeries=%d ; cid=%d; height=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid, req.getHeight()));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender);
        }
    }

    private void broadcastPreConsReqMsg(WrbPreConsReq req, int channel, int cidSeries, int cid, int sender) {
        logger.debug(format("[#%d-C[%d]] broadcasts pre cons request message [cidSeries=%d ; cid=%d ; height=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid, req.getHeight()));
        for (int p : peers.keySet()) {
//            if (p == id) continue;
            sendPreConsReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender);
        }
    }

    private void broadcastFastVoteMessage(BbcMsg v) {
        for (int p : peers.keySet()) {
            sendFastVoteMessage(peers.get(p).stub, v);
        }
    }

    void wrbSend(BlockHeader msg, int[] recipients) {
        for (int i : recipients) {
            sendDataMessage(peers.get(i).stub, msg);
        }

    }
    void wrbBroadcast(BlockHeader msg) {
        for (peer p : peers.values()) {
            sendDataMessage(p.stub, msg);
        }
    }
    private void addToPendings(BlockHeader request) {
        int sender = request.getM().getSender();
        // I think we don't need to verify the block (if we use reliable channels)
        if (!blockDigSig.verifyHeader(sender, request)) return;
        int cid = request.getM().getCid();
        int channel = request.getM().getChannel();
        int cidSeries = request.getM().getCidSeries();
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (bcs[channel].contains(request.getHeight())) return;
        synchronized (msgNotifyer[channel]) {
            Data.pending[channel].putIfAbsent(key, request);
            msgNotifyer[channel].notify();
        }
        logger.debug(format("[#%d-C[%d]] has received data message from [%d], [cidSeries=%d ; cid=%d]"
                , id, channel, sender, cidSeries, cid));
    }

    @Override
    public void disseminateMessage(BlockHeader request, StreamObserver<Empty> responseObserver) {
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
        fastVoteV2(request, key, channel, cidSeries, cid);


    }

    void fastVoteV1(BbcMsg request, Meta key, int channel, int cidSeries, int cid) {
        fvData[channel].computeIfPresent(key, (k, v) ->{
            if (v.getVotersNum() == n) return v;
            if (!v.addVote(request.getM().getSender(), request.getVote())) return v;
            if (v.getVotersNum() == n) {
                synchronized (fastVoteNotifyer[channel]) {
                    Data.votes[channel].computeIfAbsent(k, k1 -> {
                        Data.bbcDec[channel].computeIfAbsent(k1, k2 -> {
                            fastVoteNotifyer[channel].notifyAll();
                            return new BbcDecData(v.getVoteReasult(), true);
                        });
                        return null;
                    });
                    Data.votes[channel].computeIfPresent(k, (k1, v1) -> {
                        Data.bbcDec[channel].computeIfAbsent(k1, k2 -> {
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
                    Data.votes[channel].computeIfAbsent(k, k1 -> {
                        Data.bbcDec[channel].computeIfAbsent(k1, k2 -> {
                            fastVoteNotifyer[channel].notifyAll();
                            return new BbcDecData(v.getVoteReasult(), true);
                        });
                        return null;
                    });

                    Data.votes[channel].computeIfPresent(k, (k1, v1) -> {
                        Data.bbcDec[channel].computeIfAbsent(k1, k2 -> {
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
    public void reqMessage(WrbReq request,  StreamObserver<WrbRes> responseObserver)  {
        BlockHeader msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        Meta key =  Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        msg = Data.pending[channel].get(key);
        if (msg == null && bcs[channel].contains(request.getHeight())) {
            msg = bcs[channel].getBlock(request.getHeight()).getHeader();
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
            responseObserver.onNext(WrbRes.newBuilder().
                    setData(msg).
                    setM(meta).
                    build());
        } else {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));
        }
    }

    @Override
    public void preConsReqMessage(WrbPreConsReq request,  StreamObserver<WrbRes> responseObserver)  {
        BlockHeader msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        Meta key =  Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        msg = Data.pending[channel].get(key);
        if (msg == null && bcs[channel].contains(request.getHeight())) {
            msg = bcs[channel].getBlock(request.getHeight()).getHeader();
        }
        if (msg == null) {
            msg = BlockHeader.getDefaultInstance();
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
        responseObserver.onNext(WrbRes.newBuilder().
                setData(msg).
                setM(meta).
                build());
    }

    private void fastBbcVoteV1(Meta key, int channel, int sender, int cidSeries, int cid, BlockHeader next) {
        Data.pending[channel].computeIfPresent(key, (k, val) -> {
            long start = System.currentTimeMillis();
            if (val.getM().getSender() != sender ||
                    val.getM().getCidSeries() != cidSeries ||
                    val.getM().getCid() != cid ||
                    val.getM().getChannel() != channel) return val;
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
                        id, channel, next.getM().getCidSeries(), next.getM().getCid()));
                long st = System.currentTimeMillis();
                bv.setNext(setFastModeData(val, next));
                logger.debug(format("[#%d-C[%d]] creating new block of [cidSeries=%d ; cid=%d] took about [%d] ms",
                        id, channel, next.getM().getCidSeries(), next.getM().getCid(),
                        System.currentTimeMillis() - st));
            }
            broadcastFastVoteMessage(bv.build());
            logger.debug(format("[#%d-C[%d]] sending fast vote [cidSeries=%d ; cid=%d] took about[%d] ms",
                    id, channel, cidSeries, cid, System.currentTimeMillis() - start));
            return val;
        });

    }

    private void fastBbcVoteV2(Meta key, int channel, int sender, int cidSeries, int cid, BlockHeader next) {
        BbcMsg.Builder bv = BbcMsg
                .newBuilder()
                .setM(Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .setSender(id)
                        .build())
                .setVote(false);

        Data.pending[channel].computeIfPresent(key, (k, val) -> {
            if (val.getM().getSender() != sender ||
                    val.getM().getCidSeries() != cidSeries ||
                    val.getM().getCid() != cid ||
                    val.getM().getChannel() != channel) return val;
            BlockID bid = BlockID.newBuilder().setPid(val.getM().getSender()).setBid(val.getBid()).build();
            if (!comm.contains(channel, bid, val)) {
                logger.debug(format("[#%d-C[%d]] comm does not contains the block itself (or it is invalid) [cidSeries=%d ; cid=%d ; bid=%d ; empty=%b]",
                        id, channel, val.getM().getCidSeries(), val.getM().getCid(), bid.getBid(), val.getEmpty()));
                return val;
            }
                bv.setVote(true);
            if (next != null) {
                logger.debug(format("[#%d-C[%d]] broadcasts [cidSeries=%d ; cid=%d] via fast mode",
                        id, channel, next.getM().getCidSeries(), next.getM().getCid()));
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
                    !Data.bbcDec[channel].containsKey(key)) {
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
            while (!Data.bbcDec[channel].containsKey(key)) {
                fastVoteNotifyer[channel].wait();
            }
            logger.debug(format("[#%d-C[%d]] have waited for more [%d] ms for fast bbc", id, channel,
                    System.currentTimeMillis() - startTime));
        }
    }

    public BlockHeader deliver(int channel, int cidSeries, int cid, int sender, int height, BlockHeader next)
            throws InterruptedException {
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();

        preDeliverLogic(key, channel, cidSeries, cid);
        if (!deliverV2(key, channel, cidSeries, cid, sender, height, next)) {
            logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, 0, cidSeries, cid));
            return null;
        }
        logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, 1, cidSeries, cid));
        if (Data.bbcDec[channel].get(key).fv) {
            optimialDec.getAndIncrement();
        }

        return postDeliverLogic(key, channel, cidSeries, cid, sender, height);
    }

    private boolean deliverV1(Meta key, int channel, int cidSeries, int cid, int sender, int height, BlockHeader next, long estimatedTime)
            throws InterruptedException
    {
        totalDeliveredTries.getAndIncrement();


        fastBbcVoteV1(key, channel, sender, cidSeries, cid, next);

        if (Data.bbcDec[channel].containsKey(key)
                && Data.bbcDec[channel].get(key).getDec() != -1) {
            int dec = Data.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] already has a decision!" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }

        wait4FastVoteV1(key, channel, estimatedTime);



        if (Data.bbcDec[channel].containsKey(key)
                && Data.bbcDec[channel].get(key).getDec() != -1) {
            int dec = Data.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] deliver probably by fast vote" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }
        AtomicBoolean prop = new AtomicBoolean(false);

        Data.pending[channel].computeIfAbsent(key, k -> {
            currentTmo[channel] += tmoInterval;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 0));
            bbcService.propose(0, channel, cidSeries, cid);
            prop.set(true);
            return null;
        });

        Data.pending[channel].computeIfPresent(key, (k, v) -> {
            if (prop.get()) return v;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 1));
            bbcService.propose(1, channel, cidSeries, cid);
            return v;
        });

        return bbcService.decide(channel, cidSeries, cid);
    }

    private void preConsReq(Meta key, int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        if (Data.pending[channel].containsKey(key)) return;
        logger.debug(format("[#%d-C[%d]] starts preConsReq", id, channel));
        WrbPreConsReq req = WrbPreConsReq.newBuilder()
                .setMeta(Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .setSender(id)
                        .build())
                .setHeight(height)
                .build();
        broadcastPreConsReqMsg(req, channel, cidSeries, cid, sender);
        synchronized (preConsDone[channel]) {
            while (!preConsDone[channel].contains(key)) {
                preConsDone[channel].wait();
            }
        }

        logger.debug(format("[#%d-C[%d]] finishes preConsReq", id, channel));
    }

    private boolean deliverV2(Meta key, int channel, int cidSeries, int cid, int sender, int height, BlockHeader next)
            throws InterruptedException
    {
        totalDeliveredTries.getAndIncrement();

        fastBbcVoteV2(key, channel, sender, cidSeries, cid, next);

        if (Data.bbcDec[channel].containsKey(key)
                && Data.bbcDec[channel].get(key).getDec() != -1) {

            int dec = Data.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] already has a decision!" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }

        wait4FastVoteV2(key, channel);



        if (Data.bbcDec[channel].containsKey(key)
                && Data.bbcDec[channel].get(key).getDec() != -1) {
            int dec = Data.bbcDec[channel].get(key).getDec();
            logger.debug(format("[#%d-C[%d]] deliver probably by fast vote" +
                    " bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
            if (dec == 1) currentTmo[channel] = tmo;
            return dec == 1;
        }

        preConsReq(key, channel, cidSeries, cid, sender, height);

        AtomicBoolean prop = new AtomicBoolean(false);

        Data.pending[channel].computeIfAbsent(key, k -> {
            currentTmo[channel] += tmoInterval;
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, 0));
            bbcService.propose(0, channel, cidSeries, cid);
            prop.set(true);
            return null;
        });


        Data.pending[channel].computeIfPresent(key, (k, v) -> {
            if (prop.get()) return v;
            currentTmo[channel] = tmo;
            int vote = 1;
            BlockID bid = BlockID.newBuilder().setPid(v.getM().getSender()).setBid(v.getBid()).build();
            if (!comm.contains(channel, bid, v)) {
                logger.debug(format("[#%d-C[%d]] Has a header but not the block itself [cidSeries=%d ; cid=%d, pid=%d, bid=%d]",
                        id, channel, cidSeries, cid, bid.getPid(), bid.getBid()));
                vote = 0;
            }
            logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, vote));
            bbcService.propose(vote, channel, cidSeries, cid);
            return v;
        });

        return bbcService.decide(channel, cidSeries, cid);
    }

    private void preDeliverLogic(Meta key, int channel, int cidSeries, int cid) throws InterruptedException {
        int realTmo = currentTmo[channel];
        long startTime = System.currentTimeMillis();
        synchronized (msgNotifyer[channel]) {
            while (realTmo > 0 && !Data.pending[channel].containsKey(key)) {
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

    private BlockHeader postDeliverLogic(Meta key, int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        requestData(channel, cidSeries, cid, sender, height);
        BlockHeader msg = Data.pending[channel].get(key);
//        GlobalData.pending[channel].remove(key);
//        GlobalData.received[channel].put(key, msg);
        return msg;
    }

    private void requestData(int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        Meta key = Meta.newBuilder()
                .setChannel(channel)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (Data.pending[channel].containsKey(key)) return;
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setChannel(channel)
                .setSender(id).
                build();
        WrbReq req = WrbReq.newBuilder()
                .setMeta(meta)
                .setHeight(height)
                .build();
        broadcastReqMsg(req,channel, cidSeries, cid, sender);
        synchronized (msgNotifyer[channel]) {
            while (!Data.pending[channel].containsKey(key)) {
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

    private BlockHeader setFastModeData(BlockHeader curr, BlockHeader next) {
        BlockHeader.Builder nextBuilder = next
                    .toBuilder()
                    .setPrev(ByteString
                            .copyFrom(DigestMethod
                                    .hash(curr.toByteArray())));
        String signature = blockDigSig.sign(next);
        return nextBuilder
                .setProof(signature)
                .build();
    }
    
}
