//package sync;
//
//import blockchain.blockchain;
//import com.google.protobuf.InvalidProtocolBufferException;
//import config.Config;
//import config.Node;
//import consensus.RBroadcast.RBrodcastService;
//import crypto.bbcDigSig;
//import crypto.blockDigSig;
//import crypto.frontSupportDigSig;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import io.grpc.Server;
//import io.grpc.ServerBuilder;
//import io.grpc.stub.StreamObserver;
//import proto.*;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static java.lang.String.format;
//
//public class syncService extends syncServiceGrpc.syncServiceImplBase{
//    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(syncService.class);
//    int id;
//    boolean stopped = false;
//    int f;
//    int sid = 0;
//    int syncPort;
//    protected Server syncServer;
////    rmfSer
//    protected RBrodcastService syncRB;
//    final Object globalLock = new Object();
//    Map<Integer, peer> peers;
//    List<Node> nodes;
//    HashMap<Integer, ArrayList<frontSupport>> recFS;
////    Thread rbThread;
//    class peer {
//        ManagedChannel channel;
//        syncServiceGrpc.syncServiceStub stub;
//
//        peer(Node node) {
//            channel = ManagedChannelBuilder.
//                    forAddress(node.getAddr(), node.getSyncPort()).
//                    usePlaintext().
//                    build();
//            stub = syncServiceGrpc.newStub(channel);
//        }
//
//        void shutdown() {
//            try {
//                channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                logger.error("", e);
//            }
//        }
//    }
//
//    public syncService(int id, int f, int syncPort, ArrayList<Node> nodes) {
//        this.id = id;
//        this.nodes = nodes;
//        this.f = f;
//        this.recFS = new HashMap<>();
//        this.peers = new HashMap<>();
//        this.syncPort = syncPort;
//        syncRB = new RBrodcastService(id, Config.getSyncRBConfigHome());
//
//    }
//    void startGrpcServer() {
//        try {
//            syncServer = ServerBuilder.
//                    forPort(syncPort).
//                    addService(this).
//                    build().
//                    start();
//        } catch (IOException e) {
//            logger.error(format("[#%d]", id), e);
//        }
//        logger.info(format("[#%d] sync server is up", id));
//
//    }
//    public void start() {
//        startGrpcServer();
//        syncRB.start();
////        rbThread.start();
//        for (Node n : nodes) {
//            peers.put(n.getID(), new peer(n));
//        }
//
//    }
//
//    public void shutdown() {
//        stopped = true;
//        syncRB.shutdown();
//        for (peer p : peers.values()) {
//            p.shutdown();
//        }
//    }
//
//    protected void sendSupport(syncServiceGrpc.syncServiceStub stub, frontSupport s) {
//        stub.supportFront(s, new StreamObserver<Empty>() {
//            @Override
//            public void onNext(Empty empty) {
//
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });
//    }
//
//    public int broadcastSupport(frontSupport s) {
//        frontSupport.Builder fs = s.toBuilder().setSid(sid);
//        fs.setSig(frontSupportDigSig.sign(fs));
//        for (peer p : peers.values()) {
//            sendSupport(p.stub, fs.build());
//        }
//        int ret = sid;
//        sid++;
//        return ret;
//    }
//    @Override
//    public void supportFront(frontSupport request, StreamObserver<proto.Empty> responseObserver) {
//        synchronized (globalLock) {
//            if (!frontSupportDigSig.verify(request.getId(), request)) {
//                logger.info(format("[#%d] received an invalid front support", id));
//                return;
//            }
//            // TODO: Byzantine process may fake the sid
//            int rsid = request.getSid();
//            ArrayList<frontSupport> fs = new ArrayList<>();
//            if (!recFS.containsKey(rsid)) {
//                recFS.put(rsid, fs);
//            }
//            recFS.get(rsid).add(request);
//            globalLock.notify();
//
//        }
//    }
//    public ArrayList<frontSupport> deliver(int height, int sid) throws InterruptedException {
//        synchronized (globalLock) {
//            // TODO: As the from may last for the last two rounds we need to add + 1 to the support
//            // TODO: Also, note that we might not even need to proof the front, as the validation of the chain should do it!
//            // TODO: So, currently we do, but basically we shouldn't?
//            // TODO: Currently we eait for n asnwer from Roy...
//            while (!recFS.containsKey(sid) || recFS.get(sid).stream().filter(r -> r.getHeight() + 1 >= height).count() < f + 1) {
//                globalLock.wait();
//            }
//            return recFS.get(sid);
//        }
//    }
//
//    public void dessiminateSubVersion(subChainVersion v, int id) {
//        syncRB.broadcast(v.toByteArray(), id);
//    }
//
//    public subChainVersion deliverSubChainVersion() throws InterruptedException {
//        try {
//            subChainVersion v = subChainVersion.parseFrom(syncRB.deliver());
//            for (frontSupport s : v.getFpList()) {
//                if (!frontSupportDigSig.verify(s.getId(), s)) {
//                    return null;
//                }
//            }
//            if (v.getFpList().size() < f + 1) {
//                return null;
//            }
//            for (proofedBlock pb : v.getVList()) {
//                if (!blockDigSig.verify(pb.getB().getHeader().getCreatorID(),
//                        pb.getB().getHeader().getCid(), pb.getSig(), pb.getB())) {
//                    return null;
//                }
//            }
//            if (v.getVList().size() < f) {
//                return null;
//            }
//            return v;
//        } catch (InvalidProtocolBufferException e) {
//            logger.error("", e);
//            return null;
//        }
//    }
//
//
////    public void sync(blockchain bc, int forkPoint) throws InterruptedException {
////        frontSupport.Builder fs = frontSupport.newBuilder().
////                setHeight(bc.getHeight()).
////                setId(id).
////                setSid(sid);
////        broadcastSupport(fs.setSig(frontSupportDigSig.sign(fs)).build());
////        ArrayList<frontSupport> lfs = deliver(bc.getHeight(), sid);
////
////    }
//
//
////    void disseminateChainVersion(blockchain bc, int forkPoint) {
////        int low = forkPoint + 1 - f;
////        int high = bc.getHeight() + 1;
////        subChainVersion.Builder sv = subChainVersion.newBuilder();
////        for (Block b : bc.getBlocks(low, high)) {
////            int cid = b.getHeader().getCid();
////            proofedBlock pb = proofedBlock.newBuilder().
////                    setCid(cid).
////                    setSig(rmfServer.getRmfDataSig(cid)).
////                    setB(b).
////                    build();
////            sv.addV(pb);
////        }
////        syncRB.broadcast(sv.build().toByteArray(), getID());
////    }
//}
