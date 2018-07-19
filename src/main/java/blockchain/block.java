package blockchain;

import com.google.protobuf.ByteString;
import crypto.DigestMethod;
import proto.Block;
import proto.BlockHeader;
import proto.Crypto;
import proto.Transaction;

import java.util.ArrayList;
import java.util.List;

public abstract class block {
    private Block.Builder blockBuilder = Block.newBuilder();

    abstract boolean validateTransaction(Transaction t);

//    public Crypto.Digest getPrevHash() {
//        return blockBuilder.getHeader().getPrev();
//    }

    public Transaction getTransaction(int index) {
        return blockBuilder.getData(index);
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> ret = new ArrayList<>();
        blockBuilder.addAllData(ret);
        return ret;
    }

//    public int getCreatorID() {
//        return blockBuilder.getHeader().getCreatorID();
//    }

    public void addTransaction(Transaction t) {
            blockBuilder.addData(t);
    }

    public void removeTransaction(int index) {
        blockBuilder.removeData(index);
    }

    public Block construct(int creatorID, int height, byte[] prevHash) {

        return blockBuilder.setHeader(blockBuilder.
                getHeaderBuilder().
                setCreatorID(creatorID).
                setHeight(height).
                setPrev(ByteString.copyFrom(prevHash))).
                build();
    }

    public int getTransactionCount() {
        return blockBuilder.getDataCount();
    }


}
