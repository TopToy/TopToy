package wrb;

import config.Node;
//import crypto.rmfDigSig;

import proto.Types.*;
import java.util.ArrayList;

import static java.lang.String.format;

public class WrbNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WrbNode.class);

//    protected AtomicBoolean stopped = new AtomicBoolean(false);
    WrbService wrbService;

    public WrbNode(int channels, int id, String addr, int rmfPort, int f , int tmo, int tmoInterval, ArrayList<Node> nodes, String bbcConfig,
                   String serverCrt, String serverPrivKey, String caRoot) {
        super(addr, rmfPort,  id);
        wrbService = new WrbService(channels, id, f, tmo, tmoInterval, nodes, bbcConfig, serverCrt, serverPrivKey, caRoot);
    }

//    public WrbNode(int channel, int id, String addr, int rmfPort, int f , ArrayList<Node> nodes, bbcService bbc) {
//        super(addr, rmfPort,  id);
//        wrbService = new WrbService(channel, id, f, nodes, bbc);
//    }

    public void stop() {
//        stopped = true;
        if (wrbService != null)
            wrbService.shutdown();
    }

//    public void blockUntilShutdown() throws InterruptedException {
//        wrbService.shutdown();
//    }

    // This should be called only after all servers are running (as this object contains also the client logic)
    public void start() {
        wrbService.start();
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

        wrbService.rmfBroadcast(data);
    }

    public Block deliver(int channel, int cidSeries, int cid, int height, int sender, Block msg)
            throws InterruptedException {
//        Data dMsg = null;
//        if (msg != null) {
//            dMsg = buildData(msg, channel, cidSeries, cid + 1, height + 1, true);
//        }
        long start = System.currentTimeMillis();
        Block m = wrbService.deliver(channel, cidSeries, cid, sender, height, msg);
        logger.debug(format("[#%d-C[%d]] Deliver on wrb node took about %d [cidSeries=%d ; cid=%d]", getID(), channel,
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
//        return wrbService.getMessageSig(channel, cidSeries, cid);
//    }
    public void clearBuffers(Meta key) {
        wrbService.clearBuffers(key);
    }

    public long getTotolDec() {
        return wrbService.getTotalDeliveredTries();
    }

    public long getOptemisticDec() {
        return wrbService.getOptimialDec();
    }
}
