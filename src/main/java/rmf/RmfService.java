package rmf;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.protobuf.ByteString;
import config.Node;
//import consensus.bbc.bbcClient;
import consensus.bbc.bbcService;
import crypto.bbcDigSig;
import crypto.pkiUtils;
import crypto.rmfDigSig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import proto.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.lang.String.copyValueOf;
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
        int cidSeries;
        ArrayList<Integer> voters;
        BbcProtos.BbcDecision.Builder dec = BbcProtos.BbcDecision.newBuilder();
    }
    class peer {
        ManagedChannel channel;
        RmfGrpc.RmfStub stub;

        peer(Node node) {
            channel = ManagedChannelBuilder.
                    forAddress(node.getAddr(), node.getRmfPort()).
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
    protected int id;
    int n;
    protected int f;
    protected bbcService bbcService;
    final Object globalLock = new Object();
    final Object bbcLock = new Object();

    // TODO: change to CountDownLatch??
    protected Semaphore receivedSem;
    final protected Table<Integer, Integer, Data> recMsg;

    final Table<Integer, Integer, fvote> fVotes;
    final Table<Integer, Integer, Data> pendingMsg;
    final Table<Integer, Integer, BbcProtos.BbcDecision> fastBbcCons;

    protected Map<Integer, peer> peers;
    protected List<Node> nodes;
    boolean onSync = false;
    final Object syncLock = new Object();
    private Thread bbcServiceThread;
    private Thread bbcMissedConsensusThread;
    protected Server server;
    private String bbcConfig;
    private final Table<Integer, Integer, BbcProtos.BbcDecision> regBbcCons;
    private boolean stopped = false;

    public RmfService(int id, int f, ArrayList<Node> nodes, String bbcConfig) {
        this.bbcConfig = bbcConfig;
        this.receivedSem = new Semaphore(0, true);

        this.fVotes = HashBasedTable.create();
        this.pendingMsg =  HashBasedTable.create();
        this.recMsg =  HashBasedTable.create();

        this.peers = new HashMap<>();
        this.f = f;
        this.n = 3*f +1;
        this.id = id;
        this.nodes = nodes;
        this.fastBbcCons =  HashBasedTable.create();
        this.regBbcCons = HashBasedTable.create();
    }

    public void start() {
        this.bbcService = new bbcService(id, 2*f + 1, bbcConfig);
        CountDownLatch latch = new CountDownLatch(1);
        bbcServiceThread = new Thread(() -> {
                /*
                    Note that in case that there is more than one bbc server this call blocks until all servers are up.
                 */
            this.bbcService.start();
            logger.info(format("[#%d] bbc server is up", id));
            latch.countDown();
        }
        );
        bbcServiceThread.start();
        bbcMissedConsensusThread = new Thread(() ->
        {
            try {
                bbcMissedConsensus();
            } catch (InterruptedException e) {
                logger.error(format("[#%d]", e));
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
            Thread.sleep(5 * 1000); // TODO: Hard code timeout, should it changed? (we probably can do it by notifications)
            synchronized (fastBbcCons) {
                if (fastBbcCons.rowKeySet().isEmpty()) {
                    logger.info(format("[#%d] There are no fast bbc", id));
                    continue;
                }
                int fcidSeries = Collections.max(fastBbcCons.rowKeySet());
                int fcid = Collections.max(fastBbcCons.row(fcidSeries).keySet());
                logger.info(format("[#%d] Trying re-participate [cidSeries=%d ; cid=%d]", id, fcidSeries, fcid));
                bbcService.periodicallyVoteMissingConsensus(fastBbcCons.get(fcidSeries, fcid));
            }

        }
    }

    public void shutdown() {
        stopped = true;
        for (peer p : peers.values()) {
            p.shutdown();
        }
        logger.info(format("[#%d] Connections has been shutdown", id));
        if (bbcService != null) {
            bbcService.shutdown();
        }
        try {
            // TODO: Do we really have to interrupt the threads?
            logger.info(format("[#%d] wait for bbcServiceThread", id));
            if (bbcServiceThread != null) {
                bbcServiceThread.interrupt();
                bbcServiceThread.join();
                logger.info(format("[#%d] wait for bbcMissedConsensusThread", id));
            }
            if (bbcMissedConsensusThread != null) {
                bbcMissedConsensusThread.interrupt();
                bbcMissedConsensusThread.join();
            }
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

    protected void sendFastVoteMessage(RmfGrpc.RmfStub stub, BbcProtos.BbcMsg v) {
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

    protected void sendReqMessage(RmfGrpc.RmfStub stub, Req req, int cidSeries,  int cid, int sender, int height) {
        stub.reqMessage(req, new StreamObserver<Res>() {
            @Override
            public void onNext(Res res) {
                // TODO: We should validate the answer
                if (res.getData().getMeta().getCid() == cid && res.getData().getMeta().getCidSeries() == cidSeries) {
                    synchronized (globalLock) {
                        if (!pendingMsg.contains(cidSeries, cid) && !recMsg.contains(cidSeries, cid)) {
                            if (!rmfDigSig.verify(sender, res.getData())) {
                                logger.info(format("[#%d] has received invalid response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                        id, res.getMeta().getSender(), cidSeries, cid));
                                return;
                            }
                            logger.info(format("[#%d] has received response message from [#%d] for [cidSeries=%d ; cid=%d]",
                                    id, res.getMeta().getSender(), cidSeries,  cid));
                            pendingMsg.put(cidSeries, cid, res.getData());
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
    protected void broadcastReqMsg(Req req, int cidSeries, int cid, int sender, int height) {
        logger.info(format("[#%d] broadcast request message [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
        for (int p : peers.keySet()) {
            if (p == id) continue;
            sendReqMessage(peers.get(p).stub, req, cidSeries, cid, sender, height);
        }
    }
    protected void broadcastFastVoteMessage(BbcProtos.BbcMsg v) {
        for (int p : peers.keySet()) {
//            if (p == id) continue;
            sendFastVoteMessage(peers.get(p).stub, v);
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
        int cidSeries = request.getMeta().getCidSeries();
        synchronized (globalLock) {
            synchronized (fastBbcCons) {
                if (fastBbcCons.contains(cidSeries, cid) || regBbcCons.contains(cidSeries, cid)) return;
            }
            if (pendingMsg.contains(cidSeries, cid) || recMsg.contains(cidSeries, cid)) return;
            pendingMsg.put(cidSeries, cid, request);
            logger.info(format("[#%d] received data message from [%d], [cidSeries=%d ; cid=%d]", id,
                    sender, cidSeries, cid));
//            BbcProtos.BbcMsg.Builder v = BbcProtos.BbcMsg
//                    .newBuilder().setId(cid).setPropserID(id).setVote(1);
//            v.setSig(bbcDigSig.sign(v));
//
//            broadcastFastVoteMessage(v.build());
            globalLock.notify();
        }
    }

//    void localFastVote(BbcProtos.BbcMsg request) {
//        int cid = request.getId();
//        if (regBbcCons.containsKey(cid)) return;
//        if (fastBbcCons.containsKey(cid)) return; // Against byzantine activity
//        if (!fVotes.containsKey(cid)) {
//            fvote v = new fvote();
//            v.dec.setConsID(cid);
//            v.cid = cid;
//            v.count = 0;
//            fVotes.put(cid, v);
//        }
//
//        fVotes.get(cid).count++;
//        fVotes.get(cid).dec.addVotes(request);
//        if (fVotes.get(cid).count == n) {
//            logger.info(format("[#%d] fastVote has been detected [cid=%d]", id, cid));
//            fastBbcCons.put(cid, fVotes.get(cid).dec.setDecosion(1).build());
//            globalLock.notify();
//        }
//
//    }
    @Override
    public void fastVote(BbcProtos.BbcMsg request, StreamObserver<Empty> responeObserver) {
        synchronized (globalLock) {
            int cid = request.getCid();
            int cidSeries = request.getCidSeries();
            if (regBbcCons.contains(cidSeries, cid)) return;
            synchronized (fastBbcCons) {
                if (fastBbcCons.contains(cidSeries, cid)) return; // Against byzantine activity
            }
            if (!fVotes.contains(cidSeries, cid)) {
                fvote v = new fvote();
                v.dec.setCid(cid);
                v.dec.setCidSeries(cidSeries);
                v.cid = cid;
                v.cidSeries = cidSeries;
                v.voters = new ArrayList<>();
                fVotes.put(cidSeries, cid, v);
            }

            if (fVotes.get(cidSeries, cid).voters.contains(request.getPropserID())) {
                logger.info(format("[#%d] received duplicate vote from [%d] for [cidSeries=%d ; cid=%d]",
                        id, request.getPropserID(), cidSeries, cid));
                return;
            }
            fVotes.get(cidSeries, cid).voters.add(request.getPropserID());
            fVotes.get(cidSeries, cid).dec.addVotes(request);
            if (fVotes.get(cidSeries, cid).voters.size() == n) {
                logger.info(format("[#%d] fastVote has been detected [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
                synchronized (fastBbcCons) {
                    fastBbcCons.put(cidSeries, cid, fVotes.get(cidSeries, cid).dec.setDecosion(1).build());
                }
                bbcService.updateFastVote(fVotes.get(cidSeries, cid).dec.build());
                globalLock.notify();
            }
        }
    }
    @Override
    public void reqMessage(proto.Req request,  StreamObserver<proto.Res> responseObserver)  {
        Data msg;
        int cid = request.getMeta().getCid();
        int cidSeries = request.getMeta().getCidSeries();
        synchronized (globalLock) {
                msg = recMsg.get(cidSeries, cid);
            if (msg == null) {
                msg = pendingMsg.get(cidSeries, cid);
            }

        }
        if (msg != null) {
            logger.info(format("[#%d] has received request message from [#%d] of [cidSeries=%d ; cid=%d]",
                    id, request.getMeta().getSender(), cidSeries, cid));
            Meta meta = Meta.newBuilder().
                    setSender(id).
                    setHeight(msg.getMeta().getHeight()).
                    setCid(cid).
                    setCidSeries(cid).
                    build();
            responseObserver.onNext(Res.newBuilder().
                    setData(msg).
                    setMeta(meta).
                    build());
        } else {
            logger.info(format("[#%d] has received request message from [#%d] of [cidSeries=%d ; cid=%d] but buffers are empty",
                    id, request.getMeta().getSender(), cidSeries, cid));
        }
    }


    // TODO: Review this method again
    public Data deliver(int cidSeries, int cid, int tmo, int sender, int height) throws InterruptedException {
//        if (id == 0) {
//            int a = 1;
//        }
//        missedBbcConsensus();
        int cVotes = 0;
        int v = 0;
        synchronized (globalLock) {
            long startTime = System.currentTimeMillis();
            if (!pendingMsg.contains(cidSeries, cid)) {
//                try {
                    globalLock.wait(tmo);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                    return Data.newBuilder().
//                            setMeta(Meta.newBuilder().setCid(-1).build()).
//                            build();
////                    return null;
//                }
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            if (pendingMsg.contains(cidSeries, cid)) {
                BbcProtos.BbcMsg.Builder bv = BbcProtos.BbcMsg
                        .newBuilder().setCid(cid).setCidSeries(cidSeries).setPropserID(id).setVote(1);
                bv.setSig(bbcDigSig.sign(bv));
                broadcastFastVoteMessage(bv.build());
//                localFastVote(bv.build());
            }
            if (fVotes.contains(cidSeries, cid)) {
                cVotes = fVotes.get(cidSeries, cid).voters.size();
            }
            if (cVotes < n) {
//                try {
                    globalLock.wait(Math.max(tmo - estimatedTime, 1));
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                    return Data.newBuilder().
//                            setMeta(Meta.newBuilder().setCid(-1).build()).
//                            build();
////                    return null;
//                }
            }

            if (pendingMsg.contains(cidSeries, cid)) {
                v = 1;
            }
            if (fVotes.contains(cidSeries, cid)) {
                cVotes = fVotes.get(cidSeries, cid).voters.size();
            }

            if (cVotes < n) {
                logger.info(format("[#%d] cvotes is [%d] for [cidSeries=%d ; cid=%d]", id, cVotes,cidSeries, cid));
                int dec = fullBbcConsensus(v, cidSeries, cid);
                logger.info(format("[#%d] bbc returned [%d] for [cidSeries=%d ; cid=%d]", id, dec, cidSeries, cid));
                if (dec == 0) {
                    pendingMsg.remove(cidSeries, cid);
                    return null;
                }
                if (dec == -1) {
                    return Data.newBuilder().
                            setMeta(Meta.newBuilder().setCid(-1).build()).
                            build();
                }
            } else {
                logger.info(format("[#%d] deliver by fast vote [cidSeries=%d ; cid=%d]", id, cidSeries, cid));
            }
            requestData(cidSeries, cid, sender, height);
            Data msg;
                msg = pendingMsg.get(cidSeries, cid);
                recMsg.put(cidSeries, cid, msg);
                pendingMsg.remove(cidSeries, cid);
            return msg;
        }


    }
//    protected void missedBbcConsensus() {
//        ArrayList<Integer> done = new ArrayList<>(fastBbcCons.keySet());
//        ArrayList<Integer> last =  bbcService.getConsensusInstance(done);
//        if (last == null) return;
//        for (int mcid : last) {
//            logger.info(format("[#%d] have found missed bbc [cid=%d]", id, mcid));
//            fullBbcConsensus(fastBbcCons.get(mcid).getDecosion(), mcid);
//        }
//    }
    // TODO: We have a little bug here... note that if a process wish to perform a bbc it doesn't mean that other processes know about it. [solved??]
    protected int fullBbcConsensus(int vote, int cidSeries, int cid) throws InterruptedException {
//        synchronized (bbcLock) {
//        if (regBbcCons.containsKey(cid)) {
//            return regBbcCons.get(cid).getDecosion();
//        }
        logger.info(format("[#%d] Initiates full bbc instance [cidSeries=%d ; cid=%d], [vote:%d]", id, cidSeries, cid, vote));
        bbcService.propose(vote, cidSeries, cid);
        BbcProtos.BbcDecision dec = bbcService.decide(cidSeries, cid);
        if (dec != null) regBbcCons.put(cidSeries, cid, dec);
        return dec != null ? dec.getDecosion() : -1; // TODO: On shutting down null might expected
//        }


    }

    protected void requestData(int cidSeries, int cid, int sender, int height) throws InterruptedException {
        if (pendingMsg.contains(cidSeries, cid)) return;
        Meta meta = Meta.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setSender(id).
                build();
        Req req = Req.newBuilder().setMeta(meta).build();
        broadcastReqMsg(req, cidSeries, cid, sender, height);
        while (!pendingMsg.contains(cidSeries, cid)) {
            globalLock.wait();
        }
    }

    public String getMessageSig(int cidSeries, int cid) {
        if (recMsg.contains(cidSeries, cid)) {
            return recMsg.get(cidSeries, cid).getSig();
        }
        return null;
    }

    public Data nonBlockingDeliver(int cidSeries, int cid) {
        if (recMsg.contains(cidSeries, cid)) {
            return recMsg.get(cidSeries, cid);
        }
        return null;
    }

//    protected void cleanBuffers(int cid) {
//        synchronized (globalLock) {
//            pendingMsg.remove(cid);
//            recMsg.remove(cid);
//            fVotes.remove(cid);
//            synchronized (bbcLock) {
//                regBbcCons.remove(cid);
//                fastBbcCons.remove(cid);
//            }
//            bbcService.cleanBuffers(cid);
//        }
//
//    }
}
