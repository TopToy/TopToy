package consensus.vpbc;

import bftsmart.tom.ServiceProxy;
import com.google.protobuf.InvalidProtocolBufferException;
import proto.VpbcProtos;

public class vpbcClient {
    int id;
    ServiceProxy vpbcProxy;

    public vpbcClient(int id, String configHome) {
        this.id = id;
        vpbcProxy = new ServiceProxy(id, configHome);
    }

    public byte[] propose(VpbcProtos.VpbcMsg msg) throws InvalidProtocolBufferException {
        byte[] data = msg.toByteArray();
        byte[] ret = vpbcProxy.invokeOrdered(data);
        VpbcProtos.VpbcMsg m = VpbcProtos.VpbcMsg.parseFrom(ret);
        return m.getData().toByteArray();
    }

    public void close() {
        vpbcProxy.close();
    }

}
