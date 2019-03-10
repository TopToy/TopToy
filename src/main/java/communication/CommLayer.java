package communication;

public interface CommLayer {
    void broadcast(int channel, byte[] data);
    byte[] recMsg(int channel) throws InterruptedException;
    void join();
    void leave();
}
