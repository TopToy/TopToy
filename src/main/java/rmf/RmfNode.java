package rmf;

import config.Node;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.ArrayList;

public class RmfNode extends Node{
    private RmfService rmfService;
    private Server rmfServer;
    public RmfNode(String addr, int port, int id, int f, int tmoInterval, int tmo, ArrayList<Node> nodes) {
        super(addr, port, id);
        rmfService = new RmfService(id, f, tmoInterval, tmo, nodes);
    }

    public void start() {
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
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RmfNode.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() {
        rmfServer.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (rmfServer != null) {
            rmfServer.awaitTermination();
        }
    }


}
