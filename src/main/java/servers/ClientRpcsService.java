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
    public void txWrite(Transaction request,
                      StreamObserver<TxID> responseObserver) {
        logger.debug(format("received txWrite request from [%d]", request.getClientID()));
        TxID tid = topServer.addTransaction(request);
        if (tid == null) tid = TxID.getDefaultInstance();
        responseObserver.onNext(tid);
        responseObserver.onCompleted();
    }

    @Override
    public void txRead(TxReq request,
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
    public void txStatus(TxReq request,
                      StreamObserver<TxStatus> responseObserver) {
        logger.debug(format("received status request from [%d]", request.getCid()));
        TxState sts = TxState.UNRECOGNIZED;
        try {
             sts = topServer.status(request.getTid(), request.getBlocking());
        } catch (InterruptedException e) {
            logger.error(e);
        }
        responseObserver.onNext(TxStatus.newBuilder().setStatus(sts).build());
        responseObserver.onCompleted();
    }
}
