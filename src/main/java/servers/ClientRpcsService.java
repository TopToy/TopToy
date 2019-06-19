package servers;

import io.grpc.stub.StreamObserver;
import proto.crpcs.clientService.ClientServiceGrpc.*;
import proto.types.transaction.*;
import proto.types.client.*;

import static java.lang.String.format;

public class ClientRpcsService extends ClientServiceImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ClientRpcsService.class);

    private Top topServer;

    ClientRpcsService(Top topServer)  {
        this.topServer = topServer;
    }

    @Override
    public void write(Transaction request,
                      StreamObserver<TxID> responseObserver) {
        logger.debug(format("received write request from [%d]", request.getClientID()));
        TxID tid = topServer.addTransaction(request);
        responseObserver.onNext(tid);
        responseObserver.onCompleted();
    }

    @Override
    public void read(ReadReq request,
                     StreamObserver<Transaction> responseObserver) {
        logger.debug(format("received read request from [%d]", request.getCid()));
        Transaction tx = null;
        try {
            tx = topServer.getTransaction(request.getTid(), request.getBlocking());
        } catch (InterruptedException e) {
            logger.error(e);
        }
        if (tx == null) tx = Transaction.getDefaultInstance();
        responseObserver.onNext(tx);
        responseObserver.onCompleted();
    }

    @Override
    public void status(ReadReq request,
                      StreamObserver<TxStatus> responseObserver) {
        logger.debug(format("received status request from [%d]", request.getCid()));
        int sts = -1;
        try {
             sts = topServer.status(request.getTid(), request.getBlocking());
        } catch (InterruptedException e) {
            logger.error(e);
        }
        responseObserver.onNext(TxStatus.newBuilder().setRes(sts).build());
        responseObserver.onCompleted();
    }
}
