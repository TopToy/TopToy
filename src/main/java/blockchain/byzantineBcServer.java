package blockchain;

import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import crypto.DigestMethod;

import org.apache.commons.lang.ArrayUtils;
import proto.Types.*;
import rmf.ByzantineRmfNode;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.*;

import static java.lang.String.format;

public class byzantineBcServer extends bcServer {

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(byzantineBcServer.class);
    private boolean fullByz = false;
    List<List<Integer>> groups = new ArrayList<>();
    public byzantineBcServer(String addr, int rmfPort, int id, int channel, int f, int tmo, int tmoInterval,
                             int maxTx, boolean fastMode, ArrayList<Node> cluster,
                             String bbcConfig, String panicConfig, String syncConfig) {
        super(addr, rmfPort, id, channel, f, tmo, tmoInterval, maxTx, fastMode, cluster,
                bbcConfig, panicConfig, syncConfig);
        rmfServer.stop(); //TODO: May cause a problem
        rmfServer = new ByzantineRmfNode(1, id, addr, rmfPort, Config.getF(),
                cluster, bbcConfig);
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

    private void addByzData() {
        SecureRandom random = new SecureRandom();
        byte[] byzTx = new byte[40];
        random.nextBytes(byzTx);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long magic = timestamp.getTime();
        String txID = new BigInteger(DigestMethod.hash(ArrayUtils.addAll(byzTx, (String.valueOf(magic)).getBytes())))
                .toString()
                .replaceAll("-","N");
        Transaction t = Transaction.newBuilder()
                .setClientID(getID())
                .setData(ByteString.copyFrom(byzTx))
                .setTxID(txID)
                .build();
        currBlock.addTransaction(t);
        if (currBlock.getTransactionCount() > 1) {
            currBlock.removeTransaction(0);
        }
    }
     byte[] leaderImpl() {
        if (currLeader != getID()) {
            return null;
        }
        logger.debug(format("[#%d] prepare to disseminate a new block of [height=%d]", getID(), currHeight));

//        synchronized (blockLock) {
            addTransactionsToCurrBlock();
            Block sealedBlock1 = currBlock.construct(getID(), currHeight, cidSeries, cid,
                    DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));

            List<byte[]> msgs = new ArrayList<>();
            List<Integer> heights = new ArrayList<>();
            for (int i = 0 ; i < groups.size() ; i++) {
                addByzData();
                Block byzBlock = currBlock.construct(getID(), currHeight, cidSeries, cid,
                        DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
                msgs.add(byzBlock.toByteArray());
                heights.add(currHeight);
            }

            if (fullByz) {
                ((ByzantineRmfNode)rmfServer).devidedBroadcast(channel, cidSeries, cid, msgs, heights, groups);

            } else {
                ((ByzantineRmfNode)rmfServer).selectiveBroadcast(channel, cidSeries, cid, sealedBlock1.toByteArray(), currHeight, groups.get(0));
            }
//        }
         return null;
    }

    @Override
    public blockchain initBC(int id) {
        return new basicBlockchain(id);
    }

    @Override
    public blockchain getBC(int start, int end) {
        return new basicBlockchain(this.bc, start, end);
    }

    public void setFullByz() {
        fullByz = true;
    }
}
