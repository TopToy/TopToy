package utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;
import proto.Types;

public class CacheUtils {
    static private int maxCacheSize = 1000;
    static private @NonNull Cache<Object, Object> txCache;

    public CacheUtils(int maxSize, int workers) {
        if (maxSize > 0) {
            CacheUtils.maxCacheSize = maxSize;
        }
        txCache = Caffeine
                .newBuilder()
                .maximumSize(maxCacheSize)
                .build();
    }

    static public void add(Types.Transaction tx) {
        if (txCache.getIfPresent(tx.getId()) != null) return;
        txCache.put(tx.getId(), tx);
    }

    static public Types.Transaction get(Types.txID id) {
        return (Types.Transaction) txCache.getIfPresent(id);

    }

    static public void addBlock(Types.Block b) {
        for (Types.Transaction tx : b.getDataList()) {
            add(tx.toBuilder().build());
        }
    }

    static public boolean contains(Types.txID tid) {
        return (txCache.getIfPresent(tid) != null);
    }

}
