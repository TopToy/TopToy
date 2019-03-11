package servers;

import blockchain.Blockchain;

import blockchain.Utils;
import communication.CommLayer;
import config.Node;
import das.RBroadcast.RBrodcastService;

import das.wrb.ByzantineWrbNode;
import das.wrb.WrbNode;
import proto.Types;
import java.util.*;

import static blockchain.Utils.createBlockchain;
import static java.lang.String.format;

public class ByzToyServer extends ToyBaseServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ByzToyServer.class);
    private boolean fullByz = false;
    List<List<Integer>> groups = new ArrayList<>();
    private int delayTime;

    public ByzToyServer(String addr, int wrbPort, int id, int channel, int f, int maxTx, boolean fastMode,
                          WrbNode wrb, CommLayer comm, RBrodcastService rb) {
        super(addr, wrbPort, id, channel, f, maxTx, fastMode, wrb, comm, rb);
    }

    public ByzToyServer(String addr, int wrbPort, int commPort, int id, int channel, int f, int tmo, int tmoInterval,
                          int maxTx, boolean fastMode, ArrayList<Node> wrbCluster, ArrayList<Node> commCluster,
                          String bbcConfig, String rbConfigPath, String serverCrt, String serverPrivKey, String caRoot) {

        super(addr, wrbPort, commPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, wrbCluster, commCluster,
                bbcConfig, rbConfigPath, serverCrt, serverPrivKey, caRoot);
        rmfServer.stop();
        rmfServer = new ByzantineWrbNode(1, id, addr, wrbPort, f, tmo, tmoInterval,
                wrbCluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        groups.add(new ArrayList<>());
        for (int i = 0 ; i < n ; i++) {
            groups.get(0).add(i); // At the beginning there is no byzantine behaviour
        }
    }

    Types.BlockHeader leaderImpl() {
        addTransactionsToCurrBlock();
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }


    private Types.BlockHeader normalLeaderPhase() {
        if (currLeader != getID()) {
            return null;
        }
        logger.debug(format("[#%d -C[%d]] prepare to disseminate a new block header for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), channel, currHeight, cidSeries, cid));

        rmfServer.broadcast(getHeaderForCurrentBlock(bc.getBlock(currHeight - 1).getHeader(),
                currHeight, cidSeries, cid));
        return null;
    }

    Types.BlockHeader fastModePhase() {
        if ((currLeader + 1) % n != getID()) {
            return null;
        }
        logger.debug(format("[#%d-C[%d]] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                getID(), channel, currHeight + 1, cidSeries, cid + 1));
        return getHeaderForCurrentBlock(null, currHeight + 1, cidSeries, cid + 1);
    }

    public void setByzSetting(boolean fullByz, List<List<Integer>> groups) {
        if (!fullByz && groups.size() > 2) {
            logger.debug("On non full byzantine behavior there is at most two send groups");
            return;
        }
        this.fullByz = fullByz;
        this.groups = groups;

    }

    public void setAsyncParam(int maxTime) {
        this.delayTime = maxTime;

    }
//
//    private void addByzData() {
//        SecureRandom random = new SecureRandom();
//        byte[] byzTx = new byte[40];
//        random.nextBytes(byzTx);
//
//        synchronized (blocksForPropose.element()) {
//            int cbid = blocksForPropose.element().blockBuilder.getId().getBid();
//            int txNum = blocksForPropose.element().getTransactionCount();
//            Transaction t = Transaction.newBuilder()
//                    .setClientID(-1)
//                    .setId(Types.txID.newBuilder()
//                            .setTxNum(txNum)
//                            .setBid(cbid)
//                            .setProposerID(getID())
//                            .build())
//                    .setData(ByteString.copyFrom(byzTx))
//                    .build();
//            blocksForPropose.element().addTransaction(t);
//            if(blocksForPropose.element().getTransactionCount() > 1) {
//                blocksForPropose.element().removeTransaction(0);
//            }
//        }
////        Transaction t = Transaction.newBuilder()
////                .setClientID(-1)
////                .setId(Types.txID.newBuilder()
////                        .setTxNum(txNum.getAndIncrement())
////                        .setProposerID(getID())
////                        .build())
////                .setData(ByteString.copyFrom(byzTx))
////                .build();
////        currBlock.addTransaction(t);
////        if (currBlock.getTransactionCount() > 1) {
////            currBlock.removeTransaction(0);
////        }
//    }
//
//    Block leaderImpl() throws InterruptedException {
//        if (currLeader != getID()) {
//            return null;
//        }
//        if (delayTime > 0) {
//            Random rand = new Random();
//            int x = rand.nextInt(delayTime);
////            int x = maxTime;
//            logger.info(format("[#%d] sleeps for %d ms", getID(), x));
//            Thread.sleep(x);
//        }
//        logger.debug(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));
//
//    //        synchronized (blockLock) {
//        addTransactionsToCurrBlock();
//        Block sealedBlock1;
//        Block byzBlock;
//        List<Block> msgs = new ArrayList<>();
////        List<Integer> heights = new ArrayList<>();
//        synchronized (blocksForPropose.element()) {
//            sealedBlock1 = blocksForPropose.element().construct(getID(), currHeight, cidSeries, cid,
//                    channel, bc.getBlock(currHeight - 1).getHeader());
//
//            for (int i = 0 ; i < groups.size() ; i++) {
//                addByzData();
//                byzBlock = blocksForPropose.element()
//                        .construct(getID(), currHeight, cidSeries, cid,
//                        channel, bc.getBlock(currHeight - 1).getHeader());
//                msgs.add(byzBlock);
////                heights.add(currHeight);
//            }
//        }
////        Block sealedBlock1 = currBlock.construct(getID(), currHeight, cidSeries, cid,
////                channel, bc.getBlock(currHeight - 1).getHeader());
////
////        List<Block> msgs = new ArrayList<>();
////        List<Integer> heights = new ArrayList<>();
////        for (int i = 0 ; i < groups.size() ; i++) {
////            addByzData();
////            Block byzBlock = currBlock.construct(getID(), currHeight, cidSeries, cid,
////                    channel, bc.getBlock(currHeight - 1).getHeader());
////            msgs.add(byzBlock);
////            heights.add(currHeight);
////        }
//
//        if (fullByz) {
//            ((ByzantineWrbNode)rmfServer).devidedBroadcast(msgs, groups);
//
//        } else {
//            ((ByzantineWrbNode)rmfServer).broadcast(sealedBlock1);
//        }
//    //        }
//     return null;
//    }

    @Override
    public Blockchain initBC(int id, int channel) {
        return createBlockchain(Utils.BCT.SGC, id, 10000, sPath);
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
            logger.debug(format("[#%d-%d] potentialBehaviourForSync sleeps for [%d]", getID(), channel, delayTime));
            Thread.sleep(delayTime);
        }
    }

}
