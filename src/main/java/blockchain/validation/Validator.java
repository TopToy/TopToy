package blockchain.validation;

import proto.Types;

public interface Validator {
    public boolean validateTX(Types.Block.Builder b, Types.Transaction tx);
}
