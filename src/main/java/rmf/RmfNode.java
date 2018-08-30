package rmf;

import com.google.protobuf.ByteString;
import config.Node;
import crypto.rmfDigSig;

import proto.Types.*;
import java.util.ArrayList;
import java.util.Base64;

import static java.lang.String.format;

public class RmfNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfNode.class);

    protected boolean stopped = false;
    RmfService rmfService;

    public RmfNode(int channel, int id, String addr, int rmfPort, int f , ArrayList<Node> nodes, String bbcConfig) {
        super(addr, rmfPort,  id);
        rmfService = new RmfService(channel, id, f, nodes, bbcConfig);
    }

    public void stop() {
        stopped = true;
        if (rmfService != null)
            rmfService.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        rmfService.shutdown();
    }

    // This should be called only after all servers are running (as this object contains also the client logic)
    public void start() {
        rmfService.start();
    }

    Data buildData(byte[] msg, int cidSeries, int cid, int height, boolean fm) {
        Meta metaMsg = Meta.
                newBuilder().
                setSender(getID()).
               // setHeight(height).
                setCid(cid).
                setCidSeries(cidSeries).
                build();
        Data.Builder dataMsg = Data.
                newBuilder().
                setData(ByteString.copyFrom(msg)).
                setMeta(metaMsg);
        if (!fm) {
            dataMsg = dataMsg.setSig(rmfDigSig.sign(dataMsg));
        }
        return dataMsg.build();
    }
    public void broadcast(int cidSeries, int cid, byte[] msg, int height) {
        logger.debug(format("[#%d] broadcasts data message with [height=%d]", getID(), height));

        rmfService.rmfBroadcast(buildData(msg, cidSeries, cid, height, false));
    }

    public byte[][] deliver(int cidSeries, int cid, int height, int sender, int tmo, byte[] msg) throws InterruptedException {
        Data dMsg = null;
        if (msg != null) {
            dMsg = buildData(msg, cidSeries, cid + 1, height + 1, true);
        }
        Data m = rmfService.deliver(cidSeries, cid, tmo, sender, height, dMsg);
        return (m == null ? new byte[][] {null, null} :
                new byte[][] {m.getData().toByteArray(), Base64.getDecoder().decode(m.getSig())});
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

    public String getRmfDataSig(int cidSeries, int cid) {
        return rmfService.getMessageSig(cidSeries, cid);
    }
}
