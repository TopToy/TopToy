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
    public Types.Block recMsg(int channel, Types.BlockID bid) throws InterruptedException {
        synchronized (GlobalData.blocks[channel]) {
            while (!GlobalData.blocks[channel].containsKey(bid) || GlobalData.blocks[channel].get(bid).isEmpty()) {
                GlobalData.blocks[channel].wait();
            }
            return Objects.requireNonNull(GlobalData.blocks[channel].get(bid).poll());
        }
    }

    @Override
    public boolean contains(int channel, Types.BlockID bid, Types.BlockHeader proof) {
        return GlobalData.blocks[channel].containsKey(bid)
                && GlobalData.blocks[channel].get(bid).stream().filter(b -> verfiyBlockWRTheader(b, proof)).count() > 0;
    }

    @Override
    public void dsm(Types.Comm request, StreamObserver<proto.Types.Empty> responseObserver) {
        int c = request.getChannel();
        synchronized (GlobalData.blocks[c]) {
            GlobalData.blocks[c].putIfAbsent(request.getData().getId(), new LinkedList<Types.Block>());
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
