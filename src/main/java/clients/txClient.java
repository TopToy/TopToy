package clients;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import proto.Types;
import proto.blockchainServiceGrpc;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class txClient {
    blockchainServiceGrpc.blockchainServiceBlockingStub stub;
    ManagedChannel channel;
    int clientID;
    boolean stopped = false;
    int txCount = 0;
    long startTime;
    double txPs;

    public txClient(int clientID, String addr, int port) {
        this.clientID = clientID;
        channel = ManagedChannelBuilder.forAddress(addr, port).usePlaintext().build();
        stub = blockchainServiceGrpc.newBlockingStub(channel);

    }
    public int getTxCount() {
        return txCount;
    }

    public double getAvgTx() {
        return txPs;
    }

    public int getID() {
        return clientID;
    }

    public int addTx(byte[] data) {
        if (txCount  == 0) {
            startTime = System.currentTimeMillis();
        }
        try {
            Types.Transaction t = Types.Transaction.newBuilder()
                    .setClientID(clientID)
                    .setData(ByteString.copyFrom(data))
                    .build();
            if (stub.addTransaction(t).getAccepted()) {
                txCount++;
            };
        } catch (Exception e) {
//            System.out.println(e.getMessage());
            return -1;
        }
        return 0;
//        System.out.println(format("Sending tx of size %d", data.length));

    }

    public double shutdown() {
        if (stopped) return txPs;
        long diff = (System.currentTimeMillis() - startTime ) / 1000;
        txPs = ((double) txCount / (double) diff);
        stopped = true;
        channel.shutdown();
        return txPs;
    }
}
