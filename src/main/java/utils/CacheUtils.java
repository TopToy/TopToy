package utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;
import proto.types.transaction.*;
import proto.types.block.*;

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

    static public void add(Transaction tx) {
        if (txCache.getIfPresent(tx.getId()) != null) return;
        txCache.put(tx.getId(), tx);
    }

    static public Transaction get(TxID id) {
        return (Transaction) txCache.getIfPresent(id);

    }

    static public void addBlock(Block b) {
        for (Transaction tx : b.getDataList()) {
            add(tx.toBuilder().build());
        }
    }

    static public boolean contains(TxID tid) {
        return (txCache.getIfPresent(tid) != null);
    }

}
