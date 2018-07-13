package blockchain;

import proto.Block;
import proto.Transaction;

import java.util.ArrayList;
import java.util.List;

public abstract class block {
    Block.Builder blockBuilder;

    public block(int creatorID) {
        blockBuilder = Block.newBuilder();
        blockBuilder.setCreatorID(creatorID);
    }

    abstract boolean validateTransaction(Transaction t);

    public int getPrevHash() {
        return blockBuilder.getPrevHash();
    }

    public Transaction getTransaction(int index) {
        return blockBuilder.getData(index);
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> ret = new ArrayList<>();
        blockBuilder.addAllData(ret);
        return ret;
    }

    public int getCreatorID() {
        return blockBuilder.getCreatorID();
    }

    public void addTransaction(Transaction t) {
            blockBuilder.addData(t);
    }

    public void removeTransaction(int index) {
        blockBuilder.removeData(index);
    }

    // TODO: Is it enough for hashing??
    public int getBlockHash() {
        return blockBuilder.hashCode();
    }

    public Block construct(int height, int prevHash) {

        return blockBuilder.setHeight(height).setPrevHash(prevHash).build();
    }

    public int getSize() {
        return blockBuilder.getDataCount();
    }


}
