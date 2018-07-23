package rmf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import config.Node;
import consensus.bbc.bbcClient;
import consensus.bbc.bbcServer;
import crypto.pkiUtils;
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
6. Handle the case in which byzantine node sends its message twice
 */
public class RmfService extends RmfGrpc.RmfImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfService.class);
    class fvote {
        int cid;
        int count;
        BbcProtos.BbcDecision.Builder dec = BbcProtos.BbcDecision.newBuilder();
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
//    int consID = 0;
    protected int id;
//    protected int timeoutMs;
//    int initTO;
//    protected int timeoutInterval;
    int n;
    protected int f;
    protected bbcServer bbcServer;
    protected bbcClient bbcClient;
    final Object globalLock = new Object();
    final Object bbcLock = new Object();

    // TODO: change to CountDownLatch??
    protected Semaphore receivedSem;
    final protected Map<Integer, Data> recMsg;

    final HashMap<Integer, fvote> fVotes;
    final HashMap<Integer, Data> pendingMsg;
    final HashMap<Integer,  BbcProtos.BbcDecision> fastBbcCons;

    protected Map<Integer, peer> peers;
    protected List<Node> nodes;

    private Thread bbcServerThread;
    private Thread bbcMissedConsensusThread;
    protected Server server;
    private String bbcConfig;
    private final HashMap<Integer, BbcProtos.BbcDecision> regBbcCons;
    private boolean stopped = false;

    public RmfService(int id, int f, ArrayList<Node> nodes, String bbcConfig) {
        this.bbcConfig = bbcConfig;
        this.bbcClient = new bbcClient(id, bbcConfig);
        this.receivedSem = new Semaphore(0, true);

        this.fVotes = new HashMap<>();
        this.pendingMsg = new HashMap<>(); //HashBasedTable.create(); //new HashMap<>();
        this.recMsg = new HashMap<>();

        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
//        this.timeoutInterval = tmoInterval;
//        this.initTO = tmo;
//        this.timeoutMs = tmo;
        this.id = id;
        this.nodes = nodes;
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
        for (Node n : nodes) {
            peers.put(n.getID(), new peer(n));
        }
        logger.info(format("[#%d] Initiated grpc client", id));
    }
//    // TODO: Bug alert!!
    private void bbcMissedConsensus() throws InterruptedException {
        while (!stopped) {
            ArrayList<Integer> done;
            synchronized (globalLock) {
                done = new ArrayList<>(fastBbcCons.keySet());
            }
            ArrayList<Integer> last = bbcServer.notifyOnConsensusInstance(done);
            for (int cid : last) {
//                synchronized (globalLock) {
                    logger.info(format("[#%d] have found missed bbc [cid=%d]", id, cid));
                    fullBbcConsensus(fastBbcCons.get(cid).getDecosion(), cid);
                }
//            }
        }
    }

    public void shutdown() {
        stopped = true;
        for (peer p : peers.values()) {
            p.shutdown();
        }
        logger.info(format("[#%d] Connections has been shutdown", id));
        bbcClient.close();
        logger.info(format("[#%d] bbc client has been shutdown", id));
        bbcServer.shutdown();
        try {
            // TODO: Do we really have to interrupt the threads?
            logger.info(format("[#%d] wait for bbcServerThread", id));
            bbcServerThread.interrupt();
            bbcServerThread.join();
            logger.info(format("[#%d] wait for bbcMissedConsensusThread", id));
            bbcMissedConsensusThread.interrupt();
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

    protected void sendReqMessage(RmfGrpc.RmfStub stub, Req req, int cid, int sender, int height) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                // TODO: We should validate the answer
                if (res.getData().getMeta().getCid() == cid) {
                    int cid = res.getMeta().getCid();
                    synchronized (globalLock) {
                        if (!pendingMsg.containsKey(cid) && !recMsg.containsKey(cid)) {
                            if (!pkiUtils.verify(sender,
                                    String.valueOf(cid) +
                            String.valueOf(sender) + String.valueOf(height) + new String(res.getData().getData().toByteArray()),
                                    res.getData().getSig())) {
                                logger.info(format("[#%d] has received invalid response message from [#%d] for [cid=%d]",
                                        id, res.getMeta().getSender(),  cid));
                            }
                            logger.info(format("[#%d] has received response message from [#%d] for [cid=%d]",
                                    id, res.getMeta().getSender(),  cid));
                            pendingMsg.put(cid, res.getData());
                            globalLock.notify();
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
    protected void broadcastReqMsg(Req req, int cid, int sender, int height) {
        logger.info(format("[#%d] broadcast request message [cid=%d]", id, cid));
        for (peer p : peers.values()) {
            sendReqMessage(p.stub, req, cid, sender, height);
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
        int height = request.getMeta().getHeight();
        int sender = request.getMeta().getSender();
        int cid = request.getMeta().getCid();
        synchronized (globalLock) {
            if (fastBbcCons.containsKey(cid) || regBbcCons.containsKey(cid)) return;
            if (pendingMsg.containsKey(cid) || recMsg.containsKey(cid)) return;
            pendingMsg.put(cid, request);
            logger.info(format("[#%d] received data message from [%d], [cid=%d]", id,
                    sender, cid));
//            timeoutMs = initTO;
            FastBbcVote v = FastBbcVote
                    .newBuilder().setCid(cid).setSender(id).setVote(1).
                            setSig(String.valueOf(cid) + String.valueOf(id) + String.valueOf(1)).build();
            broadcastFastVoteMessage(v);
        }
    }

    @Override
    public void fastVote(FastBbcVote request, StreamObserver<Empty> responeObserver) {
        synchronized (globalLock) {
            int cid = request.getCid();
//            logger.info(format("[#%d] received fast vote from [%d] in [cid=%d]", id, request.getSender(), cid));
            if (regBbcCons.containsKey(cid)) return;
            if (fastBbcCons.containsKey(cid)) return; // Against byzantine activity
//            synchronized (fVotes) {
            if (!fVotes.containsKey(cid)) {
                fvote v = new fvote();
                v.dec.setConsID(cid);
                v.cid = cid;
                v.count = 0;
                fVotes.put(cid, v);
//                    fVotes.notify(); // To prevent the case in which a process have received a message but did not vote for it yet.
            }
//            }
//            int curr = fVotes.get(cid);
//            fVotes.remove(cid);
//            fVotes.put(cid, curr + 1);
            fVotes.get(cid).count++;
            fVotes.get(cid).dec.addSignatures(request.getSig());
            if (fVotes.get(cid).count == n) {
                logger.info(format("[#%d] fastVote has been detected [cid=%d]", id, cid));
                    fastBbcCons.put(cid, fVotes.get(cid).dec.setDecosion(1).build());
                globalLock.notify();
            }
        }
    }
    @Override
    public void reqMessage(proto.Req request,  StreamObserver<proto.Res> responseObserver)  {
        Data msg;
        int cid = request.getMeta().getCid();
        synchronized (globalLock) {
                msg = recMsg.get(cid);
            if (msg == null) {
                msg = pendingMsg.get(cid);
            }

        }
        if (msg != null) {
            logger.info(format("[#%d] has received request message from [#%d] of [cid=%d]",
                    id, request.getMeta().getSender(), cid));
            Meta meta = Meta.newBuilder().
                    setSender(id).
                    setHeight(msg.getMeta().getHeight()).
                    setCid(cid).
                    build();
            responseObserver.onNext(Res.newBuilder().
                    setData(msg).
                    setMeta(meta).
                    build());
        }
    }
    // TODO: Review this method again
    public byte[] deliver(int cid, int tmo, int sender, int height) {
        int cVotes = 0;
        int v = 0;
        synchronized (globalLock) {
            if (fVotes.containsKey(cid)) {
                cVotes = fVotes.get(cid).count;
            }
            if (cVotes < n) {
                try {
                    globalLock.wait(tmo);
                } catch (InterruptedException e) {
                    logger.error("", e);
                    return null;
                }
            }

            if (pendingMsg.containsKey(cid)) {
                v = 1;
            }
            if (fVotes.containsKey(cid)) {
                cVotes = fVotes.get(cid).count;
            }

            if (cVotes < n) {
                int dec = fullBbcConsensus(v, cid);
                logger.info(format("[#%d] bbc returned [%d] for [cid=%d]", id, dec, cid));
                if (dec == 0) {
//                    timeoutMs += timeoutInterval;
//                    logger.info(format("[#%d] timeout increased to %d", id, timeoutMs));
                    pendingMsg.remove(cid);
                    return null;
                }

            } else {
                logger.info(format("[#%d] deliver by fast vote [cid=%d]", id, cid));
            }
            requestData(cid, sender, height);
            Data msg;
                msg = pendingMsg.get(cid);
                recMsg.put(cid, msg);
                pendingMsg.remove(cid);
            return msg.getData().toByteArray();
        }


    }
    // TODO: We have a little bug here... note that if a process wish to perform a bbc it doesn't mean that other processes know about it. [solved??]
    protected int fullBbcConsensus(int vote, int cid) {
        synchronized (bbcLock) {
            if (regBbcCons.containsKey(cid)) {
                return regBbcCons.get(cid).getDecosion();
            }
//            int vote = 0;
//            if (pendingMsg.containsKey(cid) || recMsg.containsKey(cid)) {
//                vote = 1;
//            }
            logger.info(format("[#%d] Initiates full bbc instance [cid=%d], [vote:%d]", id, cid, vote));
            bbcClient.propose(vote, cid);
            BbcProtos.BbcDecision dec = bbcServer.decide(cid);
            regBbcCons.put(cid, dec);
            return dec.getDecosion();
        }


    }

    protected void requestData(int cid, int sender, int height) {
        if (pendingMsg.containsKey(cid)) return;
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req, cid, sender, height);
        while (!pendingMsg.containsKey(cid)) {
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
