package blockchain;


import proto.Types;

public class basicBlock extends block {

    @Override
    boolean validateTransaction(Types.Transaction t) {
        return true;
    }
}
