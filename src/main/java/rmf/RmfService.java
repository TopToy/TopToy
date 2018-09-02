package rmf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import config.Config;
import config.Node;
import consensus.bbc.bbcService;
import crypto.DigestMethod;
import crypto.rmfDigSig;
import crypto.sslUtils;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;

import io.grpc.stub.StreamObserver;
import proto.Types.*;
import proto.*;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
            try {
                channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.fatal(format("[#%d]", id), e);
            }
        }
    }
//    int channel;
    protected int id;
    private int n;
    protected int f;
    private bbcService bbcService;
    private final Object[] globalLock;

    private final Table<Integer, Integer, Data>[] recMsg;

    private final Table<Integer, Integer, fvote>[] fVotes;
    private final Table<Integer, Integer, Data>[] pendingMsg;
    private final Table<Integer, Integer, BbcDecision>[] fastBbcCons;

    Map<Integer, peer> peers;
    private List<Node> nodes;
    private Thread bbcServiceThread;
    private Thread[] bbcMissedConsensusThreads;
//    protected Server server;
    private String bbcConfig;
    private final Table<Integer, Integer, BbcDecision>[] regBbcCons;
    private boolean stopped = false;
    private Server rmfServer;
    int channels;

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

    public RmfService(int channels, int id, int f, ArrayList<Node> nodes, String bbcConfig,
                      String serverCrt, String serverPrivKey, String caRoot) {
        this.channels = channels;
        this.globalLock = new Object[channels];
        this.bbcConfig = bbcConfig;
        this.fVotes = new HashBasedTable[channels];
        this.pendingMsg =  new HashBasedTable[channels];
        this.recMsg =  new HashBasedTable[channels];

        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.id = id;
        this.nodes = nodes;
        this.fastBbcCons = new HashBasedTable[channels];
        this.regBbcCons = new HashBasedTable[channels];
        this.bbcMissedConsensusThreads = new Thread[channels];
        for (int i = 0 ; i < channels ; i++) {
            globalLock[i] = new Object();
            recMsg[i] = HashBasedTable.create();
            pendingMsg[i] = HashBasedTable.create();
            fVotes[i] = HashBasedTable.create();
            fastBbcCons[i] = HashBasedTable.create();
            regBbcCons[i] = HashBasedTable.create();
        }
        startGrpcServer(serverCrt, serverPrivKey, caRoot);
    }


    private void startGrpcServer(String serverCrt, String serverPrivKey, String caRoot) {
        try {
//            rmfServer = ServerBuilder.forPort(nodes.get(id).getRmfPort())
//                    .addService(this)
//                    .build()
//                    .start();
            rmfServer = NettyServerBuilder.
                    forPort(nodes.get(id).getRmfPort()).
                    sslContext(sslUtils.buildSslContextForServer(serverCrt,
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
            this.bbcService = new bbcService(id, 2*f + 1, bbcConfig);
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

        for (int i = 0 ; i < channels ; i++) {
            int finalI = i;
            bbcMissedConsensusThreads[i] = new Thread(() ->
            {
                try {
                    bbcMissedConsensus(finalI);
                } catch (InterruptedException e) {
                    logger.error(format("[#%d-C[%d]]", id, finalI), e);
                }
            });
            bbcMissedConsensusThreads[i].start();
        }

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
            if (stopped) return;
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.warn(format("[#%d] *** shutting down rmf service since JVM is shutting down", id));
            shutdown();
        }));
    }

    private void bbcMissedConsensus(int channel) throws InterruptedException {
        while (!stopped) {
            Thread.sleep(200);
            synchronized (fastBbcCons) {
                if (fastBbcCons[channel].rowKeySet().isEmpty()) {
//                    logger.debug(format("[#%d] There are no fast bbc", id));
                    continue;
                }
                int fcidSeries = Collections.max(fastBbcCons[channel].rowKeySet());
                int fcid = Collections.max(fastBbcCons[channel].row(fcidSeries).keySet());
//                logger.debug(format("[#%d] Trying re-participate [cidSeries=%d ; cid=%d]", id, fcidSeries, fcid));
                bbcService.periodicallyVoteMissingConsensus(fastBbcCons[channel].get(fcidSeries, fcid));
            }

        }
    }

    public void shutdown() {
        stopped = true;
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
            if (bbcMissedConsensusThreads != null) {
                for (int i = 0 ; i < channels ; i++) {
                    if (bbcMissedConsensusThreads[i] != null) {
                        bbcMissedConsensusThreads[i].interrupt();
                        bbcMissedConsensusThreads[i].join();
                    }
                }
            }

        } catch (InterruptedException e) {
            logger.error("", e);
        }
        if (rmfServer != null) {
            rmfServer.shutdown();
        }
        logger.info(format("[#%d] shutting down rmf service", id));
    }

    void sendDataMessage(RmfGrpc.RmfStub stub, Data msg) {
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
                if (res.getData().getMeta().getCid() == cid &&
                        res.getData().getMeta().getCidSeries() == cidSeries
                        && res.getMeta().getChannel() == channel && res.getData().getMeta().getChannel() == channel) {
                    synchronized (globalLock[channel]) {
                        if (!pendingMsg[channel].contains(cidSeries, cid) && !recMsg[channel].contains(cidSeries, cid)) {
                            if (!rmfDigSig.verify(sender, res.getData())) {
                                logger.debug(format("[#%d-C[%d]] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                        id, channel, res.getMeta().getSender(), cidSeries, cid));
                                return;
                            }
                            logger.debug(format("[#%d-C[%d]] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, channel, res.getMeta().getSender(), cidSeries,  cid));
                            pendingMsg[channel].put(cidSeries, cid, res.getData());
                            globalLock[channel].notify();
                        }
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

    void rmfBroadcast(Data msg) {
        for (peer p : peers.values()) {
            sendDataMessage(p.stub, msg);
        }
    }
    private void addToPendings(Data request) {
        int sender = request.getMeta().getSender();
        int cid = request.getMeta().getCid();
        int channel = request.getMeta().getChannel();
        int cidSeries = request.getMeta().getCidSeries();
        if (pendingMsg[channel].contains(cidSeries, cid) || recMsg[channel].contains(cidSeries, cid)) return;
        pendingMsg[channel].put(cidSeries, cid, request);
        logger.debug(format("[#%d-C[%d]] has received data message from [%d], [cidSeries=%d ; cid=%d]", id, channel,
                sender, cidSeries, cid));
    }

    @Override
    public void disseminateMessage(Data request, StreamObserver<Empty> responseObserver) {

        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        synchronized (globalLock[channel]) {
            synchronized (fastBbcCons) {
                if (fastBbcCons[channel].contains(cidSeries, cid) || regBbcCons[channel].contains(cidSeries, cid)) return;
            }
            addToPendings(request);
            globalLock[channel].notify();
        }
    }

    @Override
    public void fastVote(BbcMsg request, StreamObserver<Empty> responeObserver) {
        int cid = request.getM().getCid();
        int cidSeries = request.getM().getCidSeries();
        int channel = request.getM().getChannel();
        synchronized (globalLock[channel]) {
            if (regBbcCons[channel].contains(cidSeries, cid)) return;
            synchronized (fastBbcCons) {
                if (fastBbcCons[channel].contains(cidSeries, cid)) return; // Against byzantine activity
            }
            if (!fVotes[channel].contains(cidSeries, cid)) {
                fvote v = new fvote();
                v.dec.setM(Meta.newBuilder()
                        .setChannel(channel)
                        .setCidSeries(cidSeries)
                        .setCid(cid));
                v.cid = cid;
                v.cidSeries = cidSeries;
                v.voters = new ArrayList<>();
                fVotes[channel].put(cidSeries, cid, v);
            }

            if (fVotes[channel].get(cidSeries, cid).voters.contains(request.getM().getSender())) {
                logger.debug(format("[#%d-C[%d]] has received duplicate vote from [%d] for [channel=%d ; cidSeries=%d ; cid=%d]",
                        id, channel, request.getM().getSender(), channel, cidSeries, cid));
                return;
            }
            fVotes[channel].get(cidSeries, cid).voters.add(request.getM().getSender());
            if (request.hasNext()) {
                addToPendings(request.getNext());
            }
            if (fVotes[channel].get(cidSeries, cid).voters.size() == n) {
                logger.debug(format("[#%d-C[%d]] fastVote has been detected [cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
                synchronized (fastBbcCons) {
                    fastBbcCons[channel].put(cidSeries, cid, fVotes[channel].get(cidSeries, cid).dec.setDecosion(1).build());
                }
                bbcService.updateFastVote(fVotes[channel].get(cidSeries, cid).dec.build());
                globalLock[channel].notify();
            }
        }
    }
    @Override
    public void reqMessage(Req request,  StreamObserver<Res> responseObserver)  {
        Data msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        synchronized (globalLock[channel]) {
                msg = recMsg[channel].get(cidSeries, cid);
            if (msg == null) {
                msg = pendingMsg[channel].get(cidSeries, cid);
            }

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
                    setMeta(meta).
                    build());
        } else {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, channel, request.getMeta().getSender(), cidSeries, cid));
        }
    }

    public Data deliver(int channel, int cidSeries, int cid, int tmo, int sender, int height, Data next) throws InterruptedException {
        int cVotes = 0;
        int v = 0;
        boolean verfied = false;
        boolean valid = false;
        synchronized (globalLock[channel]) {
            long startTime = System.currentTimeMillis();
            if (!pendingMsg[channel].contains(cidSeries, cid)) {
                globalLock[channel].wait(tmo);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            logger.debug(format("[#%d-C[%d]] have waited [%d] ms for data msg", id, channel, estimatedTime));
            if (pendingMsg[channel].contains(cidSeries, cid) &&
                    pendingMsg[channel].get(cidSeries, cid).getMeta().getSender() == sender) { // &&
                Data msg = pendingMsg[channel].get(cidSeries, cid);
                verfied = true;
                valid = rmfDigSig.verify(msg.getMeta().getSender(), msg);
                if (valid) {
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
                                id, channel, next.getMeta().getCidSeries(), next.getMeta().getCid()));
                        try {
                            bv.setNext(setFastModeData(pendingMsg[channel].get(cidSeries, cid), next));
                        } catch (InvalidProtocolBufferException e) {
                            logger.error(format("[#%d-C[%d]]", id, channel), e);
                        }
                    }
                    broadcastFastVoteMessage(bv.build());
                }
//                    pendingMsg.get(cidSeries, cid).getMeta().getHeight() == height) {
            }
            if (fVotes[channel].contains(cidSeries, cid)) {
                cVotes = fVotes[channel].get(cidSeries, cid).voters.size();
            }
            
            if (cVotes < n) {
                startTime = System.currentTimeMillis();
                globalLock[channel].wait(Math.max(tmo - estimatedTime, 1));
                logger.debug(format("[#%d-C[%d]] have waited for more [%d] ms for fast bbc", id, channel, System.currentTimeMillis() - startTime));
            }

//            if (cVotes < n) {
//                while (cVotes < n && estimatedTime < tmo) {
//                    if (fVotes.contains(cidSeries, cid)) {
//                        cVotes = fVotes.get(cidSeries, cid).voters.size();
//                    }
//                    estimatedTime = System.currentTimeMillis() - startTime;
//                }
//                logger.debug(format("#(2)# have waited for more [%d] ms for fast bbc", System.currentTimeMillis() - startTime));
//            }

//                startTime = System.currentTimeMillis();
//                globalLock.wait(Math.max(tmo - estimatedTime, 1));
//                logger.debug(format("#(2)# have waited for more [%d] ms for fast bbc", System.currentTimeMillis() - startTime));
//            }

            if (fVotes[channel].contains(cidSeries, cid)) {
                cVotes = fVotes[channel].get(cidSeries, cid).voters.size();
            }

            if (cVotes < n) {
                if (pendingMsg[channel].contains(cidSeries, cid) &&
                        pendingMsg[channel].get(cidSeries, cid).getMeta().getSender() == sender) { // &&
                    //   pendingMsg.get(cidSeries, cid).getMeta().getHeight() == height) {
                    if (verfied && valid) {
                        v = 1;
                    }
                    if (!verfied) {
                        Data msg = pendingMsg[channel].get(cidSeries, cid);
                        verfied = true;
                        valid = rmfDigSig.verify(msg.getMeta().getSender(), msg);
                        if (valid) {
                            v = 1;
                        }
                    }
                }
//                td.t = Ctype.FULL;
                logger.debug(format("[#%d-C[%d]] cvotes is [%d] for [cidSeries=%d ; cid=%d]", id, channel, cVotes,cidSeries, cid));
                int dec = fullBbcConsensus(channel, v, cidSeries, cid);
                logger.debug(format("[#%d-C[%d]] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, channel, dec, cidSeries, cid));
                if (dec == 0) {
                    pendingMsg[channel].remove(cidSeries, cid);
                    return null;
                }
            } else {
                logger.debug(format("[#%d-C[%d]] deliver by fast vote [cidSeries=%d ; cid=%d]", id, channel, cidSeries, cid));
            }
            requestData(channel, cidSeries, cid, sender, height);
            Data msg;
                msg = pendingMsg[channel].get(cidSeries, cid);
                recMsg[channel].put(cidSeries, cid, msg);
                pendingMsg[channel].remove(cidSeries, cid);
            return msg;
        }


    }
    private int fullBbcConsensus(int channel, int vote, int cidSeries, int cid) throws InterruptedException {
        logger.debug(format("[#%d-C[%d]] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, channel, cidSeries, cid, vote));
        bbcService.propose(vote, channel, cidSeries, cid);
        BbcDecision dec = bbcService.decide(channel, cidSeries, cid);
        regBbcCons[channel].put(cidSeries, cid, dec);
        return dec.getDecosion();
    }

    private void requestData(int channel, int cidSeries, int cid, int sender, int height) throws InterruptedException {
        if (pendingMsg[channel].contains(cidSeries, cid) &&
                pendingMsg[channel].get(cidSeries, cid).getMeta().getSender() == sender) return; //&&
//                pendingMsg.get(cidSeries, cid).getMeta().getHeight() == height) return;
        pendingMsg[channel].remove(cidSeries, cid);
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setChannel(channel)
                .setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req,channel,  cidSeries, cid, sender, height);
        while ((!pendingMsg[channel].contains(cidSeries, cid)) ||
                pendingMsg[channel].get(cidSeries, cid).getMeta().getSender() != sender) { // ||
//                pendingMsg.get(cidSeries, cid).getMeta().getHeight() != height) {
            pendingMsg[channel].remove(cidSeries, cid);
            globalLock[channel].wait();
        }
    }

    String getMessageSig(int channel, int cidSeries, int cid) {
        if (recMsg[channel].contains(cidSeries, cid)) {
            return recMsg[channel].get(cidSeries, cid).getSig();
        }
        return null;
    }

//    public Data nonBlockingDeliver(int cidSeries, int cid) {
//        if (recMsg.contains(cidSeries, cid)) {
//            return recMsg.get(cidSeries, cid);
//        }
//        return null;
//    }

    private Data setFastModeData(Data curr, Data next) throws InvalidProtocolBufferException {
        Block currBlock = Block.parseFrom(curr.getData());
        Block nextBlock = Block.parseFrom(next.getData());
        nextBlock = nextBlock
                .toBuilder()
                .setHeader(nextBlock
                        .getHeader()
                        .toBuilder()
                        .setPrev((ByteString.copyFrom(DigestMethod
                                .hash(currBlock
                                        .getHeader()
                                        .toBuilder()
                                        .build()
                                        .toByteArray()))))
                .build())
                .build();
        next = next.toBuilder().setData(ByteString.copyFrom(nextBlock.toByteArray())).build();
        return next.toBuilder().setSig(rmfDigSig.sign(next.toBuilder())).build();

    }
    
}
