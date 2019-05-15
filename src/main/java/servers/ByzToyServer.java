package servers;

import blockchain.Blockchain;

import blockchain.Utils;
import blockchain.data.BCS;
import com.google.protobuf.ByteString;
import communication.CommLayer;
import das.ab.ABService;

import das.wrb.WRB;
import proto.Types;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static blockchain.Utils.createBlockHeader;
import static blockchain.Utils.createBlockchain;
import static java.lang.String.format;

public class ByzToyServer extends ToyBaseServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ByzToyServer.class);
    private boolean fullByz = false;
    List<List<Integer>> groups = new ArrayList<>();
    private int delayTime;
    final Queue<Types.Block> byzProposed = new LinkedList<>();


    public ByzToyServer(int id, int worker, int n, int f, int maxTx, boolean fastMode,
                          CommLayer comm) {
        super(id, worker, n, f, maxTx, fastMode, comm);
        groups.add(new ArrayList<>());
        for (int i = 0 ; i < n ; i++) {
            groups.get(0).add(i); // At the beginning there is no byzantine behaviour
        }
    }

    @Override
    void commSendLogic() throws InterruptedException {
        while (!stopped.get()) {
            Types.Block b1;
            Types.Block b2;
            synchronized (blocksForPropose) {
                while (blocksForPropose.isEmpty()) {
                    blocksForPropose.wait();
                }
                b1 = blocksForPropose.poll();
                if (b1.getDataCount() == 0) continue;
                b2 = getByzBlock(b1);
                synchronized (byzProposed) {
                    synchronized (proposedBlocks) {
                        comm.send(worker, b1, groups.get(0).stream().mapToInt(i->i).toArray());
                        proposedBlocks.add(b1);
                        if (groups.size() > 1) {
                            comm.send(worker, b2, groups.get(1).stream().mapToInt(i->i).toArray());
                            byzProposed.add(b2);
                        }


                    }
                }

            }

        }
    }

//    @Override
//    void commSendLogic() throws InterruptedException {
//        while (!stopped.get()) {
//            Thread.sleep(200);
//            synchronized (byzProposed) {
//                sendByzLogic();
//            }
//
//
//        }
//    }

//    void sendByzLogic() {
//        ArrayList<Types.Block> byzs = new ArrayList<>();
//        for (List<Integer> g : groups) {
//            Types.Block byz = getByzBlock();
//            comm.send(worker, byz, g.stream().mapToInt(i->i).toArray());
//            byzs.add(byz);
//        }
//        byzProposed.add(byzs);
//    }

//    Types.BlockHeader leaderImpl() {
//        addTransactionsToCurrBlock();
//        if (!configuredFastMode) {
//            return normalLeaderPhase();
//        }
//        if (currHeight == 1 || !fastMode) {
//            normalLeaderPhase();
//        }
//        return fastModePhase();
//    }

    Types.BlockHeader leaderImpl() throws InterruptedException {
        addTransactionsToCurrBlock();

        if (currLeader != getID()) {
            return null;
        }
        if (delayTime > 0) {
            Random rand = new Random();
            int x = rand.nextInt(delayTime);
//            int x = maxTime;
            logger.info(format("[#%d] sleeps for %d ms", getID(), x));
            Thread.sleep(x);
        }
        synchronized (byzProposed) {
            WRB.WRBSend(getHeaderForCurrentBlock(BCS.nbGetBlock(worker, currHeight - 1).getHeader(),
                    currHeight, cidSeries, cid), groups.get(0).stream().mapToInt(i->i).toArray());


            if (groups.size() > 1 && byzProposed.size() > 0) {
                Types.Block byz = byzProposed.element();
                WRB.WRBSend(createBlockHeader(byz, BCS.nbGetBlock(worker, currHeight - 1).getHeader(), getID(),
                        currHeight, cidSeries, cid, worker, byz.getId()),  groups.get(1).stream().mapToInt(j->j).toArray());
            }
        }

        return null;
    }

    @Override
    void removeFromPendings(Types.BlockHeader recHeader, Types.Block recBlock) {
        super.removeFromPendings(recHeader, recBlock);
        if (currLeader == getID() && !recHeader.getEmpty()) {
            logger.debug(format("[#%d-C[%d]] byz nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d ; cid=%d]",
                    getID(), worker, recBlock.getHeader().getBid().getPid(), currHeight, cidSeries, cid));
            synchronized (byzProposed) {
                byzProposed.poll();
            }
        }
    }

    @Override
    void resetState() {
        super.resetState();
        synchronized (byzProposed) {
            while (byzProposed.poll() != null);
        }
    }


    public void setByzSetting() {
        LinkedList<Integer> ids = new LinkedList<>();
        for (int i =  0 ; i < n ; i++) {
            ids.add(i);
        }
        logger.info("Setting byz");
        synchronized (byzProposed) {
            this.fullByz = true;
            groups = new ArrayList<>();
            groups.add(new ArrayList<>());
            Random rand = new Random();
            for (int i = 0 ; i < n/2 ; i++) {
                int index = rand.nextInt(ids.size());
                groups.get(0).add(ids.get(index));
                ids.remove(index);
            }
            this.groups.add(ids);
            logger.info("Byzantine group are " + groups);
            synchronized (proposedBlocks) {
                while (proposedBlocks.poll() != null);
            }
            while (byzProposed.poll() != null);
        }
    }

    public void setAsyncParam(int maxTime) {
        this.delayTime = maxTime;

    }

    Types.Block getByzBlock(Types.Block b) {
        SecureRandom random = new SecureRandom();
        byte[] byzTx = new byte[40];
        int bid = random.nextInt();
        return b.toBuilder().addData(Types.Transaction.newBuilder()
                .setId(Types.txID.newBuilder()
                        .setProposerID(getID())
                        .setBid(bid)
                        .setTxNum(1)
                        .setChannel(worker))
                        .setData(ByteString.copyFrom(byzTx))
//                        .setServerTs(System.currentTimeMillis())
                .build()).build();
//        Types.Block.Builder b = Types.Block.newBuilder()
//                .setId(Types.BlockID.newBuilder()
//                        .setBid(bid)
//                        .setPid(getID())
//                        .build());
//        for (int i = 0 ; i < maxTransactionInBlock ; i++) {
//            random.nextBytes(byzTx);
//             b.addData(Types.Transaction.newBuilder()
//                    .setId(Types.txID.newBuilder()
//                            .setProposerID(getID())
//                            .setBid(bid)
//                            .setTxNum(1)
//                            .setChannel(worker))
////                        .setServerTs(System.currentTimeMillis())
//                    .build());
//        }
//
//                return b.build();
    }

//    @Override
//    public Blockchain initBC(int id, int channel) {
//        return createBlockchain(Utils.BCT.SGC, id, n, sPath);
//    }

//    @Override
//    public Blockchain getBC(int start, int end) {
//        return new Blockchain(this.bc, start, end);
//    }
//
//    @Override
//    public Blockchain getEmptyBC() {
//        return new Blockchain(this.getID());
//    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {
        if (delayTime > 0) {
            logger.debug(format("[#%d-%d] potentialBehaviourForSync sleeps for [%d]", getID(), worker, delayTime));
            Thread.sleep(delayTime);
        }
    }

}
