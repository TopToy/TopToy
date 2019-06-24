package das.wrb;

import blockchain.data.BCS;
import proto.prpcs.wrbService.WrbGrpc.*;
import utils.Node;
import crypto.BlockDigSig;
import crypto.SslUtils;
import das.data.Data;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static das.data.Data.addToPendings;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.lang.String.format;
import static proto.prpcs.wrbService.WrbGrpc.newStub;
import proto.types.block.*;
import proto.types.utils.Empty;
import proto.types.wrb.*;
import proto.types.meta.*;

public class WrbRpcs extends WrbImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WRB.class);


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
        WrbStub stub;
        AtomicInteger[] pending;
        int maxPending = 100;

        peer(String addr, int port, int workers, String caRoot, String serverCrt, String serverPrivKey) {
            try {
                channel = SslUtils.buildSslChannel(addr, port,
                        SslUtils.buildSslContextForClient(caRoot,
                                serverCrt, serverPrivKey)).
                        intercept(new clientTlsIntercepter()).build();
            } catch (SSLException e) {
                logger.fatal(format("[#%d]", id), e);
            }
            pending = new AtomicInteger[workers];
            for (int i = 0 ; i < workers ; i++) {
                pending[i] = new AtomicInteger(0);
            }
            stub = newStub(channel);
        }
        void send(BlockHeader h, int w) {
//            if (pending[w].get() > maxPending) return;
//            pending[w].incrementAndGet();
            stub.disseminateMessage(h, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty empty) {
//                    pending[w].decrementAndGet();
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });
        }
        void shutdown() {
            channel.shutdown();
        }
    }

    private int id;
    private Map<Integer, peer> peers = new HashMap<>();
    private List<Node> nodes;
    private Server wrbServer;
    private int workers;
    private int n;
    protected int f;
    private String serverCrt;
    private String serverPrivKey;
    private String caRoot;

    WrbRpcs(int id, int workers, int n, int f, ArrayList<Node> wrbCluster,
            String serverCrt, String serverPrivKey, String caRoot) {
        this.workers = workers;
        this.id = id;
        this.n = n;
        this.f = f;
        this.nodes = wrbCluster;
        this.serverCrt = serverCrt;
        this.serverPrivKey = serverPrivKey;
        this.caRoot = caRoot;
        logger.info(format("Initiated WrbRpcs: [id=%d; n=%d; f=%d]", id, n, f));
    }

    void start() {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            logger.debug(format("[#%d] There are %d CPU's in the system", id, cores));
            Executor executor = Executors.newFixedThreadPool(n);
            EventLoopGroup weg = new NioEventLoopGroup(cores);
            wrbServer = NettyServerBuilder.
                    forPort(nodes.get(id).getPort())
                    .executor(executor)
                    .workerEventLoopGroup(weg)
                    .bossEventLoopGroup(weg)
//                    .maxConcurrentCallsPerConnection(100)
                    .sslContext(SslUtils.buildSslContextForServer(serverCrt,
                            caRoot, serverPrivKey)).
                            addService(this).
                            intercept(new authInterceptor()).
                            build().
                            start();
        } catch (IOException e) {
            logger.fatal(format("[#%d]", id), e);
        }

        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n.getAddr(),n.getPort(), workers, caRoot, serverCrt, serverPrivKey));
        }

        logger.debug(format("[#%d] initiates wrbRpcs", id));

    }

    void shutdown() {
        for (peer p : peers.values()) {
            p.shutdown();
        }
        wrbServer.shutdown();
    }

    private void sendDataMessage(peer p, BlockHeader msg) {
        p.send(msg, msg.getM().getChannel());
    }

    private void sendReqMessage(WrbStub stub, WrbReq req, int worker,
                                int cidSeries, int cid, int sender) {
        stub.reqMessage(req, new StreamObserver<>() {
            @Override
            public void onNext(WrbRes res) {
                if (res.equals(WrbRes.getDefaultInstance())) return;
                Meta key = Meta.newBuilder()
                        .setChannel(worker)
                        .setCidSeries(cidSeries)
                        .setCid(cid)
                        .build();
                if (res.getData().getM().getCid() == cid &&
                        res.getData().getM().getCidSeries() == cidSeries
                        && res.getM().getChannel() == worker &&
                        res.getData().getM().getChannel() == worker) {
                    if (Data.pending[worker].containsKey(key) || BCS.contains(worker, req.getHeight())) return;
                    if (!BlockDigSig.verifyHeader(sender, res.getData())) {
                        logger.debug(format("[#%d-C[%d]] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                id, worker, res.getSender(), cidSeries, cid));
                        return;
                    }
                    logger.debug(format("[#%d-C[%d]] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                            id, worker, res.getSender(), cidSeries, cid));
                    synchronized (Data.pending[worker]) {
                        Data.pending[worker].putIfAbsent(key, res.getData());
                        Data.pending[worker].notify();
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

    void broadcastReqMsg(WrbReq req, int channel, int cidSeries, int cid, int sender) {
        logger.debug(format("[#%d-C[%d]] broadcasts request message [cidSeries=%d ; cid=%d; height=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid, req.getHeight()));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender);
        }
    }

    void wrbSend(BlockHeader msg, int[] recipients) {
        for (int i : recipients) {
            sendDataMessage(peers.get(i), msg);
        }

    }
    void wrbBroadcast(BlockHeader msg) {
        for (peer p : peers.values()) {
            sendDataMessage(p, msg);
        }
    }

    @Override
    public void disseminateMessage(BlockHeader request, StreamObserver<Empty> responseObserver) {
        Meta key1 = request.getM();
        logger.debug(format("received a header for [w=%d; cidSeries=%d; cid=%d]", key1.getChannel(),
                key1.getCidSeries(), key1.getCid()));
        addToPendings(request, key1);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void reqMessage(WrbReq request, StreamObserver<WrbRes> responseObserver)  {
        BlockHeader msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int worker = request.getMeta().getChannel();
        Meta key =  Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        msg = Data.pending[worker].get(key);
        if (msg == null && BCS.contains(worker, request.getHeight())) {
            msg = BCS.nbGetBlock(worker, request.getHeight()).getHeader();
        }
        if (msg != null) {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d]",
                    id, worker, request.getSender(), cidSeries, cid));
            Meta meta = Meta.newBuilder().
                    setChannel(worker).
                    setCid(cid).
                    setCidSeries(cidSeries).
                    build();
            responseObserver.onNext(WrbRes.newBuilder().
                    setData(msg).
                    setM(meta).
                    setSender(id).
                    build());
        } else {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, worker, request.getSender(), cidSeries, cid));
            responseObserver.onNext(WrbRes.getDefaultInstance());
        }
        responseObserver.onCompleted();
    }
}
