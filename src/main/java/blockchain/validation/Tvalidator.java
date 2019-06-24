package blockchain.validation;
import proto.types.transaction.*;
import proto.types.block.*;

public class Tvalidator implements Validator{
    @Override
    public boolean validateTX(Block.Builder b, Transaction tx) {
        return true;
    }
}
