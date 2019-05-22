package das.bbc;

import blockchain.data.BCS;
import utils.Node;
import crypto.BlockDigSig;
import crypto.SslUtils;
import das.data.BbcDecData;
import das.data.Data;
import das.data.VoteData;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import proto.ObbcGrpc;
import proto.Types;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static das.data.Data.*;
import static das.data.Data.bbcFastDec;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.lang.String.format;

public class OBBCRpcs extends ObbcGrpc.ObbcImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OBBCRpcs.class);

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
    class Peer {
        ManagedChannel channel;
        ObbcGrpc.ObbcStub stub;
        AtomicInteger[] pending;
        int maxPending = 1000;

        Peer(String addr, int port, int workers, String caRoot, String serverCrt, String serverPrivKey) {
            try {
                channel = SslUtils.buildSslChannel(addr, port,
                        SslUtils.buildSslContextForClient(caRoot,
                                serverCrt, serverPrivKey)).
                        intercept(new clientTlsIntercepter()).build();
            } catch (SSLException e) {
                logger.fatal("", e);
            }
            pending = new AtomicInteger[workers];
            for (int i = 0 ; i < workers ; i++) {
                pending[i] = new AtomicInteger(0);
            }
            stub = ObbcGrpc.newStub(channel);
        }

        void send(Types.BbcMsg msg, int w) {
//            if (pending[w].get() > maxPending) return;
//            pending[w].incrementAndGet();
            stub.fastVote(msg, new StreamObserver<Types.Empty>() {
                @Override
                public void onNext(Types.Empty empty) {
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

    private Server ObbcInnerServer;
    private Map<Integer, Peer> peers = new HashMap<>();
    private List<Node> nodes;
    private int id;
    private int n;
    private int f;
    private int qSize;
    private String caRoot;
    private String serverCrt;
    private String serverPrivKey;
    private int workers;

    OBBCRpcs(int id, int n, int f, int workers, int qSize, ArrayList<Node> obbcCluster, String caRoot, String serverCrt,
             String serverPrivKey) {
        this.id = id;
        this.nodes = obbcCluster;
        this.n = n;
        this.f = f;
        this.qSize = qSize;
        this.caRoot = caRoot;
        this.serverCrt = serverCrt;
        this.serverPrivKey = serverPrivKey;
        this.workers = workers;
        logger.info(format("Initiated OBBCRpcs: [id=%d; n=%d; f=%d; qSize=%d]", id, n, f, qSize));
    }

    public void start() {
        try {
            ObbcInnerServer = NettyServerBuilder.
                    forPort(nodes.get(id).getPort())
                    .sslContext(SslUtils.buildSslContextForServer(serverCrt,
                            caRoot, serverPrivKey)).
                            addService(this).
                            intercept(new authInterceptor()).
//                            maxConcurrentCallsPerConnection(1000).
                            build().
                            start();
        } catch (IOException e) {
            logger.fatal("", e);
        }

        for (Node n : nodes) {
            peers.put(n.getID(), new Peer(n.getAddr(), n.getPort(), workers, caRoot, serverCrt, serverPrivKey));
        }
    }

    public void shutdown() {
        ObbcInnerServer.shutdown();
        for (Peer p : peers.values()) {
            p.shutdown();
        }
    }

    private void handlePgbMsg(Types.BbcMsg msg) {
        Types.BlockHeader nxt = msg.getNext();
//        int sender = nxt.getBid().getPid();
        int cid = nxt.getM().getCid();
        int worker = nxt.getM().getChannel();
        int cidSeries = nxt.getM().getCidSeries();
        Types.Meta key = Types.Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();
        if (BCS.contains(worker, nxt.getHeight())) return;
        Data.addToPendings(nxt, key);
    }

    @Override
    public void fastVote(proto.Types.BbcMsg request,
                         StreamObserver<Types.Empty> responseObserver) {


        int cid = request.getM().getCid();
        int cidSeries = request.getM().getCidSeries();
        int channel = request.getM().getChannel();
        Types.Meta key = request.getM();

        if (request.hasNext()) {
            handlePgbMsg(request);
        }


        fvData[channel].putIfAbsent(key, new VoteData());
        fvData[channel].computeIfPresent(key, (k, v) -> {
            if (v.getVotersNum() == qSize) return v;
            if (!v.addVote(request.getSender(), request.getVote())) {
                logger.debug(format("[#%d-C[%d]] received an invalid fv message [cidSeries=%d ; cid=%d ; sender=%d, v=%b]",
                        id, channel, cidSeries, cid, request.getSender(), request.getVote()));
                return v;
            }
            logger.debug(format("[#%d-C[%d]] received valid fv message [cidSeries=%d ; cid=%d ; sender=%d, v=%b]",
                    id, channel, cidSeries, cid, request.getSender(), request.getVote()));
            if (v.getVotersNum() == qSize) {
                boolean dec = v.posFastVote(qSize);
                logger.debug(format("Add fastVote decision for [w=%d ; cidSeries=%d ; cid=%d ; dec=%b]", channel, cidSeries, cid, dec));
                addNewFastDec(channel, key, dec, request.getHeight());
            }
            return v;
        });
        responseObserver.onNext(Types.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    private void addNewFastDec(int worker, Types.Meta key, boolean dec, int height) {

        synchronized (bbcFastDec[worker]) {
            bbcFastDec[worker].computeIfAbsent(key, k1 -> {
                if (dec) {
                    bbcVotes[worker].computeIfPresent(key, (k2, v2) -> {
                        logger.debug(format("[#%d-C[%d]] (addNewFastDec) found that a full bbc initialized, thus propose [cidSeries=%d ; cid=%d]",
                                id, worker, key.getCidSeries(),  key.getCid()));
                        BBC.nonBlockingPropose(Types.BbcMsg.newBuilder()
                                .setM(key)
                                .setHeight(height)
                                .setSender(id)
                                .setVote(dec).build());
                        return v2;
                    });
                }
                return new BbcDecData(dec, true);
            });
            bbcFastDec[worker].notifyAll();
        }
    }

    @Override
    public void evidenceReqMessage(proto.Types.EvidenceReq request,
                                   StreamObserver<proto.Types.EvidenceRes> responseObserver) {
        Types.BlockHeader msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        int worker = request.getMeta().getChannel();
        Types.Meta key =  Types.Meta.newBuilder()
                .setChannel(worker)
                .setCidSeries(cidSeries)
                .setCid(cid)
                .build();

        msg = Data.pending[worker].get(key);
        if (msg == null && BCS.contains(worker, request.getHeight())) {
            msg = BCS.nbGetBlock(worker, request.getHeight()).getHeader();
        }
        if (msg == null) {
            msg = Types.BlockHeader.getDefaultInstance();
            logger.debug(format("[#%d-C[%d]] has received evidence request message" +
                            " from [#%d] of [cidSeries=%d ; cid=%d] response with NULL",
                    id, worker, request.getSender(), cidSeries, cid));

        } else {
            logger.debug(format("[#%d-C[%d]] has received evidence request message from [#%d] of [cidSeries=%d ; cid=%d] responses with a value",
                    id, worker, request.getSender(), cidSeries, cid));
        }

        Types.Meta meta = Types.Meta.newBuilder().
                setChannel(worker).
                setCid(cid).
                setCidSeries(cidSeries).
                build();
        responseObserver.onNext(Types.EvidenceRes.newBuilder().
                setData(msg).
                setSender(id).
                setM(meta).
                build());
        responseObserver.onCompleted();
    }

    private void sendFVMessage(Peer p, Types.BbcMsg v) {
        p.send(v, v.getM().getChannel());
    }

    void broadcastFVMessage(Types.BbcMsg v) {
        for (int p : peers.keySet()) {
            sendFVMessage(peers.get(p), v);
        }
    }

    private void sendEvidenceReqMessage(ObbcGrpc.ObbcStub stub, Types.EvidenceReq req, Types.Meta key, int expSender) {
        stub.evidenceReqMessage(req, new StreamObserver<Types.EvidenceRes>() {
            @Override
            public void onNext(Types.EvidenceRes res) {

                int worker = key.getChannel();
                int cidSeries = key.getCidSeries();
                int cid = key.getCid();
                if (res.getM().getChannel() != worker
                        || res.getM().getCidSeries() != cidSeries
                        || res.getM().getCid() != cid) return;
                preConsVote[worker].computeIfAbsent(key, k -> new ArrayList<>());
                if (preConsVote[worker].get(key).contains(res.getSender())) return;
                preConsVote[worker].computeIfPresent(key, (k, val) -> {
                    if (val.size() > n - f) return val;
                    val.add(res.getSender());
                    logger.debug(format("[#%d-C[%d]] received evidence response from [%d] [cidSeries=%d ; cid=%d]",
                            id, worker, res.getSender(), cidSeries, cid));

                    if ((!res.getData().equals(Types.BlockHeader.getDefaultInstance()))
                            && res.getData().getM().getCid() == cid
                            && res.getData().getM().getCidSeries() == cidSeries
                            && res.getData().getM().getChannel() == worker
                            && res.getSender() == expSender) {
                        if (!BlockDigSig.verifyHeader(expSender, res.getData())) {
                            logger.debug(format("[#%d-C[%d]] has evidence received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, worker, res.getSender(), cidSeries, cid));
                            return val;
                        }
                        if (!Data.pending[worker].containsKey(key)) {
                            logger.debug(format("[#%d-C[%d]] has evidence received message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, worker, res.getSender(), cidSeries, cid));
                            Data.pending[worker].putIfAbsent(key, res.getData());
                        }
                    } else if (res.getData().equals(Types.BlockHeader.getDefaultInstance())) {
                        logger.debug(format("[#%d-C[%d]] has evidence received NULL message from [#%d] for [cidSeries=%d ; cid=%d]",
                                id, worker, res.getSender(), cidSeries,  cid));
                    }

                    if (val.size() == n - f) {
                        synchronized (preConsDone[worker]) {
                            preConsDone[worker].add(key);
                            logger.debug(format("[#%d-C[%d]] notify on evidence for [cidSeries=%d ; cid=%d]",
                                    id, worker, cidSeries,  cid));
                            preConsDone[worker].notify();
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

    void broadcastEvidenceReq(Types.EvidenceReq req, Types.Meta key, int expSender) {
        for (int p : peers.keySet()) {
            sendEvidenceReqMessage(peers.get(p).stub, req, key, expSender);
        }
    }

}
