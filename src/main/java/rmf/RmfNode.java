package rmf;

import com.google.protobuf.ByteString;
import config.Node;
import crypto.rmfDigSig;
import proto.Data;
import proto.Meta;
import proto.RmfResult;
import java.util.ArrayList;
import static java.lang.String.format;

public class RmfNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfNode.class);

    protected boolean stopped = false;
    protected RmfService rmfService;

    public RmfNode(int id, String addr, int rmfPort, int f , ArrayList<Node> nodes, String bbcConfig) {
        super(addr, rmfPort,  id);
        rmfService = new RmfService(id, f, nodes, bbcConfig);
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

    public void broadcast(int cidSeries, int cid, byte[] msg, int height) {
        logger.info(format("[#%d] broadcast data message with [height=%d]", getID(), height));
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
        rmfService.rmfBroadcast(dataMsg.setSig(rmfDigSig.sign(dataMsg)).build());
    }

    public RmfResult deliver(int cidSeries, int cid, int height, int sender, int tmo) throws InterruptedException {
        Data data = rmfService.deliver(cidSeries, cid, tmo, sender, height);
        if (data != null && data.getMeta().getCid() == -1) {
            return RmfResult.newBuilder().setCid(-1).build();
        }
        return RmfResult.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setData(data == null ? ByteString.EMPTY : data.getData()).
                build();
    }

    public String getRmfDataSig(int cidSeries, int cid) {
        return rmfService.getMessageSig(cidSeries, cid);
    }
}
