package communication.overlays.clique;

import communication.data.Data;
import config.Node;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import proto.CommunicationGrpc;
import proto.Types;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static blockchain.data.BCS.bcs;
import static crypto.blockDigSig.verfiyBlockWRTheader;
import static java.lang.String.format;

public class CliqueRpcs  extends CommunicationGrpc.CommunicationImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CliqueRpcs.class);

    class Peer {
        ManagedChannel channel;
        CommunicationGrpc.CommunicationStub stub;

        Peer(String addr, int port) {
            channel = ManagedChannelBuilder
                    .forAddress(addr, port)
                    .usePlaintext()
                    .maxInboundMessageSize(16 * 1024 * 1024)
                    .build();

            stub = CommunicationGrpc.newStub(channel);
        }

        void shutdown() {
            channel.shutdown();
        }

    }

    private Server rpcServer;
    private Map<Integer, Peer> peers = new HashMap<>();
    private List<Node> nodes;
    int id;
    int n;

    public CliqueRpcs(int id, ArrayList<Node> commCluster, int n) {
        this.id = id;
        this.nodes = commCluster;
        this.n = n;
    }
    // TODO: Re configure grpc server so it would be anble to handle mass of messages.
    public void start() {
//        Executor executor = Executors.newFixedThreadPool(n);
        rpcServer =
                NettyServerBuilder
                .forPort(nodes.get(id).getPort())
                .addService(this)
                .maxInboundMessageSize(16 * 1024 * 1024)
//                .maxConcurrentCallsPerConnection(2 * n * workers)
//                .executor(executor)
                .build();
        try {
            rpcServer.start();
        } catch (IOException e) {
            logger.error(format("[%d] error while starting server", id), e);
        }

        for (Node n : nodes) {
            peers.put(n.getID(), new Peer(n.getAddr(), n.getPort()));
        }
        logger.debug("starting clique rpc server");
    }

    public void shutdown() {
        rpcServer.shutdown();
        for (Peer p : peers.values()) {
            p.shutdown();
        }
        logger.debug("Shutting down clique rpc server");
    }

    public void broadcast(int worker, Types.Block data) {
        for (Peer p : peers.values()) {
            sendImpl(Types.Comm.newBuilder()
                    .setChannel(worker)
                    .setData(data)
                    .build(), p);
            }

    }

    private void sendImpl(Types.Comm msg, Peer p) {
            p.stub.dsm(msg, new StreamObserver<Types.Empty>() {
                @Override
                public void onNext(Types.Empty empty) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {
                }
            });
    }

    public void send(int worker, Types.Block data, int[] recipients) {
        for (int i : recipients) {
            Peer p = peers.get(i);
           sendImpl(Types.Comm.newBuilder()
                   .setChannel(worker)
                   .setData(data)
                   .build(), p);

        }
    }

    @Override
    public void reqBlock(proto.Types.commReq request,
                         io.grpc.stub.StreamObserver<proto.Types.commRes> responseObserver) {
        Types.BlockID bid = request.getProof().getBid();
        int height = request.getProof().getHeight();
        int channel = request.getProof().getM().getChannel();
        int pid = request.getProof().getBid().getPid();
        logger.debug(format("[%d-C[%d]] received commReq [height=%d]", id, channel, height));
        Types.Block res[] = {null};
        Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {

            LinkedList<Types.Block> ll = v.stream().filter(b -> verfiyBlockWRTheader(b, request.getProof()))
                    .collect(Collectors.toCollection(LinkedList::new));

            res[0] = ll.peek();
            return v;
        });
        if (res[0] == null) {
            res[0] = bcs[channel].getBlock(height);
        }
        if (res[0] != null) {
            logger.debug(format("[%d-C[%d]] received commReq and has a match [height=%d]", id, channel, height));
            Types.commRes cr = Types.commRes.newBuilder().setB(res[0]).build();
            responseObserver.onNext(cr);
        }
        responseObserver.onCompleted();

    }

    void broadcastCommReq(Types.commReq request) {
        for (Peer p : peers.values()) {
            p.stub.reqBlock(request, new StreamObserver<Types.commRes>() {
                @Override
                public void onNext(Types.commRes commRes) {

                    int channel = request.getProof().getM().getChannel();
                    int pid = request.getProof().getBid().getPid();
                    Types.BlockID bid = request.getProof().getBid();
                    if (verfiyBlockWRTheader(commRes.getB(), request.getProof())) {
                        logger.debug(format("[%d-C[%d]] received valid commRes [height=%d]", id, channel,
                                request.getProof().getHeight()));
                        synchronized (Data.blocks[pid][channel]) {
                            Data.blocks[pid][channel].putIfAbsent(bid, new LinkedList<>());
                            Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {
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
    public void dsm(Types.Comm request, StreamObserver<proto.Types.Empty> responseObserver) {
        int c = request.getChannel();
        int pid = request.getData().getId().getPid();
        Data.blocks[pid][c].putIfAbsent(request.getData().getId(), new LinkedList<Types.Block>());
        synchronized (Data.blocks[pid][c]) {
            Data.blocks[pid][c].computeIfPresent(request.getData().getId(), (k, v) -> {
                Types.BlockID bid = request.getData().getId();
                logger.debug(format("[%d-%d] received block [pid=%d ; bid=%d]", id, c, bid.getPid(), bid.getBid()));
                v.add(request.getData());
                return v;
            });
            Data.blocks[pid][c].notifyAll();
            responseObserver.onNext(Types.Empty.newBuilder().build());
            responseObserver.onCompleted();
        }


    }


}
