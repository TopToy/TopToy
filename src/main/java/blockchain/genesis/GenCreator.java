package blockchain.genesis;

import proto.types.block.*;


public interface GenCreator {
    Block createGenesisBlock();
}
