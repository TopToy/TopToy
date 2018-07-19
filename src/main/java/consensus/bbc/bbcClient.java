package consensus.bbc;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import proto.BbcProtos;


public class bbcClient {
    int id;
    AsynchServiceProxy bbcProxy;

    public bbcClient(int id, String configHome) {
        this.id = id;
        bbcProxy = new AsynchServiceProxy(id, configHome);
    }

    public int propose(int vote, int cid) {
        BbcProtos.BbcMsg.Builder b = BbcProtos.BbcMsg.newBuilder();
        b.setClientID(id);
        b.setId(cid);
        b.setVote(vote);
        BbcProtos.BbcMsg msg= b.build();
        byte[] data = msg.toByteArray();
        bbcProxy.invokeAsynchRequest(data, new ReplyListener() {
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
        bbcProxy.close();
    }
}
