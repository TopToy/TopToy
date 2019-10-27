package servers;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.google.protobuf.InvalidProtocolBufferException;
import das.ab.ABService;
import proto.types.block;
import proto.types.transaction;
import utils.DiskUtils;
import utils.statistics.Statistics;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

public class BFTSMaRtOrderer extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BFTSMaRtOrderer.class);

    private int id;
    private int workers;
    private AsynchServiceProxy ABProxy;
    private ServiceReplica sr;
    Queue<block.Block>[]  recBlocks;
//    BFTSmaRTWorker[] bftSmaRTWorkers;
    int bSize;
    String swapPath = "blocks";
//    ExecutorService executorService;
    AtomicBoolean stopped = new AtomicBoolean(true);

    public BFTSMaRtOrderer(int id, int workers, int bSize, String configHome) {
        this.id = id;
        this.workers = workers;
        this.bSize = bSize;

        sr = new ServiceReplica(id, this, this, configHome);
        logger.info("Start ServiceReplica");
        ABProxy = new AsynchServiceProxy(id, configHome);
        logger.info("Start AsynchServiceProxy");

        recBlocks = new LinkedList[workers];
//        bftSmaRTWorkers = new BFTSmaRTWorker[workers];
//        executorService = Executors.newFixedThreadPool(workers);
        for (int i = 0 ; i < workers ; i++) {
            recBlocks[i] = new LinkedList<>();
//            bftSmaRTWorkers[i] = new BFTSmaRTWorker(id, i, ABProxy);
//            DiskUtils.createStorageDir(Paths.get("blocks", String.valueOf(i)));
        }

        logger.info(format("Initiated BFTSMaRtOrderer: [id=%d; w=%d]", id,workers));
    }


    public void start() {
        stopped.set(false);
        for (int i = 0 ; i < workers ; i++) {
//            int finalI = i;
//            executorService.submit(() -> {
//                try {
//                    writeBlockToDisk(finalI);
//                } catch (InterruptedException | IOException e) {
//                    logger.error(e);
//                }
//            });
//            bftSmaRTWorkers[i].start();
        }
        logger.info(format("start BFTSMaRtOrderer"));

    }
    public void shutdown() {
        stopped.set(true);
//        executorService.shutdownNow();
        for (int i = 0 ; i < workers ; i++) {
//            bftSmaRTWorkers[i].shutdown();
        }
        ABProxy.close();
        sr.kill();
        logger.info(format("stopped BFTSMaRtOrderer"));

    }

    @Override
    public void installSnapshot(byte[] state) {
        logger.debug(format("[#%d] installSnapshot called", id));
    }

    @Override
    public byte[] getSnapshot() {
        logger.debug(format("[#%d] getSnapshot called", id));
        return new byte[1];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            block.Block b = block.Block.parseFrom(command);
            int w = b.getHeader().getM().getChannel();
//            transaction.Transaction tx = transaction.Transaction.parseFrom(command);
//            int w = tx.getId().getChannel();

            Statistics.updateTxCount(b.getDataCount());

            synchronized (recBlocks[w]) {
                recBlocks[w].add(b);
                recBlocks[w].notify();

            }
        } catch (InvalidProtocolBufferException e) {
            logger.debug("Received NULL message!");
            return new byte[0];

        }
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return new byte[0];
    }

    public block.Block deliver(int w) {
        synchronized (recBlocks[w]) {
            if (recBlocks[w].size() == 0) return null;
            return recBlocks[w].remove();
        }
    }

    public void propose(int w, block.Block b) {
        ABProxy.invokeAsynchRequest(b.toByteArray(), new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {
                ABProxy.cleanAsynchRequest(context.getReqId());
            }
        }, TOMMessageType.ORDERED_REQUEST);
    }


//    void writeBlockToDisk(int w) throws InterruptedException, IOException {
//        int h = 0;
//        while (!stopped.get()) {
//            synchronized (recTransactions[w]) {
//                while (recTransactions[w].size() < bSize) {
//                    recTransactions[w].wait();
//                }
//                block.Block.Builder bb = block.Block.newBuilder();
//                while (bb.getDataCount() < bSize) {
//                    bb.addData(recTransactions[w].remove());
//                }
//                DiskUtils.cutBlock(bb.build(), Paths.get(swapPath, String.valueOf(w)));
//                h++;
//            }
//        }
//
//    }
}
