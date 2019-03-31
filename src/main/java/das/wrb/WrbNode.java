package das.wrb;

import blockchain.Blockchain;
import communication.CommLayer;
import communication.data.GlobalData;
import config.Node;
import proto.Types;
//import crypto.rmfDigSig;

import java.util.ArrayList;
import java.util.Arrays;

import static crypto.blockDigSig.verfiyBlockWRTheader;
import static java.lang.String.format;

public class WrbNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WrbNode.class);

//    protected AtomicBoolean stopped = new AtomicBoolean(false);
    WrbService wrbService;

    public WrbNode(int channels, int id, String addr, int rmfPort, int f , int tmo, int tmoInterval,
                   ArrayList<Node> nodes, String bbcConfig, String serverCrt, String serverPrivKey,
                   String caRoot, CommLayer comm) {
        super(addr, rmfPort,  id);
        wrbService = new WrbService(channels, id, f, tmo, tmoInterval, nodes,
                bbcConfig, serverCrt, serverPrivKey, caRoot, comm);
    }

    public void setBcForChannel(int channel, Blockchain bc) {
        wrbService.setBcForChannel(channel, bc);
    }
    public void stop() {
        if (wrbService != null)
            wrbService.shutdown();
    }


    // This should be called only after all servers are running (as this object contains also the client logic)
    public void start() {
        wrbService.start();
    }

    public void broadcast(Types.BlockHeader data) {
        logger.debug(format("[#%d] broadcasts data message with [height=%d]", getID(), data.getHeight()));

        wrbService.wrbBroadcast(data);
    }

    // meant for Byzantine activity tests
    public void sned(Types.BlockHeader data, int[] recipients) {
        logger.debug(format("[#%d] send data message with [height=%d] to [%s]",
                getID(), data.getHeight(), Arrays.toString(recipients)));

        wrbService.wrbSend(data, recipients);
    }

    public Types.BlockHeader deliver(int channel, int cidSeries, int cid, int height, int sender, Types.BlockHeader msg)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        Types.BlockHeader h = wrbService.deliver(channel, cidSeries, cid, sender, height, msg);
        logger.debug(format("[#%d-C[%d]] Deliver on wrb node took about [%d] ms [cidSeries=%d ; cid=%d]", getID(), channel,
                System.currentTimeMillis() - start, cidSeries, cid));
        return h;
//        Types.Block b;
//        b = getBlockWRTheader(h, channel);
//        while (!verfiyBlockWRTheader(b, h)) {
//            b = getBlockWRTheader(h, channel);
//        }
//        return b;
    }

    public long getTotolDec() {
        return wrbService.getTotalDeliveredTries();
    }

    public long getOptemisticDec() {
        return wrbService.getOptimialDec();
    }
}
