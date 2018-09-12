package rmf;

import com.google.protobuf.ByteString;
import config.Node;
import consensus.bbc.bbcService;
//import crypto.rmfDigSig;

import proto.Types.*;
import java.util.ArrayList;
import java.util.Base64;

import static java.lang.String.format;

public class RmfNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfNode.class);

    protected boolean stopped = false;
    RmfService rmfService;

    public RmfNode(int channels, int id, String addr, int rmfPort, int f , int tmo, int tmoInterval, ArrayList<Node> nodes, String bbcConfig,
                   String serverCrt, String serverPrivKey, String caRoot) {
        super(addr, rmfPort,  id);
        rmfService = new RmfService(channels, id, f, tmo, tmoInterval, nodes, bbcConfig, serverCrt, serverPrivKey, caRoot);
    }

//    public RmfNode(int channel, int id, String addr, int rmfPort, int f , ArrayList<Node> nodes, bbcService bbc) {
//        super(addr, rmfPort,  id);
//        rmfService = new RmfService(channel, id, f, nodes, bbc);
//    }

    public void stop() {
        stopped = true;
        if (rmfService != null)
            rmfService.shutdown();
    }

//    public void blockUntilShutdown() throws InterruptedException {
//        rmfService.shutdown();
//    }

    // This should be called only after all servers are running (as this object contains also the client logic)
    public void start() {
        rmfService.start();
    }

//    Data buildData(byte[] msg, int channel, int cidSeries, int cid, int height, boolean fm) {
//        Meta metaMsg = Meta.
//                newBuilder().
//                setSender(getID()).
//               // setHeight(height).
//                setCid(cid).
//                setCidSeries(cidSeries).
//                setChannel(channel).
//                build();
//        Data.Builder dataMsg = Data.
//                newBuilder().
//                setData(ByteString.copyFrom(msg)).
//                setMeta(metaMsg);
//        if (!fm) {
//            dataMsg = dataMsg.setSig(rmfDigSig.sign(dataMsg));
//        }
//        return dataMsg.build();
//    }
    public void broadcast(Block data) {
        logger.debug(format("[#%d] broadcasts data message with [height=%d]", getID(), data.getHeader().getHeight()));

        rmfService.rmfBroadcast(data);
    }

    public Block deliver(int channel, int cidSeries, int cid, int height, int sender, Block msg)
            throws InterruptedException {
//        Data dMsg = null;
//        if (msg != null) {
//            dMsg = buildData(msg, channel, cidSeries, cid + 1, height + 1, true);
//        }
        long start = System.currentTimeMillis();
        Block m = rmfService.deliver(channel, cidSeries, cid, sender, height, msg);
        logger.debug(format("[#%d-C[%d]] Deliver on rmf node took about %d [cidSeries=%d ; cid=%d]", getID(), channel,
                System.currentTimeMillis() - start, cidSeries, cid));
        return m;
//        Data data = (td == null ? null : td.d);
//        String type = (data == null ? "FULL" : td.t.name());
//        return RmfResult.
//                newBuilder().
//                setCid(cid).
//                setCidSeries(cidSeries).
//                setData(data == null ? ByteString.EMPTY : data.getData()).
//                setType(type).
//                build();
    }

//    public String getRmfDataSig(int channel, int cidSeries, int cid) {
//        return rmfService.getMessageSig(channel, cidSeries, cid);
//    }
    public void clearBuffers(Meta key) {
        rmfService.clearBuffers(key);
    }
}
