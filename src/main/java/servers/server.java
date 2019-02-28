package servers;

import proto.Types;

import java.util.List;

public interface server {
    void start();
    void serve();
    void shutdown();
    Types.txID addTransaction(byte[] data, int clientID);
    int isTxPresent(Types.txID tid);
    int getID();
    int getBCSize();
    Types.Block nonBlockingDeliver(int index);
    void setByzSetting(boolean fullByz, List<List<Integer>> groups);
    void setAsyncParam(int time);
    Statistics getStatistics();

}
