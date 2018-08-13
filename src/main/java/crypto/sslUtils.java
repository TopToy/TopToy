package crypto;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;
import java.io.File;

public class sslUtils {

    public static SslContext buildSslContextForClient(String caCertFilePath,
                                             String clientCertFilePath,
                                             String clientPrivateKeyFilePath) throws SSLException {

        return GrpcSslContexts.
                forClient().
                trustManager(new File(caCertFilePath)).
                keyManager(new File(clientCertFilePath), new File(clientPrivateKeyFilePath)).
                build();
    }

    public static ManagedChannel buildSslChannel(String host, int port, SslContext ctx) {
        return NettyChannelBuilder.forAddress(host, port)
                .negotiationType(NegotiationType.TLS)
//                .usePlaintext()
                .sslContext(ctx)
                .build();
    }

    public static SslContext buildSslContextForServer(String serverCertFilePath,
                                                      String caCertPath,
                                                      String serverPrivateKeyFilePath) throws SSLException {
        return GrpcSslContexts.configure(GrpcSslContexts.
                forServer(new File(serverCertFilePath), new File(serverPrivateKeyFilePath)).
                trustManager(new File(caCertPath)).
                clientAuth(ClientAuth.REQUIRE)
        , SslProvider.OPENSSL).
                build();
    }
}
