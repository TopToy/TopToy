package blockchain;

import com.google.protobuf.ByteString;
import crypto.DigestMethod;
import crypto.blockDigSig;
import org.apache.commons.lang.ArrayUtils;
import proto.Types.*;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseBlock {
    public Block.Builder blockBuilder = Block.newBuilder();

    abstract public boolean validateTransaction(Transaction t);


    public Transaction getTransaction(int index) {
        return blockBuilder.getData(index);
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> ret = new ArrayList<>();
        blockBuilder.addAllData(ret);
        return ret;
    }


    public void addTransaction(Transaction t) {
            blockBuilder.addData(t);
    }

    public void removeTransaction(int index) {
        blockBuilder.removeData(index);
    }

    public Block construct(int creatorID, int height, int cidSeries, int cid, int channel, BlockHeader header) {
        long start = System.currentTimeMillis();
        byte[] tHash = new byte[0];
        for (Transaction t : blockBuilder.getDataList()) {
            tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
        }
        byte[] headerArray = new byte[0];
        if (header != null) {
            headerArray = header.toByteArray();
        }
        blockBuilder
                .setHeader(blockBuilder
                        .getHeader()
                        .toBuilder()
                        .setM(Meta.newBuilder()
                                .setCid(cid)
                                .setCidSeries(cidSeries)
                                .setSender(creatorID)
                                .setChannel(channel)
                                .build())
                        .setHeight(height)
                        .setPrev(ByteString.copyFrom(DigestMethod.hash(headerArray)))
                        .setTransactionHash(ByteString.copyFrom(tHash))
                        .build());
//        Block
//                blockBuilder.
//                getHeaderBuilder().
//                    setCreatorID(creatorID).
//                    setHeight(height).
//                    setCidSeries(cidSeries).
//                    setCid(cid).
//                    setPrev(ByteString.copyFrom(prevHash)).
//                    setTransactionHash(ByteString.copyFrom(tHash)));
        if (creatorID == -1) {
            return blockBuilder.build();
        }
//        return blockBuilder
//                .setHeader(blockBuilder
//                        .getHeader()
//                        .toBuilder()
//                        .build())
//                .build();

        String signature = blockDigSig.sign(blockBuilder.getHeader());
        return blockBuilder
                .setHeader(blockBuilder
                        .getHeader()
                        .toBuilder()
                .setProof(signature)
                .build())
                .setSt(blockBuilder.getSt().toBuilder().setSign(System.currentTimeMillis() - start))
                .build();
    }

    public int getTransactionCount() {
        return blockBuilder.getDataCount();
    }


}
