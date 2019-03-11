package blockchain.validation;

import proto.Types;

public class Tvalidator implements Validator{
    @Override
    public boolean validateTX(Types.Block.Builder b, Types.Transaction tx) {
        return true;
    }
}
