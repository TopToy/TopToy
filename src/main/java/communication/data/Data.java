package communication.data;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import proto.types.block.*;

public class Data {
    static public ConcurrentHashMap<BlockID, Queue<Block>>[][] blocks;

    public Data(int n, int channels) {
        blocks = new ConcurrentHashMap[n][channels];
        for (int i = 0 ; i < n ; i++) {
            for (int j = 0 ; j < channels ; j++) {
                blocks[i][j] = new ConcurrentHashMap<>();
            }

        }
    }

    static public void evacuateOldData(int channel, BlockID bid) {
        int pid = bid.getPid();
        synchronized (blocks[pid][channel]) {
            blocks[pid][channel].remove(bid);
        }

    }

    static public void evacuateAllOldData(int channel, BlockID bid) {
        int pid = bid.getPid();
        synchronized (blocks[pid][channel]) {
            for (BlockID id : blocks[pid][channel].keySet()) {
                if (id.getBid() < bid.getBid()) {
                    blocks[pid][channel].remove(bid);
                }
            }
        }

    }



}
