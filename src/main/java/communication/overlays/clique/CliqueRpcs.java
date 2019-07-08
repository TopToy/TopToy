package communication.overlays.clique;

import blockchain.data.BCS;
import communication.data.Data;
import proto.prpcs.commService.CommunicationGrpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static crypto.BlockDigSig.verfiyBlockWRTheader;
import static java.lang.String.format;
import static proto.prpcs.commService.CommunicationGrpc.newStub;
import proto.types.block.*;
import proto.types.comm.*;
import proto.types.utils.Empty;
import utils.config.yaml.ServerPublicDetails;

public class CliqueRpcs  extends CommunicationImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CliqueRpcs.class);

    class Peer {
        ManagedChannel channel;
        CommunicationStub stub;
        AtomicInteger[] pending;
        int maxPendings = 10;

        Peer(String addr, int port, int workers) {
            channel = ManagedChannelBuilder
                    .forAddress(addr, port)
                    .usePlaintext()
                    .maxInboundMessageSize(16 * 1024 * 1024)
                    .build();
            pending = new AtomicInteger[workers];
            for (int i = 0 ; i < workers ; i++) {
                pending[i] = new AtomicInteger(0);
            }
            stub = newStub(channel);
        }

        void send(Comm comm, int w) {
            if (pending[w].get() > maxPendings) return;
            pending[w].incrementAndGet();
            stub.dsm(comm, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty empty) {
                    pending[w].decrementAndGet();
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

    private Server rpcServer;
    private Map<Integer, Peer> peers = new HashMap<>();
    private ServerPublicDetails[] nodes;
    private int id;
    private int n;
    private int workers;

    CliqueRpcs(int id, ServerPublicDetails[] cluster, int n, int workers) {
        this.id = id;
        this.nodes = cluster;
        this.n = n;
        this.workers = workers;
    }

    void reconfigure() {

    }

    // TODO: Re configure grpc server so it would be enable to handle mass of messages.
    public void start() {
//        Executor executor = Executors.newFixedThreadPool(n);
        rpcServer =
                NettyServerBuilder
                .forPort(nodes[id].getCommPort())
                .addService(this)
                .maxInboundMessageSize(16 * 1024 * 1024)
                .maxConcurrentCallsPerConnection(10)
//                .executor(executor)
                .build();
        try {
            rpcServer.start();
        } catch (IOException e) {
            logger.error(format("[%d] error while starting server", id), e);
        }

        for (ServerPublicDetails n : nodes) {
            peers.put(n.getId(), new Peer(n.getIp(), n.getCommPort(), workers));
        }
        logger.debug("starting clique rpc server");
    }

    public void shutdown() {
        rpcServer.shutdownNow();
        for (Peer p : peers.values()) {
            p.shutdown();
        }
        logger.debug("Shutting down clique rpc server");
    }

    void broadcast(int worker, Block data) {
        for (Peer p : peers.values()) {
            sendImpl(Comm.newBuilder()
                    .setChannel(worker)
                    .setData(data.toBuilder().setBst(BlockStatistics.newBuilder()
                            .setProposeTime(System.currentTimeMillis()).build()).build())
                    .build(), p);
            }

    }

    private void sendImpl(Comm msg, Peer p) {
        p.send(msg, msg.getChannel());
//            p.stub.dsm(msg, new StreamObserver<Empty>() {
//                @Override
//                public void onNext(Empty empty) {
//
//                }
//
//                @Override
//                public void onError(Throwable throwable) {
//
//                }
//
//                @Override
//                public void onCompleted() {
//                }
//            });
    }

    public void send(int worker, Block data, int[] recipients) {
        for (int i : recipients) {
            Peer p = peers.get(i);
           sendImpl(Comm.newBuilder()
                   .setChannel(worker)
                   .setData(data.toBuilder().setBst(BlockStatistics.newBuilder()
                           .setProposeTime(System.currentTimeMillis()).build()).build())
                   .build(), p);

        }
    }

    @Override
    public void reqBlock(CommReq request,
                         io.grpc.stub.StreamObserver<CommRes> responseObserver) {
        BlockID bid = request.getProof().getBid();
        int height = request.getProof().getHeight();
        int worker = request.getProof().getM().getChannel();
        int pid = request.getProof().getBid().getPid();
        logger.debug(format("[%d-C[%d]] received commReq [height=%d]", id, worker, height));
        Block[] res = {null};
        Data.blocks[pid][worker].computeIfPresent(bid, (k, v) -> {

            LinkedList<Block> ll = v.stream().filter(b -> verfiyBlockWRTheader(b, request.getProof()))
                    .collect(Collectors.toCollection(LinkedList::new));

            res[0] = ll.peek();
            return v;
        });
        CommRes cr = CommRes.getDefaultInstance();
        if (res[0] == null) {
            res[0] = BCS.nbGetBlock(worker, height);
        }
        if (res[0] != null) {
            logger.debug(format("[%d-C[%d]] received commReq and has a match [height=%d]", id,
                    worker, height));
            cr = CommRes.newBuilder().setB(res[0]).build();

        }
        responseObserver.onNext(cr);
        responseObserver.onCompleted();

    }

    void broadcastCommReq(CommReq request) {
        for (Peer p : peers.values()) {
            p.stub.reqBlock(request, new StreamObserver<>() {
                @Override
                public void onNext(CommRes commRes) {
                    if (commRes.equals(commRes.getDefaultInstance())) return;
                    int channel = request.getProof().getM().getChannel();
                    int pid = request.getProof().getBid().getPid();
                    BlockID bid = request.getProof().getBid();
                    if (verfiyBlockWRTheader(commRes.getB(), request.getProof())) {
                        logger.debug(format("[%d-C[%d]] received valid commRes [height=%d]", id, channel,
                                request.getProof().getHeight()));
                        synchronized (Data.blocks[pid][channel]) {
                            Data.blocks[pid][channel].putIfAbsent(bid, new LinkedList<>());
                            Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {
                                if (v.size() > 0) return v;
                                v.add(commRes.getB());
                                return v;
                            });
                            Data.blocks[pid][channel].notifyAll();
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
    }

    @Override
    public void dsm(Comm request, StreamObserver<Empty> responseObserver) {
        int c = request.getChannel();
        int pid = request.getData().getId().getPid();
        Data.blocks[pid][c].putIfAbsent(request.getData().getId(), new LinkedList<>());
        synchronized (Data.blocks[pid][c]) {
            Data.blocks[pid][c].computeIfPresent(request.getData().getId(), (k, v) -> {
                if (v.contains(request.getData())) return v;
                BlockID bid = request.getData().getId();
                logger.debug(format("[%d-%d] received block [pid=%d ; bid=%d]", id, c, bid.getPid(), bid.getBid()));
                v.add(request.getData());
                return v;
            });
            Data.blocks[pid][c].notifyAll();
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }


    }


}
