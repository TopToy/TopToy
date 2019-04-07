package communication.overlays;

import blockchain.Blockchain;
import communication.CommLayer;
import communication.data.Data;
import config.Node;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import proto.CommunicationGrpc;
import proto.Types;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static crypto.blockDigSig.verfiyBlockWRTheader;
import static java.lang.String.format;

public class Clique extends CommunicationGrpc.CommunicationImplBase implements CommLayer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Clique.class);
    class Cpeer {
        ManagedChannel channel;
        CommunicationGrpc.CommunicationStub stub;

        public Cpeer(String addr, int port) {
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

    private Server CServer;
    private Map<Integer, Cpeer> peers = new HashMap<>();
    private List<Node> nodes;
    private int id;
    private String addr;
    private int port;
    private int channels;
    private Blockchain[] bcs;
    int n;

    public Clique(int id, String addr, int port, int channels, int n,  ArrayList<Node> nodes) {
        this.id = id;
        this.nodes = nodes;
        this.port = port;
        this.addr = addr;
        this.channels = channels;
        this.n = n;
        new Data(n, channels);
        this.bcs = new Blockchain[channels];
    }

    @Override
    public void join() {
        CServer = ServerBuilder
                .forPort(port)
                .addService(this)
                .maxInboundMessageSize(16 * 1024 * 1024)
                .build();
        try {
            CServer.start();
        } catch (IOException e) {
            logger.error(format("[%d] error while starting server", id), e);
        }
        for (Node n : nodes) {
            peers.put(n.getID(), new Cpeer(n.getAddr(), n.getPort()));
        }

    }

    @Override
    public void leave() {
        CServer.shutdown();
        for (Cpeer p : peers.values()) {
            p.shutdown();
        }
    }

    @Override
    public void registerBC(int channel, Blockchain bc) {
        bcs[channel] = bc;
    }

    @Override
    public void broadcast(int channel, Types.Block data) {
        logger.debug(format("[%d-%d] broadcast block data [bid=%d]", id, channel, data.getId().getBid()));
        for (Cpeer p : peers.values()) {
            p.stub.dsm(Types.Comm.newBuilder()
                    .setChannel(channel)
                    .setData(data)
                    .build(), new StreamObserver<Types.Empty>() {
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

    }



    // Meant to test Byzantine activity

    @Override
    public void send(int channel, Types.Block data, int[] recipients) {
        for (int i : recipients) {
            peers.get(i).stub.dsm(Types.Comm.newBuilder()
                    .setChannel(channel)
                    .setData(data)
                    .build(), new StreamObserver<Types.Empty>() {
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
    }

    @Override
    public void reqBlock(proto.Types.commReq request,
                         io.grpc.stub.StreamObserver<proto.Types.commRes> responseObserver) {
        Types.BlockID bid = Types.BlockID.newBuilder().setBid(request.getProof().getBid())
                .setPid(request.getProof().getM().getSender())
                .build();
        int height = request.getProof().getHeight();
        int channel = request.getProof().getM().getChannel();
        int pid = request.getProof().getM().getSender();
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

    }

    Types.Block getBlockFromData(int channel, Types.BlockID bid, Types.BlockHeader proof) {
        final Types.Block[] res = {null};
        int pid = bid.getPid();
        Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {
            v = v.stream()
                    .filter(b -> verfiyBlockWRTheader(b, proof)).collect(Collectors.toCollection(LinkedList::new));
            res[0] = v.poll();
            return v;
        });
        return res[0];
    }

    @Override
    public Types.Block recBlock(int channel, Types.BlockID bid, Types.BlockHeader proof) throws InterruptedException {
        if (proof.getEmpty()) return Types.Block.getDefaultInstance();
        Types.Block res = getBlockFromData(channel, bid, proof);
        if (res != null) return res;
        broadcastCommReq(Types.commReq.newBuilder().setProof(proof).build());
        int pid = bid.getPid();
        synchronized (Data.blocks[pid][channel]) {
            while (res == null) {
                Data.blocks[pid][channel].wait();
                res = getBlockFromData(channel, bid, proof);
            }
        }
        return res;
    }

    void broadcastCommReq(Types.commReq request) {
        for (Cpeer p : peers.values()) {
            p.stub.reqBlock(request, new StreamObserver<Types.commRes>() {
                @Override
                public void onNext(Types.commRes commRes) {
                    int channel = request.getProof().getM().getChannel();
                    int pid = request.getProof().getM().getSender();
                    Types.BlockID bid = Types.BlockID.newBuilder().setPid(pid)
                            .setBid(request.getProof().getBid()).build();
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
    public boolean contains(int channel, Types.BlockID bid, Types.BlockHeader proof) {
        if (proof.getEmpty()) return true;
        int pid = bid.getPid();
        Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {
            int bef = v.size();
            v = v.stream().filter(b -> verfiyBlockWRTheader(b, proof))
                    .collect(Collectors.toCollection(LinkedList::new));
            logger.debug(format("[%d-%d] invalidate %d records [pid=%d ; bid=%d]", id, channel, bef - v.size(),
                    bid.getPid(), bid.getBid()));
            return v;
        });
        return Data.blocks[pid][channel].containsKey(bid) && Data.blocks[pid][channel].get(bid).size() > 0;
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
        }


    }

}
