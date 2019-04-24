package servers;

import blockchain.Blockchain;

import blockchain.Utils;
import communication.CommLayer;
import das.ab.ABService;

import das.wrb.WRB;
import proto.Types;

import java.security.SecureRandom;
import java.util.*;

import static blockchain.Utils.createBlockHeader;
import static blockchain.Utils.createBlockchain;
import static java.lang.String.format;

public class ByzToyServer extends ToyBaseServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ByzToyServer.class);
    private boolean fullByz = false;
    List<List<Integer>> groups = new ArrayList<>();
    private int delayTime;
    final Queue<List<Types.Block>> byzProposed = new LinkedList<>();

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
            Thread.sleep(100);
            synchronized (byzProposed) {
                sendByzLogic();
            }


        }
    }

    void sendByzLogic() {
        ArrayList<Types.Block> byzs = new ArrayList<>();
        for (List<Integer> g : groups) {
            Types.Block byz = getByzBlock();
            comm.send(worker, byz, g.stream().mapToInt(i->i).toArray());
            byzs.add(byz);
        }
        byzProposed.add(byzs);
    }

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
            if (byzProposed.isEmpty()) {
                sendByzLogic();
            }
            int i = 0;
            for (List<Integer> g : groups) {
                Types.Block byz = byzProposed.element().get(i);
                i++;
                WRB.WRBSend(createBlockHeader(byz, bc.getBlock(currHeight - 1).getHeader(), getID(), currHeight,
                        cidSeries, cid, worker, byz.getId()),  g.stream().mapToInt(j->j).toArray());
            }
        }

        return null;
    }

    @Override
    void removeFromPendings(Types.BlockHeader recHeader, Types.Block recBlock) {
        if (currLeader == getID() && !recHeader.getEmpty()) {
            logger.debug(format("[#%d-C[%d]] nullifies currBlock [sender=%d] [height=%d] [cidSeries=%d, cid=%d]",
                    getID(), worker, recBlock.getHeader().getBid().getPid(), currHeight, cidSeries, cid));
            synchronized (byzProposed) {
                byzProposed.remove();
            }
        }
    }


    public void setByzSetting(boolean fullByz, List<List<Integer>> groups) {
        synchronized (byzProposed) {
            if (!fullByz && groups.size() > 2) {
                logger.debug("On non full byzantine behavior there is at most two send groups");
                return;
            }
            this.fullByz = fullByz;
            this.groups = groups;
            while (byzProposed.poll() != null);
        }
    }

    public void setAsyncParam(int maxTime) {
        this.delayTime = maxTime;

    }

    Types.Block getByzBlock() {
        SecureRandom random = new SecureRandom();
        byte[] byzTx = new byte[40];
        int bid = random.nextInt();
        random.nextBytes(byzTx);
        return Types.Block.newBuilder()
                .setId(Types.BlockID.newBuilder()
                        .setBid(bid)
                        .setPid(getID())
                        .build())
                .addData(Types.Transaction.newBuilder()
                        .setId(Types.txID.newBuilder()
                                .setProposerID(getID())
                                .setBid(bid)
                                .setTxNum(1)
                                .setChannel(worker))
                        .setServerTs(System.currentTimeMillis())
                        .build())
                .build();
    }

    @Override
    public Blockchain initBC(int id, int channel) {
        return createBlockchain(Utils.BCT.SGC, id, n, sPath);
    }

    @Override
    public Blockchain getBC(int start, int end) {
        return new Blockchain(this.bc, start, end);
    }

    @Override
    public Blockchain getEmptyBC() {
        return new Blockchain(this.getID());
    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {
        if (delayTime > 0) {
            logger.debug(format("[#%d-%d] potentialBehaviourForSync sleeps for [%d]", getID(), worker, delayTime));
            Thread.sleep(delayTime);
        }
    }

}
