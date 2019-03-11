package blockchain.genesis;

import blockchain.Utils;
import proto.Types;

public class SGC implements GenCreator {
    @Override
    public Types.Block createGenesisBlock() {
        Types.Block gen = Types.Block.newBuilder().build();
        gen.toBuilder().setHeader(Utils.createBlockHeader(gen, null, -1, 0,
                -1, -1, -1, -1));
        return gen;
    }
}
