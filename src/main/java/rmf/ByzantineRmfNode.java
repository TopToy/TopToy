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

    public ByzantineRmfNode(int id, String addr, int port, int f, ArrayList<Node> nodes, String bbcConfig) {
        super(id, addr, port, f, nodes, bbcConfig);
    }

//    private RmfService rmfService;
//    private Server rmfServer;
//    private int cid = 0;
//
//    public ByzantineRmfNode(int id, String addr, int port, int f, ArrayList<Node> nodes, String bbcConfig) {
//        super(addr, port, id);
//        rmfService = new RmfService(id, f, nodes, bbcConfig);
//        startGrpcServer();
//    }
//
//    private void startGrpcServer() {
//        try {
//            rmfServer = ServerBuilder.
//                    forPort(getPort()).
//                    addService(rmfService).
//                    build().
//                    start();
//        } catch (IOException e) {
//            logger.error("", e);
//        }
//
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                if (stopped) return;
//                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
//                logger.warn("*** shutting down gRPC server since JVM is shutting down");
//                ByzantineRmfNode.this.stop();
//                logger.warn("*** server shut down");
//            }
//        });
//    }
//
//    public void stop() {
//        rmfServer.shutdown();
//        rmfService.shutdown();
//        stopped = true;
//    }
//
//    public void blockUntilShutdown() throws InterruptedException {
//        if (rmfServer != null) {
//            rmfServer.awaitTermination();
//        }
//        rmfService.shutdown();
//    }
//
//    // This should be called only after all servers are running (as this object contains also the client logic)
//    public void start() {
//        rmfService.start();
//    }
//
//    public void broadcast(byte[] msg, int height) {
//        Meta metaMsg = Meta.
//                newBuilder().
//                setSender(getID()).
//                setHeight(height).
//                setCid(cid).
//                build();
//        Data.Builder dataMsg = Data.
//                newBuilder().
//                setData(ByteString.copyFrom(msg)).
//                setMeta(metaMsg);
//        rmfService.rmfBroadcast(dataMsg.setSig(rmfDigSig.sign(dataMsg)).build());
//    }

//    public RmfResult deliver(int height, int sender, int tmo) {
//        Data data = rmfService.deliver(cid, tmo, sender, height);
//        RmfResult res = RmfResult.
//                newBuilder().
//                setCid(cid).
//                setData(data == null ? ByteString.EMPTY : data.getData()).
//                build();
//        cid++;
//        return res;
//    }

    public void selectiveBroadcast(byte[] msg, int height, List<Integer> ids) {
        Meta metaMsg = Meta.
                newBuilder().
                setSender(getID()).
                setHeight(height).
                setCid(cid).
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
    public void devidedBroadcast(List<byte[]> msgs, List<Integer> heights, List<List<Integer>> ids) {
        for (int i = 0 ; i < msgs.size() ; i++) {
            selectiveBroadcast(msgs.get(i), heights.get(i), ids.get(i));
        }
    }

//    public String getRmfDataSig(int cid) {
//        return rmfService.getMessageSig(cid);
//    }
//
//    public RmfResult nonBlockingDeliver(int cid) {
//        Data data = rmfService.nonBlockingDeliver(cid);
//        return RmfResult.
//                newBuilder().
//                setCid(cid).
//                setData(data == null ? ByteString.EMPTY : data.getData()).
//                build();
//    }

}
