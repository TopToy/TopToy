package rmf;

import com.google.protobuf.ByteString;
import config.Node;
import crypto.pkiUtils;
import crypto.rmfDigSig;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.commons.lang.ArrayUtils;
import proto.Data;
import proto.Meta;
import proto.RmfResult;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collector;

/*
    TODO:
    1. Currently we still don't support a byzantine behaviour in the bbc inner protocol.
 */
public class ByzantineRmfNode extends RmfNode {
//    private boolean stopped = false;
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ByzantineRmfNode.class);

    public ByzantineRmfNode(int id, String addr, int rmfPort, int f, ArrayList<Node> nodes, String bbcConfig) {
        super(id, addr, rmfPort, f, nodes, bbcConfig);
    }

    public void selectiveBroadcast(int cidSeries, int cid, byte[] msg, int height, List<Integer> ids) {
        Meta metaMsg = Meta.
                newBuilder().
                setSender(getID()).
                setHeight(height).
                setCid(cid).
                setCidSeries(cidSeries).
                build();
        Data.Builder dataMsg = Data.
                newBuilder().
                setData(ByteString.copyFrom(msg)).
                setMeta(metaMsg);
        Data dmsg = dataMsg.setSig(rmfDigSig.sign(dataMsg)).build();
        for (Map.Entry<Integer, RmfService.peer>  p: rmfService.peers.entrySet()) {
            if (ids.contains(p.getKey())) {
                logger.info("sending message " + Arrays.toString(msg) + " to " + p.getKey() + " with height of " + height);
                rmfService.sendDataMessage(p.getValue().stub, dmsg);
            }

        }
    }
    /*
        This should used outside the rmf protocol (Note that the rmf protocol does not handle such failures)
     */
    public void devidedBroadcast(int cidSeries, int cid, List<byte[]> msgs, List<Integer> heights, List<List<Integer>> ids) {
        for (int i = 0 ; i < msgs.size() ; i++) {
            selectiveBroadcast(cidSeries, cid, msgs.get(i), heights.get(i), ids.get(i));
        }
    }

}
