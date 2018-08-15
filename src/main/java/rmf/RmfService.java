package rmf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import config.Config;
import config.Node;
import consensus.bbc.bbcService;
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

/*
TODO:
1. the recMsg can grow infinitely.
2. Do need to validate the sender identity?? (currently not)
3. When to reset the timer
4. prevBbcConsensus can grow infinitely (and some more, analyze it!!)
5. change maps to list
6. Handle the case in which byzantine node sends its message twice
 */
public class RmfService extends RmfGrpc.RmfImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfService.class);

    class authInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                     Metadata metadata,
                                                                     ServerCallHandler<ReqT, RespT> serverCallHandler) {
            ServerCall.Listener res = new ServerCall.Listener() {};
            try {
                String peerDomain = serverCall.getAttributes()
                        .get(Grpc.TRANSPORT_ATTR_SSL_SESSION).getPeerPrincipal().getName().split("=")[1];
                int peerId = Integer.parseInt(Objects.requireNonNull(metadata.get
                        (Metadata.Key.of("id", ASCII_STRING_MARSHALLER))));
                if (nodes.get(peerId).getAddr().equals(peerDomain)) {
                    res = serverCallHandler.startCall(serverCall, metadata);
                }
            } catch (SSLPeerUnverifiedException e) {
                logger.error("", e);
            } finally {
                return res;
            }

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
                logger.fatal("", e);
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
                logger.error("", e);
            }
        }
    }
    protected int id;
    private int n;
    protected int f;
    private bbcService bbcService;
    private final Object globalLock = new Object();

    private final Table<Integer, Integer, Data> recMsg;

    private final Table<Integer, Integer, fvote> fVotes;
    private final Table<Integer, Integer, Data> pendingMsg;
    private final Table<Integer, Integer, BbcDecision> fastBbcCons;

    Map<Integer, peer> peers;
    private List<Node> nodes;
    private Thread bbcServiceThread;
    private Thread bbcMissedConsensusThread;
    protected Server server;
    private String bbcConfig;
    private final Table<Integer, Integer, BbcDecision> regBbcCons;
    private boolean stopped = false;
    private Server rmfServer;

    public RmfService(int id, int f, ArrayList<Node> nodes, String bbcConfig) {
        this.bbcConfig = bbcConfig;
        this.fVotes = HashBasedTable.create();
        this.pendingMsg =  HashBasedTable.create();
        this.recMsg =  HashBasedTable.create();

        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.id = id;
        this.nodes = nodes;
        this.fastBbcCons =  HashBasedTable.create();
        this.regBbcCons = HashBasedTable.create();
        startGrpcServer();
    }


    private void startGrpcServer() {
        try {
            rmfServer = NettyServerBuilder.
                    forPort(nodes.get(id).getRmfPort()).
                    sslContext(sslUtils.buildSslContextForServer(Config.getServerCrtPath(),
                            Config.getCaRootPath(), Config.getServerTlsPrivKeyPath())).
                    addService(this).
                    intercept(new authInterceptor()).
                    build().
                    start();
        } catch (IOException e) {
            logger.fatal("", e);
        }

    }

    public void start() {
        this.bbcService = new bbcService(id, 2*f + 1, bbcConfig);
        CountDownLatch latch = new CountDownLatch(1);
        bbcServiceThread = new Thread(() -> {
                /*
                    Note that in case that there is more than one bbc server this call blocks until all servers are up.
                 */
            this.bbcService.start();
            logger.info(format("[#%d] bbc server is up", id));
            latch.countDown();
        }
        );
        bbcServiceThread.start();
        bbcMissedConsensusThread = new Thread(() ->
        {
            try {
                bbcMissedConsensus();
            } catch (InterruptedException e) {
                logger.error(format("[#%d]", id), e);
            }
        });
        bbcMissedConsensusThread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
        logger.info(format("[#%d] Initiated grpc client", id));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (stopped) return;
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.warn("*** shutting down rmf service since JVM is shutting down");
            shutdown();
            logger.warn("*** service shut down");
        }));
    }
