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

    public void add(Types.Transaction tx, int bid) {
        cache.put(tx.getId(), bid);
    }

    public int get(Types.txID id) {
        return (int) cache.getIfPresent(id);
    }

    public void addBlock(Types.Block b) {
        int bid = b.getHeader().getHeight();
        for (Types.Transaction tx : b.getDataList()) {
            add(tx, bid);
        }
    }
}
