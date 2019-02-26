package servers;

import blockchain.BaseBlockchain;

import blockchain.SBlockchain;
import com.google.protobuf.ByteString;
import config.Node;
import das.RBroadcast.RBrodcastService;

import proto.Types;
import proto.Types.*;
import das.wrb.ByzantineWrbNode;
import das.wrb.WrbNode;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

import static java.lang.String.format;

public class ByzToyServer extends ToyBaseServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ByzToyServer.class);
    private boolean fullByz = false;
    List<List<Integer>> groups = new ArrayList<>();
    private int delayTime;

    public ByzToyServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                        int maxTx, boolean fastMode, ArrayList<Node> cluster, WrbNode rmf, RBrodcastService panic, RBrodcastService sync) {
        super(addr, rmfPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster, rmf, panic, sync);
    }

    public ByzToyServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                        int maxTx, boolean fastMode, ArrayList<Node> cluster,
                        String bbcConfig, String panicConfig, String syncConfig,
                        String serverCrt, String serverPrivKey, String caRoot) {
        super(addr, rmfPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster,
                bbcConfig, panicConfig, syncConfig, serverCrt, serverPrivKey, caRoot);
        rmfServer.stop();
        rmfServer = new ByzantineWrbNode(1, id, addr, rmfPort, f, tmo, tmoInterval,
                cluster, bbcConfig, serverCrt, serverPrivKey, caRoot);
        groups.add(new ArrayList<>());
        for (int i = 0 ; i < n ; i++) {
            groups.get(0).add(i); // At the beginning there is no byzantine behaviour
        }
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

    private void addByzData() {
        SecureRandom random = new SecureRandom();
        byte[] byzTx = new byte[40];
        random.nextBytes(byzTx);
//        String txID = UUID.randomUUID().toString();
        Transaction t = Transaction.newBuilder()
                .setClientID(-1)
                .setId(Types.txID.newBuilder()
                        .setTxNum(txNum.getAndIncrement())
                        .setProposerID(getID())
                        .build())
                .setData(ByteString.copyFrom(byzTx))
                .build();
        currBlock.addTransaction(t);
        if (currBlock.getTransactionCount() > 1) {
            currBlock.removeTransaction(0);
        }
    }
    Block leaderImpl() throws InterruptedException {
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
        logger.debug(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

    //        synchronized (blockLock) {
        addTransactionsToCurrBlock();
        Block sealedBlock1 = currBlock.construct(getID(), currHeight, cidSeries, cid,
                channel, bc.getBlock(currHeight - 1).getHeader());

        List<Block> msgs = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        for (int i = 0 ; i < groups.size() ; i++) {
            addByzData();
            Block byzBlock = currBlock.construct(getID(), currHeight, cidSeries, cid,
                    channel, bc.getBlock(currHeight - 1).getHeader());
            msgs.add(byzBlock);
            heights.add(currHeight);
        }

        if (fullByz) {
            ((ByzantineWrbNode)rmfServer).devidedBroadcast(msgs, groups);

        } else {
            ((ByzantineWrbNode)rmfServer).broadcast(sealedBlock1);
        }
    //        }
     return null;
    }

    @Override
    public BaseBlockchain initBC(int id, int channel) {
        return new SBlockchain(id, channel);
    }

    @Override
    public BaseBlockchain getBC(int start, int end) {
        try {
            return new SBlockchain(this.bc, start, end);
        } catch (IOException e) {
            logger.error("Unable to return the blockchain", e);
            return null;
        }
    }
    @Override
    public BaseBlockchain getEmptyBC() {
        return new SBlockchain(getID());
    }

    @Override
    void potentialBehaviourForSync() throws InterruptedException {
        if (delayTime > 0) {
            logger.debug(format("[#%d-%c] potentialBehaviourForSync sleeps for [%d]", getID(), channel, delayTime));
            Thread.sleep(delayTime);
        }
    }

//    public void setFullByz() {
//        fullByz = true;
//    }
}
