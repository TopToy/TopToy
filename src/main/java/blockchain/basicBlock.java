package blockchain;


import proto.Types;

public class basicBlock extends block {

    @Override
    public boolean validateTransaction(Types.Transaction t) {
        return true;
    }
}
