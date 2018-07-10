package rmf;

import com.google.protobuf.ByteString;
import config.Node;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import proto.Data;
import proto.Meta;

import java.io.IOException;
import java.util.ArrayList;

public class RmfNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfNode.class);

    private RmfService rmfService;
    private Server rmfServer;
    int height;
    public RmfNode(int id, String addr, int port, int f, int tmoInterval, int tmo, ArrayList<Node> nodes, String bbcConfig) {
        super(addr, port, id);
        rmfService = new RmfService(id, f, tmoInterval, tmo, nodes, bbcConfig);
        start();
//        height = 0;
    }

    private void start() {
        try {
            rmfServer = ServerBuilder.
                    forPort(getPort()).
                    addService(rmfService).
                    build().
                    start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                logger.warn("*** shutting down gRPC server since JVM is shutting down");
                RmfNode.this.stop();
                logger.warn("*** server shut down");
            }
        });
    }

    public void stop() {
        rmfServer.shutdown();
        rmfService.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (rmfServer != null) {
            rmfServer.awaitTermination();
        }
        rmfService.shutdown();
    }

    // This should be called only after all servers are running (as this object contains also the client logic)
    public void startService() {
        rmfService.start();
    }

    public void broadcast(byte[] msg, int height) {
        Meta metaMsg = Meta.
                newBuilder().
                setSender(getID()).
                setHeight(height).
                build();
        Data dataMsg = Data.
                newBuilder().
                setData(ByteString.copyFrom(msg)).
                setMeta(metaMsg).
                build();
        rmfService.rmfBroadcast(dataMsg);
    }

    public byte[] deliver(int height) {
        byte[] msg = rmfService.deliver(height);
        return msg;
    }


}
