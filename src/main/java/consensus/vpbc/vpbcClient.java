package consensus.vpbc;

import bftsmart.tom.ServiceProxy;
import org.apache.commons.lang.SerializationUtils;

public class vpbcClient {
    int id;
    ServiceProxy vpbcProxy;

    public vpbcClient(int id, String configHome) {
        this.id = id;
        vpbcProxy = new ServiceProxy(id, configHome);
    }

    public byte[] propose(vpbcMessage msg) {
        byte[] data = SerializationUtils.serialize(msg);
        byte[] ret = vpbcProxy.invokeOrdered(data);
        vpbcMessage m = (vpbcMessage) SerializationUtils.deserialize(ret);
        return m.data;
    }

    public void close() {
        vpbcProxy.close();
    }

}
