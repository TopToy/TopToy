package das.bbc;


import com.google.protobuf.InvalidProtocolBufferException;
import das.ab.ABService;
import das.data.BbcDecData;
import das.data.Data;
import das.data.VoteData;

import static java.lang.String.format;
import proto.types.meta.*;
import proto.types.bbc.*;
import proto.types.rb.*;

public class BBC {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BBC.class);

    private static int n;
    private static int id;
    private static int f;
    private static int qSize;

    public BBC(int id, int n, int f, int qSize) {
        BBC.id = id;
        BBC.n = n;
        BBC.f = f;
        BBC.qSize = qSize;
        logger.info(format("Initiated BBC: [id=%d; n=%d; f=%d; qSize=%d]", id, n, f, qSize));

    }

    static public void reconfigure() {

    }

    static public BbcDecData propose(BbcMsg bm, Meta key) throws InterruptedException {
        logger.debug(format("[#%d-C[%d]] broadcast BBC message [cidSeries=%d ; cid=%d ; height=%d ; vote=%b]",
                id, bm.getM().getChannel(), bm.getM().getCidSeries(), bm.getM().getCid(), bm.getHeight(), bm.getVote()));
        ABService.broadcast(bm.toByteArray(), bm.getM(), Data.RBTypes.BBC);
        int worker = bm.getM().getChannel();
        synchronized (Data.bbcRegDec[worker]) {
            while (!Data.bbcRegDec[worker].containsKey(key)) {
                Data.bbcRegDec[worker].wait();
            }
        }
        return Data.bbcRegDec[worker].get(key);
    }

    static void nonBlockingPropose(BbcMsg bm) {
        ABService.broadcast(bm.toByteArray(), bm.getM(), Data.RBTypes.BBC);
    }

    static public void addToBBCData(RBMsg omsg, int worker) throws InvalidProtocolBufferException {
        BbcMsg bm = BbcMsg.parseFrom(omsg.getData());
        Meta key = omsg.getM();
        logger.debug(format("[#%d-C[%d]] received BBC message [sender=%d ; cidSeries=%d ; cid=%d ; height=%d ; vote=%b]"
                ,id, worker, bm.getSender(), bm.getM().getCidSeries(), bm.getM().getCid(), bm.getHeight(), bm.getVote()));
        OBBC.reCons(key, id, bm.getHeight());
        Data.bbcVotes[worker].putIfAbsent(key, new VoteData());
        Data.bbcVotes[worker].computeIfPresent(key, (k, v) -> {
            if (v.getVotersNum() == n - f) return v;
            if (!v.addVote(bm.getSender(), bm.getVote())) return v;
            if (v.getVotersNum() == n - f) {
                synchronized (Data.bbcRegDec[worker]) {
                    Data.bbcRegDec[worker].putIfAbsent(key, new BbcDecData(v.getVoteReasult(), false));
                    logger.debug(format("[#%d] notifies on  [channel=%d ; cidSeries=%d ; cid=%d]", id, worker,
                            bm.getM().getCidSeries(), bm.getM().getCid()));
                    Data.bbcRegDec[worker].notifyAll();
                }
            }
            return v;
        });
    }


}
