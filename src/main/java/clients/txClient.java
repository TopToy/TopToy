package clients;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import proto.Types;
import proto.blockchainServiceGrpc;

import static java.lang.String.format;

public class txClient {
    blockchainServiceGrpc.blockchainServiceBlockingStub stub;
    ManagedChannel channel;
    int clientID;

    public txClient(int clientID, String addr, int port) {
        this.clientID = clientID;
        channel = ManagedChannelBuilder.forAddress(addr, port).usePlaintext().build();
        stub = blockchainServiceGrpc.newBlockingStub(channel);
    }

    public int getID() {
        return clientID;
    }

    public void addTx(byte[] data) {
//        System.out.println(format("Sending tx of size %d", data.length));
        Types.Transaction t = Types.Transaction.newBuilder()
                .setClientID(clientID)
                .setData(ByteString.copyFrom(data))
                .build();
        Types.accepted a = stub.addTransaction(t);
    }

    public void shutdown() {
        channel.shutdown();
    }
}
