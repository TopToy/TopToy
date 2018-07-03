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

    static String bbcConfig = Paths.get("config", "rmfbbc").toString();
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

    private int currHeight;
    private int id;
    private int timeoutMs;
    private int timeoutInterval;
    int n;
    private int f;
    private bbcServer bbcServer;
    private bbcClient bbcClient;

//    private Lock pendingsRecLock;
//    private Lock votesLock;
//    private Lock recMsgLock;
//    private Lock heightLock;
    private Semaphore receivedSem;
    final private Object votesEvent;
//    final private Object receiveEvent;


    final private Map<Integer, vote> votes;
    final private Map<Integer, Data> pendingMsg;
    final private Map<Integer, Data> recMsg;
    private Map<Integer, peer> peers;

    private Server server;

    public RmfService(int id, int f, int tmoInterval, int tmo, ArrayList<Node> nodes) {
        this.currHeight = 0;
        this.bbcServer = new bbcServer(id, 2*f + 1, bbcConfig);
        this.bbcClient = new bbcClient(id, bbcConfig);
//        this.pendingsRecLock = new ReentrantLock();
//        this.votesLock = new ReentrantLock();
//        this.recMsgLock = new ReentrantLock();
//        this.heightLock = new ReentrantLock();
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
        this.votesEvent = new Object();
//        this.receiveEvent = new Object();
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
    }

    public void shutdown() {
        for (peer p : peers.values()) {
            p.shutdown();
        }
    }

    // TODO: This should be called by only one process per round.

    private void sendDataMessage(RmfGrpc.RmfStub stub, Data msg) {
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

    private void sendFastVoteMessage(RmfGrpc.RmfStub stub, FastBbcVote v) {
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

    private Res sendReqMessage(RmfGrpc.RmfStub stub, Req req) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                // TODO: We should validate the answer
                int cHeight = 0;
                synchronized ((Object) currHeight) {
                    cHeight = currHeight;
                }
//                heightLock.lock();
//                int cHeight = currHeight;
//                heightLock.unlock();
                if (res.getMeta().getHeight() == cHeight && res.getMeta().getSender() == cHeight % n) {
                    synchronized (pendingMsg) {
                        if (!pendingMsg.containsKey(cHeight)) {
                            pendingMsg.put(cHeight, res.getData());
                            receivedSem.release();
                        }
                    }
//                    pendingsRecLock.lock();
//                    if (!pendingMsg.containsKey(cHeight)) {
//                        pendingMsg.put(cHeight, res.getData());
//                        receivedSem.release();
//                    }
//                    pendingsRecLock.unlock();
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
    private void broadcastReqMsg(Req req) {
        for (peer p : peers.values()) {
            sendReqMessage(p.stub, req);
        }
    }
    private void broadcastFastVoteMessage(FastBbcVote v) {
        for (peer p : peers.values()) {
            sendFastVoteMessage(p.stub, v);
        }
    }

    public void Broadcast(Data msg) {
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
//        pendingsRecLock.lock();
//        pendingMsg.put(request.getMeta().getHeight(), request);
//        pendingsRecLock.unlock();


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
                votesEvent.notify();
            }
        }
//        votesLock.lock();
//        int height = request.getMeta().getHeight();
//        if (!votes.containsKey(height)) {
//            votes.put(height, new vote(height));
//        }
//        votes.get(height).ones++;
//        int nVotes = votes.get(height).ones;
//        votesLock.unlock();
//        if (nVotes == n) {
//            synchronized (votesEvent) {
//                votesEvent.notify();
//            }
//        }
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
       synchronized (votes) {
           if (votes.get(height).ones < n) {
               try {
                   votesEvent.wait(timeoutInterval);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }

       }
       int fVotes = 0;
       synchronized (votes) {
           if (votes.containsKey(height)) {
               fVotes = votes.get(height).ones;
           }
       }
//        votesLock.lock();
//        if (votes.containsKey(height)) {
//            fVotes = votes.get(height).ones;
//        }
//        votesLock.unlock();
        if (fVotes < 3*f + 1) {
            int dec = fullBbcConsensus(height);
            if (dec == 0) {
                timeoutMs += timeoutInterval;
                return null;
            }
            handlePositiveDec(height);
        }
        // We assume that n = 3f + 1
        synchronized ((Object) currHeight) {
           currHeight++;
        }
//        heightLock.lock();
//        currHeight++;
//        heightLock.unlock();

        Data msg;
        synchronized (pendingMsg) {
            msg = pendingMsg.get(height);
            pendingMsg.remove(height);
        }
//        pendingsRecLock.lock();
//        Data msg = pendingMsg.get(height);
//        pendingMsg.remove(height);
//        pendingsRecLock.unlock();

        synchronized (votes) {
            votes.remove(height);
        }
//        votesLock.lock();
//        votes.remove(height);
//        votesLock.unlock();

        synchronized (recMsg) {
            recMsg.put(height, msg);
        }
//        recMsgLock.lock();
//        recMsg.put(height, msg);
//        recMsgLock.unlock();

        return msg.getData().toByteArray();
    }

    private int fullBbcConsensus(int height) {
        int vote = 0;
        synchronized (pendingMsg) {
            if (pendingMsg.containsKey(height)) {
                vote = 1;
            }
        }
//        pendingsRecLock.lock();
//        if (pendingMsg.containsKey(height)) {
//            vote = 1;
//        }
//        pendingsRecLock.unlock();
        bbcClient.propose(vote, height);
        return bbcServer.decide(height);
    }

    private void handlePositiveDec(int height) {
        int rec = 0;
        synchronized (pendingMsg) {
            if (pendingMsg.containsKey(height)) {
                rec = 1;
            }
        }
//        pendingsRecLock.lock();
//        if (pendingMsg.containsKey(height)) {
//            rec = 1;
//        }
//        pendingsRecLock.unlock();
        if (rec == 1) {
            return;
        }
        Meta meta = Meta.
                newBuilder().
                setHeight(height).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req);
        try {
            receivedSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
