package das.wrb;

import config.Config;
import config.Node;
import crypto.blockDigSig;
import crypto.sslUtils;
import das.data.Data;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import proto.Types;
import proto.WrbGrpc;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static blockchain.data.BCS.bcs;
import static das.data.Data.addToPendings;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.lang.String.format;

public class WrbRpcs extends WrbGrpc.WrbImplBase {
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
        WrbGrpc.WrbStub stub;

        peer(String addr, int port, String caRoot, String serverCrt, String serverPrivKey) {
            try {
                channel = sslUtils.buildSslChannel(addr, port,
                        sslUtils.buildSslContextForClient(caRoot,
                                serverCrt, serverPrivKey)).
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

    private int id;
    Map<Integer, peer> peers = new HashMap<>();
    private List<Node> nodes;
    private Server wrbServer;
    private int workers;
    private int n;
    protected int f;
    private String serverCrt;
    private String serverPrivKey;
    private String caRoot;

    public WrbRpcs(int id, int workers, int n,  int f,  ArrayList<Node> wrbCluster,
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

        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n.getAddr(),n.getPort(), caRoot, serverCrt, serverPrivKey));
        }

        logger.debug(format("[#%d] initiates wrbRpcs", id));

    }

    void shutdown() {
        for (peer p : peers.values()) {
            p.shutdown();
        }
        wrbServer.shutdown();
    }

    void sendDataMessage(WrbGrpc.WrbStub stub, Types.BlockHeader msg) {
        stub.disseminateMessage(msg, new StreamObserver<>() {
            @Override
            public void onNext(Types.Empty ans) {

            }

            @Override
            public void onError(Throwable throwable) {
                logger.debug(throwable.getMessage());
            }

            @Override
            public void onCompleted() {

            }
        });
    }

    private void sendReqMessage(WrbGrpc.WrbStub stub, Types.WrbReq req, int channel,
                                int cidSeries, int cid, int sender) {
        stub.reqMessage(req, new StreamObserver<>() {
            @Override
            public void onNext(Types.WrbRes res) {
                if (res.equals(Types.WrbRes.getDefaultInstance())) return;
                Types.Meta key = Types.Meta.newBuilder()
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
                                id, channel, res.getSender(), cidSeries, cid));
                        return;
                    }
                    logger.debug(format("[#%d-C[%d]] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                            id, channel, res.getSender(), cidSeries, cid));
                    synchronized (Data.pending[channel]) {
                        Data.pending[channel].putIfAbsent(key, res.getData());
                        Data.pending[channel].notify();
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

    void broadcastReqMsg(Types.WrbReq req, int channel, int cidSeries, int cid, int sender) {
        logger.debug(format("[#%d-C[%d]] broadcasts request message [cidSeries=%d ; cid=%d; height=%d]", id,
                req.getMeta().getChannel(), cidSeries, cid, req.getHeight()));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, channel, cidSeries, cid, sender);
        }
    }

    void wrbSend(Types.BlockHeader msg, int[] recipients) {
        for (int i : recipients) {
            sendDataMessage(peers.get(i).stub, msg);
        }

    }
    void wrbBroadcast(Types.BlockHeader msg) {
        for (peer p : peers.values()) {
            sendDataMessage(p.stub, msg);
        }
    }

    @Override
    public void disseminateMessage(Types.BlockHeader request, StreamObserver<Types.Empty> responseObserver) {
        Types.Meta key1 = Types.Meta.newBuilder()
                .setChannel(request.getM().getChannel())
                .setCidSeries(request.getM().getCidSeries())
                .setCid(request.getM().getCid())
                .build();
        logger.debug(format("received a header for [w=%d; cidSeries=%d; cid=%d]", key1.getChannel(),
                key1.getCidSeries(), key1.getCid()));
        addToPendings(request, key1);
        responseObserver.onNext(Types.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void reqMessage(Types.WrbReq request, StreamObserver<Types.WrbRes> responseObserver)  {
        Types.BlockHeader msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int channel = request.getMeta().getChannel();
        Types.Meta key =  Types.Meta.newBuilder()
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
                    id, channel, request.getSender(), cidSeries, cid));
            Types.Meta meta = Types.Meta.newBuilder().
                    setChannel(channel).
                    setCid(cid).
                    setCidSeries(cidSeries).
                    build();
            responseObserver.onNext(Types.WrbRes.newBuilder().
                    setData(msg).
                    setM(meta).
                    setSender(id).
                    build());
        } else {
            logger.debug(format("[#%d-C[%d]] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, channel, request.getSender(), cidSeries, cid));
            responseObserver.onNext(Types.WrbRes.getDefaultInstance());
        }
        responseObserver.onCompleted();
    }
}
