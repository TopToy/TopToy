package das.data;

import proto.Types;

import java.util.concurrent.ConcurrentHashMap;

public class GlobalData {
    public static ConcurrentHashMap<Types.Meta, BbcDecData>[] bbcDec;
    public static ConcurrentHashMap<Types.Meta, Types.Block>[] pending;
    public static ConcurrentHashMap<Types.Meta, Types.Block>[] received;
    public static ConcurrentHashMap<Types.Meta, VoteData>[] votes;

    public GlobalData(int channels) {
        bbcDec = new ConcurrentHashMap[channels];
        pending = new ConcurrentHashMap[channels];
        received = new ConcurrentHashMap[channels];
        votes = new ConcurrentHashMap[channels];
        for (int i = 0 ; i < channels ; i++) {
            bbcDec[i] = new ConcurrentHashMap<>();
            pending[i] = new ConcurrentHashMap<>();
            received[i] = new ConcurrentHashMap<>();
            votes[i] = new ConcurrentHashMap<>();
        }
    }
}
