package blockchain.validation;
import proto.types.transaction.*;
import proto.types.block.*;


public interface Validator {
    boolean validateTX(Block.Builder b, Transaction tx);
}
