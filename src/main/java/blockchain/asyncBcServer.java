package blockchain;

import crypto.DigestMethod;
import proto.Types;
import java.util.Random;

import static java.lang.String.format;

public class asyncBcServer extends bcServer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(asyncBcServer.class);

    public asyncBcServer(String addr, int rmfPort,  int id) {
        super(addr, rmfPort,  id);
    }

    byte[] leaderImpl() throws InterruptedException {
        Random rand = new Random();
        int x = rand.nextInt(1500) + 1;
        logger.debug(format("[#%d] sleeps for %d ms", getID(), x));
        Thread.sleep(x);
        if (!configuredFastMode) {
            return normalLeaderPhase();
        }
        if (currHeight == 1 || !fastMode) {
            normalLeaderPhase();
        }
        return fastModePhase();
    }

        byte[] normalLeaderPhase() {
            if (currLeader != getID()) {
                return null;
            }
            logger.debug(format("[#%d] prepare to disseminate a new block of [height=%d] [cidSeries=%d ; cid=%d]",
                    getID(), currHeight, cidSeries, cid));
            addTransactionsToCurrBlock();
            Types.Block sealedBlock = currBlock.construct(getID(), currHeight, cidSeries, cid, DigestMethod.hash(bc.getBlock(currHeight - 1).getHeader().toByteArray()));
            rmfServer.broadcast(cidSeries, cid, sealedBlock.toByteArray(), currHeight);
            return null;
        }

        byte[] fastModePhase() {
            if ((currLeader + 1) % n != getID()) {
                return null;
            }
            logger.debug(format("[#%d] prepare fast mode phase for [height=%d] [cidSeries=%d ; cid=%d]",
                    getID(), currHeight + 1, cidSeries, cid + 1));
            addTransactionsToCurrBlock();
            return currBlock.construct(getID(), currHeight + 1, cidSeries, cid + 1, new byte[0]).toByteArray();
        }

    @Override
    blockchain initBC(int id) {
        return new basicBlockchain(id);
    }

    @Override
    blockchain getBC(int start, int end) {
        return new basicBlockchain(this.bc, start, end);
    }
}
