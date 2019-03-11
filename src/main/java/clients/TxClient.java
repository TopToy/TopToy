package clients;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import proto.Types;
import proto.blockchainServiceGrpc;

public class TxClient {
    blockchainServiceGrpc.blockchainServiceBlockingStub stub;
    ManagedChannel channel;
    int clientID;
    boolean stopped = false;
    int txCount = 0;
    long startTime;
    double txPs;

    public TxClient(int clientID, String addr, int port) {
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

    public Types.accepted addTx(byte[] data) {
        if (txCount  == 0) {
            startTime = System.currentTimeMillis();
        }
        try {
            Types.Transaction t = Types.Transaction.newBuilder()
                    .setClientID(clientID)
                    .setData(ByteString.copyFrom(data))
                    .setClientTs(System.currentTimeMillis())
                    .build();
            Types.accepted ret = stub.addTransaction(t);
            if (ret.getAccepted()) {
                txCount++;
            }
            return ret;
        } catch (Exception e) {
//            System.out.println(e.getMessage());
            return null;
        }
    }

    public Types.approved getTx(Types.read r) {
        return stub.getTransaction(r);
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
