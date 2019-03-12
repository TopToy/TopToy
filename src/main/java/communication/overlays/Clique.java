package communication.overlays;

import communication.CommLayer;
import communication.data.GlobalData;
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

    public Clique(int id, String addr, int port, int channels, ArrayList<Node> nodes) {
        this.id = id;
        this.nodes = nodes;
        this.port = port;
        this.addr = addr;
        this.channels = channels;
        new GlobalData(channels);
    }

    @Override
    public void join() {
        CServer = ServerBuilder
                .forPort(port)
                .addService(this)
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

    @Override
    public void reqBlock(proto.Types.commReq request,
                         io.grpc.stub.StreamObserver<proto.Types.commRes> responseObserver) {
        Types.BlockID bid = Types.BlockID.newBuilder().setBid(request.getProof().getBid())
                .setPid(request.getProof().getM().getSender())
                .build();
        int channel = request.getProof().getM().getChannel();
        GlobalData.blocks[channel].computeIfPresent(bid, (k, v) -> {
            Types.Block res = null;
            LinkedList<Types.Block> ll = v.stream().filter(b -> verfiyBlockWRTheader(b, request.getProof()))
                    .collect(Collectors.toCollection(LinkedList::new));

            res = ll.peek();
            if (res != null) {
                Types.commRes cr = Types.commRes.newBuilder().setB(res).build();
                responseObserver.onNext(cr);
            }
            return v;
        });

    }

    Types.Block getBlockFromData(int channel, Types.BlockID bid, Types.BlockHeader proof) {
        final Types.Block[] res = {null};
        GlobalData.blocks[channel].computeIfPresent(bid, (k, v) -> {
            v = v.stream()
                    .filter(b -> verfiyBlockWRTheader(b, proof)).collect(Collectors.toCollection(LinkedList::new));
            res[0] = v.poll();
            return v;
        });
        return res[0];
    }

    @Override
    public Types.Block recBlock(int channel, Types.BlockID bid, Types.BlockHeader proof) throws InterruptedException {
        Types.Block res = getBlockFromData(channel, bid, proof);
        if (res != null) return res;
        broadcastCommReq(Types.commReq.newBuilder().setProof(proof).build());
        synchronized (GlobalData.blocks[channel]) {
            while (res == null) {
                GlobalData.blocks[channel].wait();
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
                    Types.BlockID bid = Types.BlockID.newBuilder().setPid(request.getProof().getM().getSender())
                            .setBid(request.getProof().getBid()).build();
                    if (verfiyBlockWRTheader(commRes.getB(), request.getProof())) {
                        synchronized (GlobalData.blocks[channel]) {
                            GlobalData.blocks[channel].putIfAbsent(bid, new LinkedList<>());
                            GlobalData.blocks[channel].computeIfPresent(bid, (k, v) -> {
                                v.add(commRes.getB());
                                return v;
                            });
                            GlobalData.blocks[channel].notifyAll();
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
        GlobalData.blocks[channel].computeIfPresent(bid, (k, v) -> v.stream().filter(b -> verfiyBlockWRTheader(b, proof))
                .collect(Collectors.toCollection(LinkedList::new)));
        return GlobalData.blocks[channel].get(bid).size() > 0;
    }

    @Override
    public void dsm(Types.Comm request, StreamObserver<proto.Types.Empty> responseObserver) {
        int c = request.getChannel();
        GlobalData.blocks[c].putIfAbsent(request.getData().getId(), new LinkedList<Types.Block>());
        synchronized (GlobalData.blocks[c]) {
            GlobalData.blocks[c].computeIfPresent(request.getData().getId(), (k, v) -> {
                Types.BlockID bid = request.getData().getId();
                logger.debug(format("[%d-%d] received block [pid=%d ; bid=%d]", id, c, bid.getPid(), bid.getBid()));
                v.add(request.getData());
                return v;
            });
            GlobalData.blocks[c].notifyAll();
        }


    }

}
