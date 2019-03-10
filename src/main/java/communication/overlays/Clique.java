package communication.overlays;

import com.google.protobuf.ByteString;
import communication.CommLayer;
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
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private Queue<Types.Comm>[] msgsQueus;

    public Clique(int id, String addr, int port, int channels, ArrayList<Node> nodes) {
        this.id = id;
        this.nodes = nodes;
        this.port = port;
        this.addr = addr;
        this.channels = channels;
        this.msgsQueus = new Queue[channels];
        for (int i = 0 ; i < channels ; i++) {
            this.msgsQueus[i] = new LinkedList<>();
        }

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
    public void broadcast(int channel, byte[] data) {
        for (Cpeer p : peers.values()) {
            p.stub.dsm(Types.Comm.newBuilder()
                    .setChannel(channel)
                    .setData(ByteString.copyFrom(data))
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
    public byte[] recMsg(int channel) throws InterruptedException {
        synchronized (msgsQueus[channel]) {
            while (msgsQueus[channel].isEmpty()) {
                msgsQueus[channel].wait();
            }
            return Objects.requireNonNull(msgsQueus[channel].poll()).getData().toByteArray();
        }
    }

    @Override
    public void dsm(Types.Comm request, StreamObserver<proto.Types.Empty> responseObserver) {
        int c = request.getChannel();
        synchronized (msgsQueus[c]) {
            msgsQueus[c].add(request);
            msgsQueus[c].notifyAll();
        }

    }

}
