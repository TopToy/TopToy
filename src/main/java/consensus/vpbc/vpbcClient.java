package consensus.vpbc;

import bftsmart.tom.ServiceProxy;
import com.google.protobuf.InvalidProtocolBufferException;

import proto.Types.*;

public class vpbcClient {
    int id;
    ServiceProxy vpbcProxy;

    public vpbcClient(int id, String configHome) {
        this.id = id;
        vpbcProxy = new ServiceProxy(id, configHome);
    }

    public byte[] propose(VpbcMsg msg) throws InvalidProtocolBufferException {
        byte[] data = msg.toByteArray();
        byte[] ret = vpbcProxy.invokeOrdered(data);
        VpbcMsg m = VpbcMsg.parseFrom(ret);
        return m.getData().toByteArray();
    }

    public void close() {
        vpbcProxy.close();
    }

}
