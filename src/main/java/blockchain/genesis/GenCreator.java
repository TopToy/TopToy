package blockchain.genesis;

import proto.Types;

public interface GenCreator {
    Types.Block createGenesisBlock();
}
