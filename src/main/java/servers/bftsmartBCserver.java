package servers;

import blockchain.blockchain;
import blockchain.basicBlockchain;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import consensus.RBroadcast.RBrodcastService;
import crypto.DigestMethod;
import org.apache.commons.lang.ArrayUtils;
import proto.Types;


import javax.print.DocFlavor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

public class bftsmartBCserver implements server {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bftsmartBCserver.class);
    final blockchain bc;
    int id;
    int n;
    int f;
    int txNum;
    RBrodcastService rbService;
    final ArrayList<Types.Transaction> txPool;
    final ArrayList<Types.Transaction> pendingsTx;
    final HashMap<String, Types.Transaction> approvedTx;
    boolean stopped;
    Thread deliverThread = new Thread(() -> {
        try {
            deliverBlock();
        } catch (InvalidProtocolBufferException | InterruptedException e) {
            logger.error(format("[#%d]", id), e);
        }
    });
    Thread broadcastThread = new Thread(()-> {
        try {
            broadcastBlock();
        } catch (InterruptedException e) {
            logger.error(format("[#%d]", id), e);
        }
    });

    public bftsmartBCserver(int id, int f, int txNum, String configHome) {
        this.bc = new basicBlockchain(id, 0);
        this.id = id;
        this.f = f;
        this.n = 3*f + 1;
        this.txNum = txNum;
        this.stopped = false;
        this.txPool = new ArrayList<>();
        this.pendingsTx = new ArrayList<>();
        this.approvedTx = new HashMap<>();
        this.rbService = new RBrodcastService(1, id, configHome);
    }
    @Override
    public void start() {
        rbService.start();
    }

    @Override
    public void serve() {
        deliverThread.start();
        broadcastThread.start();
    }

    @Override
    public void shutdown() {
        stopped = true;
        deliverThread.interrupt();
        broadcastThread.interrupt();
        try {
            deliverThread.join();
            broadcastThread.join();
        } catch (InterruptedException e) {
            logger.error(format("[#%d]", id), e);
        }
        rbService.shutdown();
    }

    public String addTransaction(byte[] data, int clientID) {
        String txID = UUID.randomUUID().toString();
        Types.Transaction t = Types.Transaction.newBuilder()
                .setClientID(clientID)
                .setTxID(txID)
                .setData(ByteString.copyFrom(data))
                .build();
        synchronized (txPool) {
//            logger.debug(format("[#%d-C[%d]] adds transaction from [client=%d ; txID=%s]", getID(), t.getClientID(), t.getTxID()));
            txPool.add(t);
            txPool.notify();
        }
        return txID;
    }

    @Override
    public int isTxPresent(String txID) {
        synchronized (txPool) {
            for (Types.Transaction tx : txPool) {
                if (tx.getTxID().equals(txID)) {
                    return 0;
                }
            }
        }
        synchronized (pendingsTx) {
            for (Types.Transaction tx : pendingsTx) {
                if (tx.getTxID().equals(txID)) {
                    return 1;
                }
            }
        }

        synchronized (approvedTx) {
            if (approvedTx.keySet().contains(txID)) {
                return 2;
            }
            return -1;
        }

    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public int getBCSize() {
        return bc.getHeight() + 1;
    }

    @Override
    public Types.Block nonBlockingDeliver(int index) {
        synchronized (bc) {
            if (bc.getHeight() < index) {
                return null;
            }
            return bc.getBlock(index);
        }
    }

    @Override
    public void setByzSetting(boolean fullByz, List<List<Integer>> groups) {
        logger.warn(format("[#%d] setByzSetting not implemented", id));

    }

    @Override
    public void setAsyncParam(int tiem) {
        logger.warn(format("[#%d] setAsyncParam not implemented", id));
    }

    @Override
    public statistics getStatistics() {
        return null;
    }

    void deliverBlock() throws InvalidProtocolBufferException, InterruptedException {
        while (!stopped) {
            byte[] msg = rbService.deliver(0);

            Types.Block recBlock = Types.Block.parseFrom(msg);
            recBlock = recBlock.toBuilder()
                    .setHeader(recBlock.getHeader().toBuilder()
                            .setHeight(bc.getHeight() + 1)
                            .setPrev(ByteString.copyFrom(DigestMethod.hash(
                                    bc.getBlock(bc.getHeight()).getHeader().toByteArray())))
                            .build())
                    .setSt(recBlock.getSt().toBuilder().setDecided(System.currentTimeMillis()).build())
                    .build();
            for (int i = 0; i < recBlock.getDataCount(); i++) {
                synchronized (approvedTx) {
                    approvedTx.put(recBlock.getData(i).getTxID(), recBlock.getData(i));
                }

                synchronized (pendingsTx) {
                    pendingsTx.remove(recBlock.getData(i));
                }

            }
            synchronized (bc) {
                bc.addBlock(recBlock);
                bc.notify();
            }
        }
    }
    void broadcastBlock() throws InterruptedException {
        while (!stopped) {
            Types.Block b;
            synchronized (txPool) {
                while (txPool.size() == 0) {
                    txPool.wait();
                }
                b = createNewBlock();
//                logger.debug(format("[#%d] created new block", id));
            }
            rbService.broadcast(b.toByteArray(), 0, id);
        }

    }

    Types.Block createNewBlock() {
//        logger.debug(format("txPool is [%d]", txPool.size()));
        Types.Block.Builder b = Types.Block.newBuilder()
                .setHeader(Types.BlockHeader.newBuilder()
                        .setM(Types.Meta.newBuilder()
                                .setSender(id)
                                .build())
                        .build());
        byte[] tHash = new byte[0];
        int bound = Math.min(txNum, txPool.size());
//        logger.debug(format("going to add [%d] tx to block", bound));
        for (int i = 0 ; i < bound ; i++) {
            Types.Transaction tx = txPool.get(i);
            tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, tx.toByteArray()));
            txPool.remove(i);
            synchronized (pendingsTx) {
                pendingsTx.add(tx);
            }
            b.addData(tx);
        }
        b.setHeader(b.getHeader().toBuilder()
        .setTransactionHash(ByteString.copyFrom(tHash)));
        return b.build();

    }

    public Types.Block deliver(int index) throws InterruptedException {
        synchronized (bc) {
            while (bc.getHeight() < index) {
                bc.wait();
            }
            return bc.getBlock(index);
        }

    }

}
