package rmf;

import afu.org.checkerframework.checker.oigj.qual.O;
import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import crypto.pkiUtils;
import crypto.rmfDigSig;
import crypto.sslUtils;
import io.grpc.*;
import io.grpc.netty.NettyServerBuilder;
import proto.Data;
import proto.Meta;
import proto.RmfResult;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.lang.String.format;

public class RmfNode extends Node{
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RmfNode.class);

    class authInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                     Metadata metadata,
                                                                     ServerCallHandler<ReqT, RespT> serverCallHandler) {
            ServerCall.Listener res = new ServerCall.Listener() {};
            try {
                String peerDomain = serverCall.getAttributes()
                        .get(Grpc.TRANSPORT_ATTR_SSL_SESSION).getPeerPrincipal().getName().split("=")[1];
                int peerId = Integer.parseInt(Objects.requireNonNull(metadata.get
                        (Metadata.Key.of("id", ASCII_STRING_MARSHALLER))));
                if (rmfService.nodes.get(peerId).getAddr().equals(peerDomain)) {
                    res = serverCallHandler.startCall(serverCall, metadata);
                }
            } catch (SSLPeerUnverifiedException e) {
                logger.error("", e);
            } finally {
                return res;
            }

        }
    }

    protected boolean stopped = false;
    protected RmfService rmfService;
    protected Server rmfServer;

    public RmfNode(int id, String addr, int rmfPort, int f , ArrayList<Node> nodes, String bbcConfig) {
        super(addr, rmfPort, -1,  id);
        rmfService = new RmfService(id, f, nodes, bbcConfig);
        startGrpcServer();
    }

    private void startGrpcServer() {
        try {
            rmfServer = NettyServerBuilder.
                    forPort(getRmfPort()).
                    sslContext(sslUtils.buildSslContextForServer(Config.getServerCrtPath(),
                            Config.getCaRootPath(), Config.getServerTlsPrivKeyPath())).
                    addService(rmfService).
                    intercept(new authInterceptor()).
                    build().
                    start();
//            rmfServer = ServerBuilder.forPort(getRmfPort())
//                    // Enable TLS
////                    .useTransportSecurity(new File(serverCertPath), new File(serverKey))
//                    .addService(rmfService)
//                    .build().start();
        } catch (IOException e) {
            logger.fatal("", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (stopped) return;
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                logger.warn("*** shutting down gRPC server since JVM is shutting down");
                RmfNode.this.stop();
                logger.warn("*** server shut down");
            }
        });
    }

    public void stop() {
        stopped = true;
        if (rmfService != null)
            rmfService.shutdown();
        if (rmfServer != null)
            rmfServer.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        rmfService.shutdown();
        if (rmfServer != null) {
            rmfServer.awaitTermination();
        }

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
        RmfResult res = RmfResult.
                newBuilder().
                setCid(cid).
                setCidSeries(cidSeries).
                setData(data == null ? ByteString.EMPTY : data.getData()).
                build();
//        cid++;
        return res;
    }

    public String getRmfDataSig(int cidSeries, int cid) {
        return rmfService.getMessageSig(cidSeries, cid);
    }

//    public RmfResult nonBlockingDeliver(int cid) {
//        Data data = rmfService.nonBlockingDeliver(cid);
//        return RmfResult.
//                newBuilder().
//                setCid(cid).
//                setData(data == null ? ByteString.EMPTY : data.getData()).
//                build();
//    }

//    public void updateCid(int nCid) {
//        cid = nCid;
//    }
//
//    public void cleanBuffers() {
//        rmfService.cleanBuffers(cid);
//    }

    public void suspendAllThreads() {

    }

}
