package rmf;

import com.google.protobuf.ByteString;
import config.Node;
import crypto.rmfDigSig;

import proto.Types.*;
import java.util.*;

public class ByzantineRmfNode extends RmfNode {
//    private boolean stopped = false;
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ByzantineRmfNode.class);

    public ByzantineRmfNode(int channels, int id, String addr, int rmfPort, int f, ArrayList<Node> nodes,
                            String bbcConfig, String serverCrt, String serverPrivKey, String caRoot) {
        super(channels, id, addr, rmfPort, f, nodes, bbcConfig, serverCrt, serverPrivKey, caRoot);
    }

    public void selectiveBroadcast(int channel, int cidSeries, int cid, byte[] msg, int height, List<Integer> ids) {
        Meta metaMsg = Meta.
                newBuilder().
                setSender(getID()).
              //  setHeight(height).
                setCid(cid).
                setCidSeries(cidSeries).
                setChannel(channel).
                build();
        Data.Builder dataMsg = Data.
                newBuilder().
                setData(ByteString.copyFrom(msg)).
                setMeta(metaMsg);
        Data dmsg = dataMsg.setSig(rmfDigSig.sign(dataMsg)).build();
        for (Map.Entry<Integer, RmfService.peer>  p: rmfService.peers.entrySet()) {
            if (ids.contains(p.getKey())) {
                logger.debug("sending message " + Arrays.toString(msg) + " to " + p.getKey() + " with height of " + height);
                rmfService.sendDataMessage(p.getValue().stub, dmsg);
            }

        }
    }
    /*
        This should used outside the rmf protocol (Note that the rmf protocol does not handle such failures)
     */
    public void devidedBroadcast(int channel, int cidSeries, int cid, List<byte[]> msgs, List<Integer> heights, List<List<Integer>> ids) {
        for (int i = 0 ; i < msgs.size() ; i++) {
            selectiveBroadcast(channel, cidSeries, cid, msgs.get(i), heights.get(i), ids.get(i));
        }
    }

}
