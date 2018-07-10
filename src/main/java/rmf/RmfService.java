package rmf;

import config.Node;
import consensus.bbc.bbcClient;
import consensus.bbc.bbcServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import proto.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
TODO:
1. the recMsg can grow infinitely.
2. Do need to validate the sender identity?? (currently not)
 */
public class RmfService extends RmfGrpc.RmfImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfService.class);
    class vote {
        int ones;
        int consID;

        vote(int consID) {
            this.ones = 0;
            this.consID = consID;
        }
    }

    class peer {
        ManagedChannel channel;
        RmfGrpc.RmfStub stub;

        peer(Node node) {
            channel = ManagedChannelBuilder.
                    forAddress(node.getAddr(), node.getPort()).
                    usePlaintext().
                    build();
            stub = RmfGrpc.newStub(channel);
        }

        void shutdown() {
            try {
                channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

//    protected int currHeight;
    protected int id;
    protected int timeoutMs;
    protected int timeoutInterval;
    int n;
    protected int f;
    protected bbcServer bbcServer;
    protected bbcClient bbcClient;

    protected Semaphore receivedSem;


    final protected Map<Integer, vote> votes;
    final protected Map<Integer, Data> pendingMsg;
    final protected Map<Integer, Data> recMsg;
    protected Map<Integer, peer> peers;
    protected List<Node> nodes;

    protected Server server;

    public RmfService(int id, int f, int tmoInterval, int tmo, ArrayList<Node> nodes, String bbcConfig) {
        this.bbcServer = new bbcServer(id, 2*f + 1, bbcConfig);
        new Thread(this.bbcServer::start).start();
        this.bbcClient = new bbcClient(id, bbcConfig);
        this.receivedSem = new Semaphore(0, true);
        this.votes = new HashMap<>();
        this.pendingMsg = new HashMap<>();
        this.recMsg = new HashMap<>();
        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.timeoutInterval = tmoInterval;
        this.timeoutMs = tmo;
        this.id = id;
        this.nodes = nodes;
    }

    public void start() {
        logger.info("Initiate grpc client [id:" + this.id + "]");
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
    }
    public void shutdown() {

        for (peer p : peers.values()) {
            p.shutdown();
        }
        logger.info("Connections has been shutdown");
        bbcClient.close();
        logger.info("bbc client has been shutdown");
        bbcServer.shutdown();
        logger.info("bbc server has been shutdown");
    }

    // TODO: This should be called by only one process per round.

    protected void sendDataMessage(RmfGrpc.RmfStub stub, Data msg) {
        stub.disseminateMessage(msg, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty ans) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }

    protected void sendFastVoteMessage(RmfGrpc.RmfStub stub, FastBbcVote v) {
        stub.fastVote(v, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty empty) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }

    protected Res sendReqMessage(RmfGrpc.RmfStub stub, Req req, int cHeight) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                // TODO: We should validate the answer
                if (res.getMeta().getHeight() == cHeight && res.getMeta().getSender() == cHeight % n) {
                    synchronized (pendingMsg) {
                        if (!pendingMsg.containsKey(cHeight)) {
                            pendingMsg.put(cHeight, res.getData());
                            receivedSem.release();
                        }
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

        return null;
    }
    protected void broadcastReqMsg(Req req, int height) {
        for (peer p : peers.values()) {
            sendReqMessage(p.stub, req, height);
        }
    }
    protected void broadcastFastVoteMessage(FastBbcVote v) {
        for (peer p : peers.values()) {
            sendFastVoteMessage(p.stub, v);
        }
    }

    public void rmfBroadcast(Data msg) {
        for (peer p : peers.values()) {
            sendDataMessage(p.stub, msg);
        }
    }

    @Override
    public void disseminateMessage(Data request, StreamObserver<Empty> responseObserver) {
        /*
        Currently we assume that only one process send a message per round.
        Hence, if we receive a message we fast vote for it immediately.
         */
        if (request.getMeta().getSender() != request.getMeta().getHeight() % n) {
            return;
        }
        Meta meta = Meta.
                newBuilder().
                setHeight(request.
                        getMeta().
                        getHeight())
                .setSender(id)
                .build();
        FastBbcVote v = FastBbcVote
                .newBuilder()
                .setMeta(meta).setVote(1)
                .build();
        broadcastFastVoteMessage(v);

        synchronized (pendingMsg) {
            pendingMsg.put(request.getMeta().getHeight(), request);
        }
    }

    @Override
    public void fastVote(FastBbcVote request, StreamObserver<Empty> responeObserver) {
        synchronized (votes) {
            int height = request.getMeta().getHeight();
            if (!votes.containsKey(height)) {
                votes.put(height, new vote(height));
            }
            votes.get(height).ones++;
            int nVotes = votes.get(height).ones;
            if (nVotes == n) {
                votes.notify();
            }
        }
    }

    @Override
    public void reqMessage(proto.Req request,  StreamObserver<proto.Res> responseObserver)  {
        Data msg;
        synchronized (recMsg) {
            msg = recMsg.get(request.getMeta().getHeight());
        }
//        recMsgLock.lock();
//        msg = recMsg.get(request.getMeta().getHeight());
//        recMsgLock.unlock();
        if (msg != null) {
            Meta meta = Meta.newBuilder().
                    setSender(id).
                    setHeight(msg.getMeta().getHeight()).
                    build();
            responseObserver.onNext(Res.newBuilder().
                    setData(msg).
                    setMeta(meta).
                    build());
        }
    }

    public byte[] deliver(int height) {
        int fVotes = 0;
        synchronized (votes) {
            if (votes.containsKey(height)) {
                fVotes = votes.get(height).ones;
            }
           if (fVotes < n) {
               try {
                   votes.wait(timeoutMs);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }

       }
       synchronized (votes) {
           if (votes.containsKey(height)) {
               fVotes = votes.get(height).ones;
           }
       }
        if (fVotes < 3*f + 1) {
            int dec = fullBbcConsensus(height);
            if (dec == 0) {
                timeoutMs += timeoutInterval;
                return null;
            }
            handlePositiveDec(height);
        }

        Data msg;
        synchronized (pendingMsg) {
            msg = pendingMsg.get(height);
            pendingMsg.remove(height);
        }

        synchronized (votes) {
            votes.remove(height);
        }
        synchronized (recMsg) {
            recMsg.put(height, msg);
        }
        return msg.getData().toByteArray();
    }

    protected int fullBbcConsensus(int height) {
        int vote = 0;
        synchronized (pendingMsg) {
            if (pendingMsg.containsKey(height)) {
                vote = 1;
            }
        }
        bbcClient.propose(vote, height);
        return bbcServer.decide(height);
    }

    protected void handlePositiveDec(int height) {
        int rec = 0;
        synchronized (pendingMsg) {
            if (pendingMsg.containsKey(height)) {
                rec = 1;
            }
        }
        if (rec == 1) {
            return;
        }
        Meta meta = Meta.
                newBuilder().
                setHeight(height).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req, height);
        try {
            receivedSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
