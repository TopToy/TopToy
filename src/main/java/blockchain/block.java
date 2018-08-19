package blockchain;

import com.google.protobuf.ByteString;
import crypto.DigestMethod;
import proto.Types.*;

import java.util.ArrayList;
import java.util.List;

public abstract class block {
    private Block.Builder blockBuilder = Block.newBuilder();

    abstract boolean validateTransaction(Transaction t);


    public Transaction getTransaction(int index) {
        return blockBuilder.getData(index);
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> ret = new ArrayList<>();
        blockBuilder.addAllData(ret);
        return ret;
    }


    void addTransaction(Transaction t) {
            blockBuilder.addData(t);
    }

    public void removeTransaction(int index) {
        blockBuilder.removeData(index);
    }

    public Block construct(int creatorID, int height, int cidSeries, int cid, byte[] prevHash) {
        byte[] tHash = new byte[0];
        for (Transaction t : blockBuilder.getDataList()) {
            tHash = DigestMethod.hash(t.toByteArray());
        }
        return blockBuilder.setHeader(blockBuilder.
                getHeaderBuilder().
                    setCreatorID(creatorID).
                    setHeight(height).
                    setCidSeries(cidSeries).
                    setCid(cid).
                    setPrev(ByteString.copyFrom(prevHash)).
                    setTransactionHash(ByteString.copyFrom(tHash))).
                build();
    }

    public int getTransactionCount() {
        return blockBuilder.getDataCount();
    }


}
