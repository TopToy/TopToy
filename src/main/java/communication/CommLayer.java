package communication;

import proto.Types;

public interface CommLayer {
    void broadcast(int channel, Types.Block data);
    Types.Block recBlock(int channel, Types.BlockID bid, Types.BlockHeader proof) throws InterruptedException;
    boolean contains(int channel, Types.BlockID bid, Types.BlockHeader proof);
    void join();
    void leave();
}
