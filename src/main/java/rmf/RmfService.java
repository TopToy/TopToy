package rmf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import config.Node;
import consensus.bbc.bbcClient;
import consensus.bbc.bbcServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.omg.PortableInterceptor.INACTIVE;
import proto.*;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.lang.String.format;

/*
TODO:
1. the recMsg can grow infinitely.
2. Do need to validate the sender identity?? (currently not)
3. When to reset the timer
4. prevBbcConsensus can grow infinitely (and some more, analyze it!!)
5. change maps to list
 */
public class RmfService extends RmfGrpc.RmfImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfService.class);
//    class vote {
//        int ones;
//        int consID;
//
//        vote(int consID) {
//            this.ones = 0;
//            this.consID = consID;
//        }
//    }

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
    int consID = 0;
    protected int id;
    protected int timeoutMs;
    int initTO;
    protected int timeoutInterval;
    int n;
    protected int f;
    protected bbcServer bbcServer;
    protected bbcClient bbcClient;
    final Object globalLock = new Object();

    // TODO: change to CountDownLatch??
    protected Semaphore receivedSem;


//    final protected Map<Integer, vote> votes;
//    final protected Map<Integer, Data> pendingMsg;
    final protected Map<Integer, Data> recMsg;

    final HashMap<Integer, Integer> fVotes;
    final Table<Integer, Integer, Data> pendingMsg;
    final HashMap<Integer,  BbcProtos.BbcDecision> fastBbcCons;
//    final Table<Integer, Integer, Data> recMsg;

    protected Map<Integer, peer> peers;
    protected List<Node> nodes;

    private Thread bbcServerThread;
    private Thread bbcMissedConsensusThread;
    protected Server server;
    private String bbcConfig;
    private final HashMap<Integer, BbcProtos.BbcDecision> regBbcCons;
    private boolean stopped = false;
//    private final Object dataMsgLock = new Object();
//    private final Object wayToRecMsgLock = new Object();

    public RmfService(int id, int f, int tmoInterval, int tmo, ArrayList<Node> nodes, String bbcConfig) {
        this.bbcConfig = bbcConfig;
        this.bbcClient = new bbcClient(id, bbcConfig);
        this.receivedSem = new Semaphore(0, true);

        this.fVotes = new HashMap<>();
        this.pendingMsg = HashBasedTable.create(); //new HashMap<>();
        this.recMsg = new HashMap<>();

        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.timeoutInterval = tmoInterval;
        this.initTO = tmo;
        this.timeoutMs = tmo;
        this.id = id;
        this.nodes = nodes;
//        this.prevBbcCons = new HashMap<>();
//        this.fastVoteDone = new ArrayList<>();
        this.fastBbcCons = new HashMap<>();
        this.regBbcCons = new HashMap<>();
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
        bbcMissedConsensusThread = new Thread(() ->
        {
            try {
                bbcMissedConsensus();
            } catch (InterruptedException e) {
                logger.warn("", e);
            }
        });
        bbcMissedConsensusThread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("", e);
        }
//        bbcServerThread.start();
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
        logger.info(format("[#%d] Initiated grpc client", id));
    }
//    // TODO: Bug alert!!
    private void bbcMissedConsensus() throws InterruptedException {
        while (!stopped) {
            ArrayList<Integer> done;
//            synchronized (fastVoteDone) {
            synchronized (globalLock) {
                done = new ArrayList<>(fastBbcCons.keySet());
            }
            ArrayList<Integer> last = bbcServer.notifyOnConsensusInstance(done);
            for (int cid : last) {
                synchronized (globalLock) {
                    logger.info(format("[#%d] have found missed bbc [cid=%d]", id, cid));
                    fullBbcConsensus(fastBbcCons.get(cid).getDecosion(), cid);
                }
            }
        }
    }

