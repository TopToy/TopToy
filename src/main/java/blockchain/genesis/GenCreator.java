package blockchain.genesis;

import proto.Types;

public interface GenCreator {
    public Types.Block createGenesisBlock();
}
