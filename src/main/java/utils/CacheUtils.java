package utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;
import proto.Types;

public class CacheUtils {
    private int maxCacheSize = 100000;
    private @NonNull
    Cache<Object, Object> cache;

    public CacheUtils(int maxSize) {
        if (maxSize > 0) {
            this.maxCacheSize = maxSize;
        }
        cache = Caffeine
                .newBuilder()
                .maximumSize(maxCacheSize)
                .build();
    }

    public void add(Types.Transaction tx) {
        cache.put(tx.getId(), tx);
    }

    public Types.Transaction get(Types.txID id) {
        return (Types.Transaction) cache.getIfPresent(id);

    }

    public void addBlock(Types.Block b) {
        for (Types.Transaction tx : b.getDataList()) {
            add(tx);
        }
    }

    public boolean contains(Types.txID tid) {
        return (cache.getIfPresent(tid) != null);
    };
}
