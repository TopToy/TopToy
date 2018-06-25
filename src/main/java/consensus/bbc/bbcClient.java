package consensus.bbc;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import protos.BbcProtos;

import java.util.Arrays;


public class bbcClient {
    int id;
    AsynchServiceProxy vpbcProxy;

    public bbcClient(int id, String configHome) {
        this.id = id;
        vpbcProxy = new AsynchServiceProxy(id, configHome);
    }

    public int propose(int vote, int consID) {
        BbcProtos.BbcMsg.Builder b = BbcProtos.BbcMsg.newBuilder();
        b.setClientID(id);
        b.setConsID(consID);
        b.setVote(vote);
        BbcProtos.BbcMsg msg= b.build();
        byte[] data = msg.toByteArray();
        vpbcProxy.invokeAsynchRequest(data, new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {

            }
        }, TOMMessageType.ORDERED_REQUEST);
//        byte[] cmd = ByteBuffer.allocate(Integer.SIZE/Byte.SIZE).putInt(consID).array(); // Blocks until ends
//        byte[] res = vpbcProxy.invokeUnordered(cmd);
//        int ret = ByteBuffer.wrap(res).getInt();
//        consID++;
//        return ret;
        return 0;
    }

    public void close() {
        vpbcProxy.close();
    }
}
