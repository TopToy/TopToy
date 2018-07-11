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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

/*
TODO:
1. the recMsg can grow infinitely.
2. Do need to validate the sender identity?? (currently not)
3. When to reset the timer
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
                logger.error("", e);
            }
        }
    }

//    protected int currHeight;
    protected int id;
    protected int timeoutMs;
    int initTO;
    protected int timeoutInterval;
    int n;
    protected int f;
    protected bbcServer bbcServer;
    protected bbcClient bbcClient;

    // TODO: change to CountDownLatch??
    protected Semaphore receivedSem;


    final protected Map<Integer, vote> votes;
    final protected Map<Integer, Data> pendingMsg;
    final protected Map<Integer, Data> recMsg;
    protected Map<Integer, peer> peers;
    protected List<Node> nodes;
    private Thread bbcServerThread;
    protected Server server;
    private String bbcConfig;

    public RmfService(int id, int f, int tmoInterval, int tmo, ArrayList<Node> nodes, String bbcConfig) {
        this.bbcConfig = bbcConfig;
        this.bbcClient = new bbcClient(id, bbcConfig);
        this.receivedSem = new Semaphore(0, true);
        this.votes = new HashMap<>();
        this.pendingMsg = new HashMap<>();
        this.recMsg = new HashMap<>();
        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.timeoutInterval = tmoInterval;
        this.initTO = tmo;
        this.timeoutMs = tmo;
        this.id = id;
        this.nodes = nodes;
    }

    public void start() {
        this.bbcServer = new bbcServer(id, 2*f + 1, bbcConfig);
        CountDownLatch latch = new CountDownLatch(1);
        bbcServerThread = new Thread(() -> {
                /*
                    Note that in case that there is more than one bbc server this call blocks until all servers are up.
                 */
            this.bbcServer.start();
            logger.info(format("[#%d] bbc server is up", id));
            latch.countDown();
        }
        );
        bbcServerThread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        logger.info(format("[#%d] Initiate grpc client", id));
//        bbcServerThread.start();
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
    }
    public void shutdown() {
        for (peer p : peers.values()) {
            p.shutdown();
        }
        logger.info(format("[#%d] Connections has been shutdown", id));
        bbcClient.close();
        logger.info(format("[#%d] bbc client has been shutdown", id));
        bbcServer.shutdown();
        try {
            bbcServerThread.join();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
        logger.info(format("[#%d] bbc server has been shutdown", id));
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
    /*
     TODO: currently it is possible that after the message was pulled from pending another server sends Res message
            and insert it again. We will probably handle it by adding some cleanup mechanism.
      */

    protected void sendReqMessage(RmfGrpc.RmfStub stub, Req req, int cHeight, int sender) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                // TODO: We should validate the answer
                if (res.getData().getMeta().getHeight() == cHeight && res.getData().getMeta().getSender() == sender) {
                    synchronized (pendingMsg) {
                        synchronized (recMsg) {
                            if (!pendingMsg.containsKey(cHeight) && !recMsg.containsKey(cHeight)) {
                                logger.info(format("[#%d] has received response message from [#%d] of [height:%d, orig:%d]",
                                        id, res.getMeta().getSender(), sender, res.getMeta().getHeight()));
                                pendingMsg.put(cHeight, res.getData());
                                receivedSem.release();
                            }
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
    }
    protected void broadcastReqMsg(Req req, int height, int sender) {
        logger.info(format("[#%d] broadcast request message [height:%d]", id, height));
        for (peer p : peers.values()) {
            sendReqMessage(p.stub, req, height, sender);
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
        synchronized (pendingMsg) {
            pendingMsg.put(request.getMeta().getHeight(), request);
        }
        logger.info(format("[#%d] received data message from [%d], [height:%d]", id,
                request.getMeta().getSender(), request.getMeta().getHeight()));
        timeoutMs = initTO;
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
                logger.info(format("[#%d] fast vote has been detected", id));
                votes.notify();
            }
        }
    }

    @Override
    public void reqMessage(proto.Req request,  StreamObserver<proto.Res> responseObserver)  {
        Data msg;
        synchronized (pendingMsg) {
            synchronized (recMsg) {
                msg = recMsg.get(request.getMeta().getHeight());
                if (msg == null) {
                    msg = pendingMsg.get(request.getMeta().getHeight());
                }
            }

        }
        if (msg != null) {
            logger.info(format("[#%d] has received request message from [#%d] of [height:%d]",
                    id, request.getMeta().getSender(), request.getMeta().getHeight()));
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
    // TODO: Review this method again
    public byte[] deliver(int height, int sender) {
        int fVotes = 0;
        synchronized (votes) {
            if (votes.containsKey(height)) {
                fVotes = votes.get(height).ones;
            }
           if (fVotes < n) {
               try {
                   votes.wait(timeoutMs);
               } catch (InterruptedException e) {
                   logger.error("", e);
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
            logger.info(format("[#%d] bbc returned [%d] for [height: %d]", id, dec, height));
            if (dec == 0) {
                timeoutMs += timeoutInterval;
                logger.info(format("[#%d] timeout increased to %d", id, timeoutMs));
                return null;
            }
            handlePositiveDec(height, sender);
        }

        Data msg;
        synchronized (pendingMsg) {
            synchronized (recMsg) {
                msg = pendingMsg.get(height);
                recMsg.put(height, msg);
                pendingMsg.remove(height);
            }
        }

        synchronized (votes) {
            votes.remove(height);
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
        logger.info(format("[#%d] Initiates full bbc instance [height:%d], [vote:%d]", id, height, vote));
        bbcClient.propose(vote, height);
        return bbcServer.decide(height);
    }

    protected void handlePositiveDec(int height, int sender) {
        synchronized (pendingMsg) {
            if (pendingMsg.containsKey(height)) {
                return;
            }
        }
        Meta meta = Meta.
                newBuilder().
                setHeight(height).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req, height, sender);
        try {
            receivedSem.acquire();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
    }
}