////            Thread.sleep(timeoutMs); // Sleep for a moment to ensure that there is enough time.
////            synchronized (prevBbcCons) {
////                last.removeAll(new ArrayList<>(prevBbcCons.keySet()));
////            }
//            for(Integer height : last) {
//                int vote;
//                synchronized (dataMsgLock) {
//                    if (recMsg.containsKey(height)) {
//                        vote = 1;
//                    } else {
//                        continue;
//                    }
//                }
//                logger.info(format("[#%d] has found missed bbc consensus [height=%d]", id, height));
//                fullBbcConsensus(vote, height);
//            }
//        }
//        logger.info(format("[#%d] bbcMissedConsensus shutdown", id));
//    }
    public void shutdown() {
        stopped = true;
        for (peer p : peers.values()) {
            p.shutdown();
        }
        logger.info(format("[#%d] Connections has been shutdown", id));
        bbcClient.close();
        logger.info(format("[#%d] bbc client has been shutdown", id));
        bbcServer.shutdown();
        bbcMissedConsensusThread.interrupt();
        try {
            // TODO: Do we really have to interrupt the threads?
//            bbcServerThread.interrupt();
            logger.info(format("[#%d] wait for bbcServerThread", id));
            bbcServerThread.join();
            logger.info(format("[#%d] wait for bbcMissedConsensusThread", id));
//            bbcMissedConsensusThread.interrupt();
            bbcMissedConsensusThread.join();
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
//                    synchronized (pendingMsg) {
//                        synchronized (recMsg){
                    synchronized (globalLock) {
                            if (!pendingMsg.contains(cHeight, sender) && !recMsg.containsKey(cHeight)) {
                                //(!pendingMsg.containsKey(cHeight) && !recMsg.containsKey(cHeight)) {
                                logger.info(format("[#%d] has received response message from [#%d] of [height=%d, origSender:%d]",
                                        id, sender, res.getData().getMeta().getHeight(),  res.getData().getMeta().getSender()));
                                pendingMsg.put(cHeight, sender, res.getData());
                                globalLock.notify();
                            }
                        }
//                    }
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
        logger.info(format("[#%d] broadcast request message [height=%d]", id, height));
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
//        if (request.getMeta().getSender() != request.getMeta().getHeight() % n) {
//            return;
//        }


        int height = request.getMeta().getHeight();
        int sender = request.getMeta().getSender();

//        synchronized (prevBbcCons) {
        synchronized (globalLock) {
//            if (votes.get(height, sender) == -1) return;
//            if (prevBbcCons.get(height) != null && prevBbcCons.get(height).getDecosion() == 0) return;
        // When a new message arrives the server put it into the arrival and nothing more!!
//        synchronized (pendingMsg) {
//            synchronized (recMsg) {
                if (pendingMsg.contains(height, sender) || recMsg.containsKey(height)) return;
                pendingMsg.put(height, sender, request);
                logger.info(format("[#%d] received data message from [%d], [height=%d]", id,
                        sender, height));
                timeoutMs = initTO;
//                Meta meta = Meta.
//                        newBuilder().
//                        setHeight(height)
//                        .setSender(id)
//                        .build();
                FastBbcVote v = FastBbcVote
                        .newBuilder().setCid(consID).setSender(id).setVote(1).build();
//                        .setMeta(meta).setVote(fastVoteMsg.
//                                newBuilder().
//                                setHeight(height).
//                                setSender(sender)
//                                .build())
//                        .build();
                broadcastFastVoteMessage(v);
            }
//        }

//        synchronized (wayToRecMsgLock) {
//            synchronized (dataMsgLock) {
//                if (pendingMsg.containsKey(height) || recMsg.containsKey(height)) return;
//                pendingMsg.put(height, request);
//            }
//            synchronized (prevBbcCons) {
//                if (prevBbcCons.containsKey(height)) return;
//            }
//            logger.info(format("[#%d] received data message from [%d], [height=%d]", id,
//                    request.getMeta().getSender(), height));
//            timeoutMs = initTO;
//            Meta meta = Meta.
//                    newBuilder().
//                    setHeight(height)
//                    .setSender(id)
//                    .build();
//            FastBbcVote v = FastBbcVote
//                    .newBuilder()
//                    .setMeta(meta).setVote(1)
//                    .build();
//            broadcastFastVoteMessage(v);
//        }

    }

    @Override
    public void fastVote(FastBbcVote request, StreamObserver<Empty> responeObserver) {
//        synchronized (votes) {
        synchronized (globalLock) {
//            int height = request.getVote().getHeight();
//            int sender = request.getVote().getSender();
            int cid = request.getCid();
            if (regBbcCons.containsKey(cid)) return;
            if (fastBbcCons.containsKey(cid)) return; // Against byzantine activity
            if (!fVotes.containsKey(cid)) {
                fVotes.put(cid, 0);
            }
//            if (votes.get(height, sender) == -1) return;
            int curr = fVotes.get(cid);
            fVotes.remove(cid);
            fVotes.put(cid, curr + 1);
            if (fVotes.get(cid) == n) {
                logger.info(format("[#%d] fastVote has been detected [cid=%d]", id, cid));
//                synchronized (fastVoteDone) {
                    fastBbcCons.put(cid, BbcProtos.BbcDecision.newBuilder().setConsID(cid).setDecosion(1).build());
//                prevBbcCons.put(height, BbcProtos.BbcDecision.newBuilder().setConsID(height).setDecosion(1).build());
//                }
                globalLock.notify();
            }
        }
    }
//    @Override
//    public void fastVote(FastBbcVote request, StreamObserver<Empty> responeObserver) {
//        synchronized (votes) {
//            int height = request.getMeta().getHeight();
//            if (!votes.containsKey(height)) {
//                votes.put(height, new vote(height));
//            }
//            votes.get(height).ones++;
//            int nVotes = votes.get(height).ones;
//            if (nVotes == n) {
//                logger.info(format("[#%d] fastVote has been detected", id));
//                votes.notify();
//            }
//        }
//    }

    @Override
    public void reqMessage(proto.Req request,  StreamObserver<proto.Res> responseObserver)  {
        Data msg;
        int height = request.getMeta().getHeight();
        int sender = request.getMeta().getSender();
//        synchronized (pendingMsg) {
//            synchronized (recMsg) {
        synchronized (globalLock) {
                msg = recMsg.get(height);
//            }
            if (msg == null) {
                msg = pendingMsg.get(height, sender);
            }

        }
        if (msg != null) {
            logger.info(format("[#%d] has received request message from [#%d] of [height=%d]",
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

//    void tryFastVote(int height, int sender) {
//        Data msg = null;
//        msg = pendingMsg.get(height, sender);
//        if (msg != null) {
//
//        }
//    }

    // TODO: Review this method again
    public byte[] deliver(int height, int sender) {
        int cVotes = 0;
//        synchronized (votes) {
        synchronized (globalLock) {
            if (fVotes.containsKey(consID)) {
                cVotes = fVotes.get(consID);
            }
            if (cVotes < n) {
                try {
                    globalLock.wait(timeoutMs);
                } catch (InterruptedException e) {
                    logger.error("", e);
                    return null;
                }
            }

            if (fVotes.containsKey(consID)) {
                cVotes = fVotes.get(consID);
//                votes.remove(height, sender);
//                votes.put(height, sender, -1);
            }

//        }

            if (cVotes < n) {
                int vote = 0;
//            synchronized (pendingMsg) {
                if (pendingMsg.contains(height, sender)) {
                    vote = 1;
                }
//            }
                int dec = fullBbcConsensus(vote, consID);
                logger.info(format("[#%d] bbc returned [%d] for [cid=%d]", id, dec, consID));
//                consID++;
                if (dec == 0) {
                    consID++;
                    timeoutMs += timeoutInterval;
                    logger.info(format("[#%d] timeout increased to %d", id, timeoutMs));
                    return null;
                }

            } else {
                logger.info(format("[#%d] deliver by fast vote [height=%d ; cid=%d]", id, height, consID));
            }
            consID++;
            requestData(height, sender);
            Data msg;
//       synchronized (pendingMsg) {
//           synchronized (recMsg) {
                msg = pendingMsg.get(height, sender);
                recMsg.put(height, msg);
                Set<Integer> senders = pendingMsg.row(height).keySet();
                for (int k : senders) {
                    pendingMsg.remove(height, k);
                }
//           }
            return msg.getData().toByteArray();
        }
//       synchronized (votes) {
//           votes.remove(height, sender);
//       }


    }
    // TODO: We have a little bug here... note that if a process wish to perform a bbc it doesn't mean that other processes know about it. [solved??]
    protected int fullBbcConsensus(int vote, int cid) {
//        synchronized (prevBbcCons) {
            if (regBbcCons.containsKey(cid)) {
                return regBbcCons.get(cid).getDecosion();
            }
            logger.info(format("[#%d] Initiates full bbc instance [cid=%d], [vote:%d]", id, cid, vote));
            bbcClient.propose(vote, cid);
            int dec = bbcServer.decide(cid);
            regBbcCons.put(cid, BbcProtos.BbcDecision.newBuilder().setConsID(cid).setDecosion(dec).build());
            return dec;
//        }
    }

    protected void requestData(int height, int sender) {
        if (pendingMsg.contains(height, sender)) return;
        Meta meta = Meta.
                newBuilder().
                setHeight(height).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req, height, sender);
        while (!pendingMsg.contains(height, sender)) {
            try {
                globalLock.wait();
            } catch (InterruptedException e) {
                logger.error("", e);
                return;
            }
        }
//        try {
//            receivedSem.acquire();
//        } catch (InterruptedException e) {
//            logger.error("", e);
//        }
    }
}
