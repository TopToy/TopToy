package servers;

import blockchain.data.BCS;
import io.grpc.stub.StreamObserver;
import proto.crpcs.clientService.ClientServiceGrpc.*;
import proto.types.block.*;
import proto.types.transaction.*;
import proto.types.client.*;
import proto.types.utils;
import utils.customException.ClientException;
import static java.lang.String.format;
import static utils.Config.*;

public class ClientRpcsService extends ClientServiceImplBase {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ClientRpcsService.class);

    private Top topServer;

    ClientRpcsService(Top topServer)  {
        this.topServer = topServer;
    }

    @Override
    public void writeTx(Transaction request,
                      StreamObserver<TxID> responseObserver) {
        logger.debug(format("received txWrite request from [%d]", request.getClientID()));
        TxID tid = topServer.addTransaction(request);
        responseObserver.onNext(tid);
        responseObserver.onCompleted();
    }

    @Override
    public void readTx(TxReq request,
                     StreamObserver<Transaction> responseObserver) {
        logger.debug(format("received read request from [%d]", request.getCid()));
        try {
            Transaction tx = topServer.getTransaction(request.getTid(), request.getBlocking());
            responseObserver.onNext(tx);
        } catch (InterruptedException e) {
            logger.error(e);
            responseObserver.onError(new ClientException("Server was interrupted while handling the request"));
        } finally {
            responseObserver.onCompleted();
        }

    }

    @Override
    public void txStatus(TxReq request,
                      StreamObserver<TxStatus> responseObserver) {
        logger.debug(format("received status request from [%d]", request.getCid()));
        try {
            TxState sts = topServer.status(request.getTid(), request.getBlocking());
            responseObserver.onNext(TxStatus.newBuilder().setStatus(sts).build());
        } catch (InterruptedException e) {
            logger.error(e);
            responseObserver.onError(new ClientException("Server was interrupted while handling the request"));
        } finally {
            responseObserver.onCompleted();
        }


    }

    @Override
    public void readBlock(BlockReq request,
                          StreamObserver<Block> responseObserver) {
        try {
            Block b = topServer.deliver(request.getHeight(), request.getBlocking());
            responseObserver.onNext(b);
        } catch (InterruptedException e) {
            logger.error(e);
            responseObserver.onError(new ClientException("Server was interrupted while handling the request"));
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void isAlive(utils.Empty request,
                        StreamObserver<utils.Empty> responseObserver) {
        responseObserver.onNext(utils.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void poolSize(utils.Empty request,
                         StreamObserver<utils.Integer> responseObserver) {
        responseObserver.onNext(utils.Integer.newBuilder().setNum(topServer.poolSize()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getHeight(utils.Empty request,
                         StreamObserver<utils.Integer> responseObserver) {
        responseObserver.onNext(utils.Integer.newBuilder().setNum(BCS.height()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void pendingSize(utils.Empty request,
                         StreamObserver<utils.Integer> responseObserver) {
        responseObserver.onNext(utils.Integer.newBuilder().setNum(topServer.pendingSize()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getValidators(utils.Empty request,
                            StreamObserver<Validators> responseObserver) {
        Validators.Builder v = Validators.newBuilder();
        for (int i = 0 ; i < getC() ; i++) {
            v.addIps(getIP(i));
        }
        responseObserver.onNext(v.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getConfigInfo(utils.Empty request,
                              StreamObserver<ConfigInfo> responseObserver) {
        responseObserver.onNext(ConfigInfo.newBuilder()
                .setClusterSize(getN())
                .setMaxFailures(getF())
                .setServerID(topServer.getID())
                .setWorkers(getC())
                .setInitTmo(getTMO())
                .setMaxTxInBlock(getMaxTransactionsInBlock())
                .build());
        responseObserver.onCompleted();
    }


}
