package servers;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import com.google.protobuf.ByteString;
import proto.types.block;
import proto.types.transaction;
import utils.Config;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

public class BFTSmaRTWorker {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BFTSMaRtOrderer.class);

    private int w;
    private int id;
    private int txSize = Config.getTxSize();
    private AtomicBoolean stooped = new AtomicBoolean(true);
    private int txNum = 0;
    private Thread main;
    private AsynchServiceProxy ab;
    int blockcSize = Config.getMaxTransactionsInBlock();
    BFTSmaRTWorker(int id, int w, AsynchServiceProxy ab) {
        this.id = id;
        this.w = w;
        this.ab = ab;
    }

    private transaction.Transaction createNewTransaction() {
        SecureRandom random = new SecureRandom();
        byte[] tx = new byte[txSize];
        random.nextBytes(tx);
        return transaction.Transaction.newBuilder()
                .setId(transaction.TxID.newBuilder()
                        .setProposerID(id)
                        .setBid(0)
                        .setTxNum(txNum++)
                        .setChannel(w))
                .setClientID(random.nextInt(1000))
                .setData(ByteString.copyFrom(tx))
                .build();
    }

    private void mainPublish() {
        while (!stooped.get()) {
            block.Block.Builder b = block.Block.newBuilder();
            for (int i = 0 ; i < blockcSize ; i++) {
                b.addData(createNewTransaction());
            }
            ab.invokeAsynchRequest(b.build().toByteArray(), new ReplyListener() {
                @Override
                public void reset() {

                }

                @Override
                public void replyReceived(RequestContext context, TOMMessage reply) {
//                ABProxy.cleanAsynchRequest(context.getReqId());
                }
            }, TOMMessageType.ORDERED_REQUEST);
        }
    }

    public void start() {
        stooped.set(false);
        main = new Thread(this::mainPublish);
        main.start();
        logger.info(format("Publisher [%d] is up", w));
    }

    public void shutdown() {
        logger.info(format("Shutting down publisher [%d]", w));
        stooped.set(true);
        main.interrupt();
        try {
            main.join();
        } catch (InterruptedException e) {
            logger.error(e);
        }
        logger.info(format("Publisher [%d] is shutdown", w));

    }
}
