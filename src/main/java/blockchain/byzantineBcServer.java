package blockchain;

import config.Config;
import crypto.DigestMethod;
import proto.Block;
import rmf.ByzantineRmfNode;

import java.util.*;

import static java.lang.String.format;

public class byzantineBcServer extends bcServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cbcServer.class);
    private boolean fullByz = false;
    public byzantineBcServer(String addr, int rmfPort, int id) {
        super(addr, rmfPort, id);
        rmfServer.stop();
        rmfServer = new ByzantineRmfNode(id, addr, rmfPort, Config.getF(),
                Config.getCluster(), Config.getRMFbbcConfigHome());
    }

     void leaderImpl() {
        if (currLeader != getID()) {
            return;
        }
        logger.info(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

        synchronized (blockLock) {
            addTransactionsToCurrBlock();
            Block sealedBlock1 = currBlock.construct(getID(), currHeight,
                    DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
            String msg = "Hello Byz";
            addTransaction(msg.getBytes(), getID());
            Block sealedBlock2 = currBlock.construct(getID(), currHeight,
                    DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
            currBlock = bc.createNewBLock();
            List<Integer> all = new ArrayList<>();
            for (int i = 0 ; i < n ;i++) {
                all.add(i);
            }
            List<Integer> heights = new ArrayList<>();
            List<byte[]> msgs = new ArrayList<>();
            msgs.add(sealedBlock1.toByteArray());
            msgs.add(sealedBlock2.toByteArray());
            List<List<Integer>> sids = new ArrayList<>();
            sids.add(all.subList(0, 2));
            sids.add(all.subList(2, 4));
            for (int i = 0 ; i < 2 ; i++) {
                heights.add(currHeight);
            }
            if (fullByz) {
                ((ByzantineRmfNode)rmfServer).devidedBroadcast(cidSeries, cid, msgs, heights, sids);

            } else {
                ((ByzantineRmfNode)rmfServer).selectiveBroadcast(cidSeries, cid, sealedBlock1.toByteArray(), currHeight, all.subList(0, n/2));
            }
        }
    }

    @Override
    blockchain initBC(int id) {
        return new basicBlockchain(id);
    }

    @Override
    blockchain getBC(int start, int end) {
        return new basicBlockchain(this.bc, start, end);
    }

    public void setFullByz() {
        fullByz = true;
    }
}
