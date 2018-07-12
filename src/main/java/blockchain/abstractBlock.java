package blockchain;

import proto.Block;
import proto.Transaction;

import java.util.ArrayList;
import java.util.List;

public abstract class abstractBlock {
    Block.Builder blockBuilder;

    public abstractBlock(int height, int creatorID, int prevHash) {
        blockBuilder = Block.newBuilder();
        blockBuilder.setCreatorID(creatorID).setPrevHash(prevHash).setHeight(height);
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

    public void addTransaction(Transaction t) throws Exception {
        if (validateTransaction(t)) {
            blockBuilder.addData(t);
        } else {
            throw new Exception("Invalid Transaction");
        }
    }

    public void removeTransaction(int index) {
        blockBuilder.removeData(index);
    }

    // TODO: Is it enough for hashing??
    public int getBlockHash() {
        return blockBuilder.hashCode();
    }

    public Block construct() {

        return blockBuilder.build();
    }


}
