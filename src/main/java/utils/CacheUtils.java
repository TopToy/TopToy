package utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;
import proto.Types;

public class CacheUtils {
    static private int maxCacheSize = 1000;
    static private @NonNull Cache<Object, Object> txCache;
//    static private @NonNull Cache<Object, Object>[] blocksMappingCache;

    public CacheUtils(int maxSize, int workers) {
        if (maxSize > 0) {
            CacheUtils.maxCacheSize = maxSize;
        }
        txCache = Caffeine
                .newBuilder()
                .maximumSize(maxCacheSize)
                .build();
//        blocksMappingCache = new Cache[workers];
//        for (int i = 0; i < workers ; i++ ) {
//            blocksMappingCache[i] =  Caffeine
//                    .newBuilder()
//                    .maximumSize(100)
//                    .build();
//        }
    }

    static public void add(Types.Transaction tx) {
        if (txCache.getIfPresent(tx.getId()) != null) return;
        txCache.put(tx.getId(), tx);
    }

//    static public void add(Types.BlockID bid, int worker, int height) {
//        if (blocksMappingCache[worker].getIfPresent(bid) != null) return;
//        blocksMappingCache[worker].put(bid, height);
//    }

    static public Types.Transaction get(Types.txID id) {
        return (Types.Transaction) txCache.getIfPresent(id);

    }

//    static public Integer get(Types.BlockID bid, int worker) {
//        if (blocksMappingCache[worker].getIfPresent(bid) == null) return -1;
//        return (Integer) blocksMappingCache[worker].getIfPresent(bid);
//
//    }

    static public void addBlock(Types.Block b) {
        for (Types.Transaction tx : b.getDataList()) {
            add(tx.toBuilder().build());
        }
    }

    static public boolean contains(Types.txID tid) {
        return (txCache.getIfPresent(tid) != null);
    }
//
//    static public boolean contains(Types.BlockID bid, int worker) {
//        return (blocksMappingCache[worker].getIfPresent(bid) != null);
//    }
}
