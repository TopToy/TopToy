package blockchain;

import proto.Blockchain;

import java.util.ArrayList;
import java.util.List;

public abstract class abstractBlock {
    Blockchain.Block.Builder blockBuilder;

    public abstractBlock(int creatorID, int prevHash) {
        blockBuilder = Blockchain.Block.newBuilder();
        blockBuilder.setCreatorID(creatorID).setPrevHash(prevHash);
    }

    abstract boolean validateTransaction(Blockchain.Transaction t);

    public int getPrevHash() {
        return blockBuilder.getPrevHash();
    }

    public Blockchain.Transaction getTransaction(int index) {
        return blockBuilder.getData(index);
    }

    public ArrayList<Blockchain.Transaction> getAllTransactions() {
        ArrayList<Blockchain.Transaction> ret = new ArrayList<>();
        blockBuilder.addAllData(ret);
        return ret;
    }

    public int getCreatorID() {
        return blockBuilder.getCreatorID();
    }

    public void addTransaction(Blockchain.Transaction t) throws Exception {
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

    public Blockchain.Block getBlock() {
        return blockBuilder.build();
    }


}
