package communication.data;

import proto.Types;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class Data {
    static public ConcurrentHashMap<Types.BlockID, Queue<Types.Block>>[][] blocks;

    public Data(int n, int channels) {
        blocks = new ConcurrentHashMap[n][channels];
        for (int i = 0 ; i < n ; i++) {
            for (int j = 0 ; j < channels ; j++) {
                blocks[i][j] = new ConcurrentHashMap<>();
            }

        }
    }

    static public void evacuateOldData(int channel, Types.BlockID bid) {
        int pid = bid.getPid();
        blocks[pid][channel].remove(bid);
    }

    static public void evacuateAllOldData(int channel, Types.BlockID bid) {
        int pid = bid.getPid();
        for (Types.BlockID id : blocks[pid][channel].keySet()) {
            if (id.getBid() < bid.getBid()) {
                blocks[pid][channel].remove(bid);
            }
        }
    }



}
