package communication.data;

import proto.Types;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalData {
    static public ConcurrentHashMap<Types.BlockID, Queue<Types.Block>>[] blocks;

    public GlobalData(int channels) {
        blocks = new ConcurrentHashMap[channels];
        for (int i = 0 ; i < channels ; i++) {
            blocks[i] = new ConcurrentHashMap<>();
        }
    }
}
