package servers;

import io.grpc.stub.StreamObserver;
import proto.Types;
import proto.clientServiceGrpc;

public class ClientRpcsService extends clientServiceGrpc.clientServiceImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ClientRpcsService.class);

    Top topServer;

    public ClientRpcsService(Top topServer)  {
        this.topServer = topServer;
    }

    @Override
    public void write(Types.Transaction request,
                      StreamObserver<Types.txID> responseObserver) {
        Types.txID tid = topServer.addTransaction(request);
        responseObserver.onNext(tid);
        responseObserver.onCompleted();
    }

    @Override
    public void read(Types.readReq request,
                     StreamObserver<proto.Types.Transaction> responseObserver) {
        Types.Transaction tx = null;
        try {
            tx = topServer.getTransaction(request.getTid(), request.getBlocking());
        } catch (InterruptedException e) {
            logger.error(e);
        }
        if (tx == null) tx = Types.Transaction.getDefaultInstance();
        responseObserver.onNext(tx);
        responseObserver.onCompleted();
    }

    @Override
    public void status(Types.readReq request,
                      StreamObserver<proto.Types.txStatus> responseObserver) {
        int sts = -1;
        try {
             sts = topServer.status(request.getTid(), request.getBlocking());
        } catch (InterruptedException e) {
            logger.error(e);
        }
        responseObserver.onNext(Types.txStatus.newBuilder().setRes(sts).build());
        responseObserver.onCompleted();
    }
}
