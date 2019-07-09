package utils;

import proto.types.meta;

public class commons {

    static public meta.Meta createMeta(int worker, int cid, int cidSeries, int version) {
        return meta.Meta.newBuilder()
                .setChannel(worker)
                .setCid(cid)
                .setCidSeries(cidSeries)
                .setVersion(version)
                .build();
    }
}