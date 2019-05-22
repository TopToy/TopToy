package crypto;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.io.File;

public class SslUtils {

    public static SslContext buildSslContextForClient(String caCertFilePath,
                                             String clientCertFilePath,
                                             String clientPrivateKeyFilePath) throws SSLException {

        if (caCertFilePath.equals("")) {
            return GrpcSslContexts.
                    forClient().
                    trustManager(InsecureTrustManagerFactory.INSTANCE).
                    keyManager(new File(clientCertFilePath), new File(clientPrivateKeyFilePath)).
                    build();
        }
        return GrpcSslContexts.
                forClient().
                trustManager(new File(caCertFilePath)).
                keyManager(new File(clientCertFilePath), new File(clientPrivateKeyFilePath)).
                build();

    }

    public static ManagedChannelBuilder buildSslChannel(String host, int port, SslContext ctx) {
        return NettyChannelBuilder.forAddress(host, port)
                .negotiationType(NegotiationType.TLS)
                .sslContext(ctx);
    }

    public static SslContext buildSslContextForServer(String serverCertFilePath,
                                                      String caCertPath,
                                                      String serverPrivateKeyFilePath) throws SSLException {
        if (caCertPath.equals("")) {
            return GrpcSslContexts.configure(GrpcSslContexts.
                            forServer(new File(serverCertFilePath), new File(serverPrivateKeyFilePath)).
                            trustManager(InsecureTrustManagerFactory.INSTANCE). //new File(caCertPath)).
                            clientAuth(ClientAuth.REQUIRE)
                    , SslProvider.OPENSSL).
                    build();
        }
        return  GrpcSslContexts.configure(GrpcSslContexts.
                        forServer(new File(serverCertFilePath), new File(serverPrivateKeyFilePath)).
                        trustManager(new File(caCertPath)).
                        clientAuth(ClientAuth.REQUIRE)
                , SslProvider.OPENSSL).
                build();

    }
}
