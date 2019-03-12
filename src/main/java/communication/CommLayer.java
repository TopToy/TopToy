package communication;

import proto.Types;

public interface CommLayer {
    void broadcast(int channel, Types.Block data);
    Types.Block recMsg(int channel, Types.BlockID bid) throws InterruptedException;
    boolean contains(int channel, Types.BlockID bid, Types.BlockHeader proof);
    void join();
    void leave();
}
