package das.data;

import das.bbc.BbcService;
import proto.Types;

import java.util.concurrent.ConcurrentHashMap;

public class GlobalData {
    public static ConcurrentHashMap<Types.Meta, Boolean> bbcDec = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Types.Meta, Types.Block> pending = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Types.Meta, Boolean> received = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Types.Meta, VoteData> votes = new ConcurrentHashMap<>();
}
