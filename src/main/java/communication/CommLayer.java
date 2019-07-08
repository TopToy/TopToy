package communication;
import proto.types.block.*;


public interface CommLayer {
    void broadcast(int channel, Block data);
    void send(int chanel, Block data, int[] recipients);
    Block recBlock(int channel, BlockHeader proof) throws InterruptedException;
    boolean contains(int channel, BlockHeader proof);
    void join();
    void leave();
    void reconfigure();
}
