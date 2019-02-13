package blockchain;


import proto.Types;

public class SBlock extends BaseBlock {

    @Override
    public boolean validateTransaction(Types.Transaction t) {
        return true;
    }
}
