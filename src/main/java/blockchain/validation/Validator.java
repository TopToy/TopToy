package blockchain.validation;

import proto.Types;

public interface Validator {
    boolean validateTX(Types.Block.Builder b, Types.Transaction tx);
}