//    // TODO: Bug alert!!
    private void bbcMissedConsensus() throws InterruptedException {
        while (!stopped) {
            Thread.sleep(200); // TODO: Hard code timeout, should it changed? (we probably can do it by notifications)
            synchronized (fastBbcCons) {
                if (fastBbcCons.rowKeySet().isEmpty()) {
                    logger.debug(format("[#%d] There are no fast bbc", id));
                    continue;
                }
                int fcidSeries = Collections.max(fastBbcCons.rowKeySet());
                int fcid = Collections.max(fastBbcCons.row(fcidSeries).keySet());
                logger.debug(format("[#%d] Trying re-participate [cidSeries=%d ; cid=%d]", id, fcidSeries, fcid));
                bbcService.periodicallyVoteMissingConsensus(fastBbcCons.get(fcidSeries, fcid));
            }

        }
    }

    public void shutdown() {
        stopped = true;
        for (peer p : peers.values()) {
            p.shutdown();
        }
        logger.info(format("[#%d] Connections has been shutdown", id));
        if (bbcService != null) {
            bbcService.shutdown();
        }
        try {
            // TODO: Do we really have to interrupt the threads?
            logger.info(format("[#%d] wait for bbcServiceThread", id));
            if (bbcServiceThread != null) {
                bbcServiceThread.interrupt();
                bbcServiceThread.join();
                logger.info(format("[#%d] wait for bbcMissedConsensusThread", id));
            }
            if (bbcMissedConsensusThread != null) {
                bbcMissedConsensusThread.interrupt();
                bbcMissedConsensusThread.join();
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        if (rmfServer != null) {
            rmfServer.shutdown();
        }
        logger.info(format("[#%d] bbc server has been shutdown", id));
    }

    // TODO: This should be called by only one process per round.

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
    /*
     TODO: currently it is possible that after the message was pulled from pending another server sends Res message
            and insert it again. We will probably handle it by adding some cleanup mechanism.
      */

    private void sendReqMessage(RmfGrpc.RmfStub stub, Req req, int cidSeries, int cid, int sender, int height) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                // TODO: We should validate the answer
                if (res.getData().getMeta().getCid() == cid && res.getData().getMeta().getCidSeries() == cidSeries) {
                    synchronized (globalLock) {
                        if (!pendingMsg.contains(cidSeries, cid) && !recMsg.contains(cidSeries, cid)) {
                            if (!rmfDigSig.verify(sender, res.getData())) {
                                logger.info(format("[#%d] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                        id, res.getMeta().getSender(), cidSeries, cid));
                                return;
                            }
                            logger.info(format("[#%d] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, res.getMeta().getSender(), cidSeries,  cid));
                            pendingMsg.put(cidSeries, cid, res.getData());
                            globalLock.notify();
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
    private void broadcastReqMsg(Req req, int cidSeries, int cid, int sender, int height) {
        logger.info(format("[#%d] broadcast request message [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, cidSeries, cid, sender, height);
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

    @Override
    public void disseminateMessage(Data request, StreamObserver<Empty> responseObserver) {
        int sender = request.getMeta().getSender();
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        synchronized (globalLock) {
            synchronized (fastBbcCons) {
                if (fastBbcCons.contains(cidSeries, cid) || regBbcCons.contains(cidSeries, cid)) return;
            }
            if (pendingMsg.contains(cidSeries, cid) || recMsg.contains(cidSeries, cid)) return;
            pendingMsg.put(cidSeries, cid, request);
            logger.info(format("[#%d] received data message from [%d], [cidSeries=%d ; cid=%d]", id,
                    sender, cidSeries, cid));
            globalLock.notify();
        }
    }

    @Override
    public void fastVote(BbcMsg request, StreamObserver<Empty> responeObserver) {
        synchronized (globalLock) {
            int cid = request.getCid();
            int cidSeries = request.getCidSeries();
            if (regBbcCons.contains(cidSeries, cid)) return;
            synchronized (fastBbcCons) {
                if (fastBbcCons.contains(cidSeries, cid)) return; // Against byzantine activity
            }
            if (!fVotes.contains(cidSeries, cid)) {
                fvote v = new fvote();
                v.dec.setCid(cid);
                v.dec.setCidSeries(cidSeries);
                v.cid = cid;
                v.cidSeries = cidSeries;
                v.voters = new ArrayList<>();
                fVotes.put(cidSeries, cid, v);
            }

            if (fVotes.get(cidSeries, cid).voters.contains(request.getPropserID())) {
                logger.info(format("[#%d] received duplicate vote from [%d] for [cidSeries=%d ; cid=%d]",
                        id, request.getPropserID(), cidSeries, cid));
                return;
            }
            fVotes.get(cidSeries, cid).voters.add(request.getPropserID());
            if (fVotes.get(cidSeries, cid).voters.size() == n) {
                logger.info(format("[#%d] fastVote has been detected [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
                synchronized (fastBbcCons) {
                    fastBbcCons.put(cidSeries, cid, fVotes.get(cidSeries, cid).dec.setDecosion(1).build());
                }
                bbcService.updateFastVote(fVotes.get(cidSeries, cid).dec.build());
                globalLock.notify();
            }
        }
    }
    @Override
    public void reqMessage(Req request,  StreamObserver<Res> responseObserver)  {
        Data msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        synchronized (globalLock) {
                msg = recMsg.get(cidSeries, cid);
            if (msg == null) {
                msg = pendingMsg.get(cidSeries, cid);
            }

        }
        if (msg != null) {
            logger.info(format("[#%d] has received request message from [#%d] of [cidSeries=%d ; cid=%d]",
                    id, request.getMeta().getSender(), cidSeries, cid));
            Meta meta = Meta.newBuilder().
                    setSender(id).
                    setHeight(msg.getMeta().getHeight()).
                    setCid(cid).
                    setCidSeries(cid).
                    build();
            responseObserver.onNext(Res.newBuilder().
                    setData(msg).
                    setMeta(meta).
                    build());
        } else {
            logger.info(format("[#%d] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, request.getMeta().getSender(), cidSeries, cid));
        }
    }


    // TODO: Review this method again
    public Data deliver(int cidSeries, int cid, int tmo, int sender, int height) throws InterruptedException {
        int cVotes = 0;
        int v = 0;
        synchronized (globalLock) {
            long startTime = System.currentTimeMillis();
            if (!pendingMsg.contains(cidSeries, cid)) {
                globalLock.wait(tmo);
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            if (pendingMsg.contains(cidSeries, cid)) {
                BbcMsg.Builder bv = BbcMsg
                        .newBuilder().setCid(cid).setCidSeries(cidSeries).setPropserID(id).setVote(1);
                broadcastFastVoteMessage(bv.build());
            }
            if (fVotes.contains(cidSeries, cid)) {
                cVotes = fVotes.get(cidSeries, cid).voters.size();
            }
            if (cVotes < n) {
                globalLock.wait(Math.max(tmo - estimatedTime, 1));
            }

            if (pendingMsg.contains(cidSeries, cid)) {
                v = 1;
            }
            if (fVotes.contains(cidSeries, cid)) {
                cVotes = fVotes.get(cidSeries, cid).voters.size();
            }

            if (cVotes < n) {
                logger.info(format("[#%d] cvotes is [%d] for [cidSeries=%d ; cid=%d]", id, cVotes,cidSeries, cid));
                int dec = fullBbcConsensus(v, cidSeries, cid);
                logger.info(format("[#%d] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, dec, cidSeries, cid));
                if (dec == 0) {
                    pendingMsg.remove(cidSeries, cid);
                    return null;
                }
                if (dec == -1) {
                    return Data.newBuilder().
                            setMeta(Meta.newBuilder().setCid(-1).build()).
                            build();
                }
            } else {
                logger.info(format("[#%d] deliver by fast vote [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
            }
            requestData(cidSeries, cid, sender, height);
            Data msg;
                msg = pendingMsg.get(cidSeries, cid);
                recMsg.put(cidSeries, cid, msg);
                pendingMsg.remove(cidSeries, cid);
            return msg;
        }


    }

    // TODO: We have a little bug here... note that if a process wish to perform a bbc it doesn't mean that other processes know about it. [solved??]
    private int fullBbcConsensus(int vote, int cidSeries, int cid) throws InterruptedException {
        logger.info(format("[#%d] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, cidSeries, cid, vote));
        bbcService.propose(vote, cidSeries, cid);
        BbcDecision dec = bbcService.decide(cidSeries, cid);
        if (dec != null) regBbcCons.put(cidSeries, cid, dec);
        return dec != null ? dec.getDecosion() : -1; // TODO: On shutting down null might expected
    }

    private void requestData(int cidSeries, int cid, int sender, int height) throws InterruptedException {
        if (pendingMsg.contains(cidSeries, cid)) return;
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req, cidSeries, cid, sender, height);
        while (!pendingMsg.contains(cidSeries, cid)) {
            globalLock.wait();
        }
    }

    String getMessageSig(int cidSeries, int cid) {
        if (recMsg.contains(cidSeries, cid)) {
            return recMsg.get(cidSeries, cid).getSig();
        }
        return null;
    }

    public Data nonBlockingDeliver(int cidSeries, int cid) {
        if (recMsg.contains(cidSeries, cid)) {
            return recMsg.get(cidSeries, cid);
        }
        return null;
    }
}
