package blockchain.genesis;

import blockchain.Utils;
import proto.types.block.*;

public class SGC implements GenCreator {
    @Override
    public Block createGenesisBlock() {
        Block gen = Block.newBuilder().build();
        gen.toBuilder().setHeader(Utils.createBlockHeader(gen, null, -1, 0,
                -1, -1, -1, BlockID.newBuilder().setBid(-1).setPid(-1).build()));
        return gen;
    }
}
