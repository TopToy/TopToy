package proto.crpcs.clientService;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.20.0)",
    comments = "Source: crpcs/clientService.proto")
public final class ClientServiceGrpc {

  private ClientServiceGrpc() {}

  public static final String SERVICE_NAME = "proto.crpcs.clientService.ClientService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.types.transaction.Transaction,
      proto.types.transaction.TxID> getWriteTxMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "writeTx",
      requestType = proto.types.transaction.Transaction.class,
      responseType = proto.types.transaction.TxID.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.transaction.Transaction,
      proto.types.transaction.TxID> getWriteTxMethod() {
    io.grpc.MethodDescriptor<proto.types.transaction.Transaction, proto.types.transaction.TxID> getWriteTxMethod;
    if ((getWriteTxMethod = ClientServiceGrpc.getWriteTxMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getWriteTxMethod = ClientServiceGrpc.getWriteTxMethod) == null) {
          ClientServiceGrpc.getWriteTxMethod = getWriteTxMethod = 
              io.grpc.MethodDescriptor.<proto.types.transaction.Transaction, proto.types.transaction.TxID>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "writeTx"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.transaction.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.transaction.TxID.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("writeTx"))
                  .build();
          }
        }
     }
     return getWriteTxMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.client.TxReq,
      proto.types.transaction.Transaction> getReadTxMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "readTx",
      requestType = proto.types.client.TxReq.class,
      responseType = proto.types.transaction.Transaction.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.client.TxReq,
      proto.types.transaction.Transaction> getReadTxMethod() {
    io.grpc.MethodDescriptor<proto.types.client.TxReq, proto.types.transaction.Transaction> getReadTxMethod;
    if ((getReadTxMethod = ClientServiceGrpc.getReadTxMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getReadTxMethod = ClientServiceGrpc.getReadTxMethod) == null) {
          ClientServiceGrpc.getReadTxMethod = getReadTxMethod = 
              io.grpc.MethodDescriptor.<proto.types.client.TxReq, proto.types.transaction.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "readTx"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.TxReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.transaction.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("readTx"))
                  .build();
          }
        }
     }
     return getReadTxMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.client.TxReq,
      proto.types.client.TxStatus> getTxStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "txStatus",
      requestType = proto.types.client.TxReq.class,
      responseType = proto.types.client.TxStatus.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.client.TxReq,
      proto.types.client.TxStatus> getTxStatusMethod() {
    io.grpc.MethodDescriptor<proto.types.client.TxReq, proto.types.client.TxStatus> getTxStatusMethod;
    if ((getTxStatusMethod = ClientServiceGrpc.getTxStatusMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getTxStatusMethod = ClientServiceGrpc.getTxStatusMethod) == null) {
          ClientServiceGrpc.getTxStatusMethod = getTxStatusMethod = 
              io.grpc.MethodDescriptor.<proto.types.client.TxReq, proto.types.client.TxStatus>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "txStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.TxReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.TxStatus.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("txStatus"))
                  .build();
          }
        }
     }
     return getTxStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.client.BlockReq,
      proto.types.block.Block> getReadBlockMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "readBlock",
      requestType = proto.types.client.BlockReq.class,
      responseType = proto.types.block.Block.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.client.BlockReq,
      proto.types.block.Block> getReadBlockMethod() {
    io.grpc.MethodDescriptor<proto.types.client.BlockReq, proto.types.block.Block> getReadBlockMethod;
    if ((getReadBlockMethod = ClientServiceGrpc.getReadBlockMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getReadBlockMethod = ClientServiceGrpc.getReadBlockMethod) == null) {
          ClientServiceGrpc.getReadBlockMethod = getReadBlockMethod = 
              io.grpc.MethodDescriptor.<proto.types.client.BlockReq, proto.types.block.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "readBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.BlockReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.block.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("readBlock"))
                  .build();
          }
        }
     }
     return getReadBlockMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Integer> getGetHeightMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getHeight",
      requestType = proto.types.utils.Empty.class,
      responseType = proto.types.utils.Integer.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Integer> getGetHeightMethod() {
    io.grpc.MethodDescriptor<proto.types.utils.Empty, proto.types.utils.Integer> getGetHeightMethod;
    if ((getGetHeightMethod = ClientServiceGrpc.getGetHeightMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getGetHeightMethod = ClientServiceGrpc.getGetHeightMethod) == null) {
          ClientServiceGrpc.getGetHeightMethod = getGetHeightMethod = 
              io.grpc.MethodDescriptor.<proto.types.utils.Empty, proto.types.utils.Integer>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "getHeight"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Integer.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("getHeight"))
                  .build();
          }
        }
     }
     return getGetHeightMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Empty> getIsAliveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "isAlive",
      requestType = proto.types.utils.Empty.class,
      responseType = proto.types.utils.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Empty> getIsAliveMethod() {
    io.grpc.MethodDescriptor<proto.types.utils.Empty, proto.types.utils.Empty> getIsAliveMethod;
    if ((getIsAliveMethod = ClientServiceGrpc.getIsAliveMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getIsAliveMethod = ClientServiceGrpc.getIsAliveMethod) == null) {
          ClientServiceGrpc.getIsAliveMethod = getIsAliveMethod = 
              io.grpc.MethodDescriptor.<proto.types.utils.Empty, proto.types.utils.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "isAlive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("isAlive"))
                  .build();
          }
        }
     }
     return getIsAliveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Integer> getPoolSizeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "poolSize",
      requestType = proto.types.utils.Empty.class,
      responseType = proto.types.utils.Integer.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Integer> getPoolSizeMethod() {
    io.grpc.MethodDescriptor<proto.types.utils.Empty, proto.types.utils.Integer> getPoolSizeMethod;
    if ((getPoolSizeMethod = ClientServiceGrpc.getPoolSizeMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getPoolSizeMethod = ClientServiceGrpc.getPoolSizeMethod) == null) {
          ClientServiceGrpc.getPoolSizeMethod = getPoolSizeMethod = 
              io.grpc.MethodDescriptor.<proto.types.utils.Empty, proto.types.utils.Integer>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "poolSize"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Integer.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("poolSize"))
                  .build();
          }
        }
     }
     return getPoolSizeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Integer> getPendingSizeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "pendingSize",
      requestType = proto.types.utils.Empty.class,
      responseType = proto.types.utils.Integer.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.utils.Integer> getPendingSizeMethod() {
    io.grpc.MethodDescriptor<proto.types.utils.Empty, proto.types.utils.Integer> getPendingSizeMethod;
    if ((getPendingSizeMethod = ClientServiceGrpc.getPendingSizeMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getPendingSizeMethod = ClientServiceGrpc.getPendingSizeMethod) == null) {
          ClientServiceGrpc.getPendingSizeMethod = getPendingSizeMethod = 
              io.grpc.MethodDescriptor.<proto.types.utils.Empty, proto.types.utils.Integer>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "pendingSize"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Integer.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("pendingSize"))
                  .build();
          }
        }
     }
     return getPendingSizeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.client.Validators> getGetValidatorsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getValidators",
      requestType = proto.types.utils.Empty.class,
      responseType = proto.types.client.Validators.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.client.Validators> getGetValidatorsMethod() {
    io.grpc.MethodDescriptor<proto.types.utils.Empty, proto.types.client.Validators> getGetValidatorsMethod;
    if ((getGetValidatorsMethod = ClientServiceGrpc.getGetValidatorsMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getGetValidatorsMethod = ClientServiceGrpc.getGetValidatorsMethod) == null) {
          ClientServiceGrpc.getGetValidatorsMethod = getGetValidatorsMethod = 
              io.grpc.MethodDescriptor.<proto.types.utils.Empty, proto.types.client.Validators>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "getValidators"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.Validators.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("getValidators"))
                  .build();
          }
        }
     }
     return getGetValidatorsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.client.ConfigInfo> getGetConfigInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getConfigInfo",
      requestType = proto.types.utils.Empty.class,
      responseType = proto.types.client.ConfigInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.utils.Empty,
      proto.types.client.ConfigInfo> getGetConfigInfoMethod() {
    io.grpc.MethodDescriptor<proto.types.utils.Empty, proto.types.client.ConfigInfo> getGetConfigInfoMethod;
    if ((getGetConfigInfoMethod = ClientServiceGrpc.getGetConfigInfoMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getGetConfigInfoMethod = ClientServiceGrpc.getGetConfigInfoMethod) == null) {
          ClientServiceGrpc.getGetConfigInfoMethod = getGetConfigInfoMethod = 
              io.grpc.MethodDescriptor.<proto.types.utils.Empty, proto.types.client.ConfigInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "getConfigInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.ConfigInfo.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("getConfigInfo"))
                  .build();
          }
        }
     }
     return getGetConfigInfoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClientServiceStub newStub(io.grpc.Channel channel) {
    return new ClientServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClientServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ClientServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ClientServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ClientServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class ClientServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void writeTx(proto.types.transaction.Transaction request,
        io.grpc.stub.StreamObserver<proto.types.transaction.TxID> responseObserver) {
      asyncUnimplementedUnaryCall(getWriteTxMethod(), responseObserver);
    }

    /**
     */
    public void readTx(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.transaction.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getReadTxMethod(), responseObserver);
    }

    /**
     */
    public void txStatus(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.client.TxStatus> responseObserver) {
      asyncUnimplementedUnaryCall(getTxStatusMethod(), responseObserver);
    }

    /**
     */
    public void readBlock(proto.types.client.BlockReq request,
        io.grpc.stub.StreamObserver<proto.types.block.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getReadBlockMethod(), responseObserver);
    }

    /**
     */
    public void getHeight(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Integer> responseObserver) {
      asyncUnimplementedUnaryCall(getGetHeightMethod(), responseObserver);
    }

    /**
     */
    public void isAlive(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getIsAliveMethod(), responseObserver);
    }

    /**
     */
    public void poolSize(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Integer> responseObserver) {
      asyncUnimplementedUnaryCall(getPoolSizeMethod(), responseObserver);
    }

    /**
     */
    public void pendingSize(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Integer> responseObserver) {
      asyncUnimplementedUnaryCall(getPendingSizeMethod(), responseObserver);
    }

    /**
     */
    public void getValidators(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.client.Validators> responseObserver) {
      asyncUnimplementedUnaryCall(getGetValidatorsMethod(), responseObserver);
    }

    /**
     */
    public void getConfigInfo(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.client.ConfigInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetConfigInfoMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getWriteTxMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.transaction.Transaction,
                proto.types.transaction.TxID>(
                  this, METHODID_WRITE_TX)))
          .addMethod(
            getReadTxMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.client.TxReq,
                proto.types.transaction.Transaction>(
                  this, METHODID_READ_TX)))
          .addMethod(
            getTxStatusMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.client.TxReq,
                proto.types.client.TxStatus>(
                  this, METHODID_TX_STATUS)))
          .addMethod(
            getReadBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.client.BlockReq,
                proto.types.block.Block>(
                  this, METHODID_READ_BLOCK)))
          .addMethod(
            getGetHeightMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.utils.Empty,
                proto.types.utils.Integer>(
                  this, METHODID_GET_HEIGHT)))
          .addMethod(
            getIsAliveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.utils.Empty,
                proto.types.utils.Empty>(
                  this, METHODID_IS_ALIVE)))
          .addMethod(
            getPoolSizeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.utils.Empty,
                proto.types.utils.Integer>(
                  this, METHODID_POOL_SIZE)))
          .addMethod(
            getPendingSizeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.utils.Empty,
                proto.types.utils.Integer>(
                  this, METHODID_PENDING_SIZE)))
          .addMethod(
            getGetValidatorsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.utils.Empty,
                proto.types.client.Validators>(
                  this, METHODID_GET_VALIDATORS)))
          .addMethod(
            getGetConfigInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.utils.Empty,
                proto.types.client.ConfigInfo>(
                  this, METHODID_GET_CONFIG_INFO)))
          .build();
    }
  }

  /**
   */
  public static final class ClientServiceStub extends io.grpc.stub.AbstractStub<ClientServiceStub> {
    private ClientServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClientServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClientServiceStub(channel, callOptions);
    }

    /**
     */
    public void writeTx(proto.types.transaction.Transaction request,
        io.grpc.stub.StreamObserver<proto.types.transaction.TxID> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWriteTxMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void readTx(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.transaction.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadTxMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void txStatus(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.client.TxStatus> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTxStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void readBlock(proto.types.client.BlockReq request,
        io.grpc.stub.StreamObserver<proto.types.block.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadBlockMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getHeight(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Integer> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetHeightMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void isAlive(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getIsAliveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void poolSize(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Integer> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPoolSizeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pendingSize(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.utils.Integer> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPendingSizeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getValidators(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.client.Validators> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetValidatorsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getConfigInfo(proto.types.utils.Empty request,
        io.grpc.stub.StreamObserver<proto.types.client.ConfigInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetConfigInfoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ClientServiceBlockingStub extends io.grpc.stub.AbstractStub<ClientServiceBlockingStub> {
    private ClientServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClientServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClientServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.types.transaction.TxID writeTx(proto.types.transaction.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getWriteTxMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.transaction.Transaction readTx(proto.types.client.TxReq request) {
      return blockingUnaryCall(
          getChannel(), getReadTxMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.client.TxStatus txStatus(proto.types.client.TxReq request) {
      return blockingUnaryCall(
          getChannel(), getTxStatusMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.block.Block readBlock(proto.types.client.BlockReq request) {
      return blockingUnaryCall(
          getChannel(), getReadBlockMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.utils.Integer getHeight(proto.types.utils.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetHeightMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.utils.Empty isAlive(proto.types.utils.Empty request) {
      return blockingUnaryCall(
          getChannel(), getIsAliveMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.utils.Integer poolSize(proto.types.utils.Empty request) {
      return blockingUnaryCall(
          getChannel(), getPoolSizeMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.utils.Integer pendingSize(proto.types.utils.Empty request) {
      return blockingUnaryCall(
          getChannel(), getPendingSizeMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.client.Validators getValidators(proto.types.utils.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetValidatorsMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.client.ConfigInfo getConfigInfo(proto.types.utils.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetConfigInfoMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ClientServiceFutureStub extends io.grpc.stub.AbstractStub<ClientServiceFutureStub> {
    private ClientServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClientServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClientServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClientServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.transaction.TxID> writeTx(
        proto.types.transaction.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getWriteTxMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.transaction.Transaction> readTx(
        proto.types.client.TxReq request) {
      return futureUnaryCall(
          getChannel().newCall(getReadTxMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.client.TxStatus> txStatus(
        proto.types.client.TxReq request) {
      return futureUnaryCall(
          getChannel().newCall(getTxStatusMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.block.Block> readBlock(
        proto.types.client.BlockReq request) {
      return futureUnaryCall(
          getChannel().newCall(getReadBlockMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.utils.Integer> getHeight(
        proto.types.utils.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetHeightMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.utils.Empty> isAlive(
        proto.types.utils.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getIsAliveMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.utils.Integer> poolSize(
        proto.types.utils.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getPoolSizeMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.utils.Integer> pendingSize(
        proto.types.utils.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getPendingSizeMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.client.Validators> getValidators(
        proto.types.utils.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetValidatorsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.client.ConfigInfo> getConfigInfo(
        proto.types.utils.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetConfigInfoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_WRITE_TX = 0;
  private static final int METHODID_READ_TX = 1;
  private static final int METHODID_TX_STATUS = 2;
  private static final int METHODID_READ_BLOCK = 3;
  private static final int METHODID_GET_HEIGHT = 4;
  private static final int METHODID_IS_ALIVE = 5;
  private static final int METHODID_POOL_SIZE = 6;
  private static final int METHODID_PENDING_SIZE = 7;
  private static final int METHODID_GET_VALIDATORS = 8;
  private static final int METHODID_GET_CONFIG_INFO = 9;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ClientServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ClientServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_WRITE_TX:
          serviceImpl.writeTx((proto.types.transaction.Transaction) request,
              (io.grpc.stub.StreamObserver<proto.types.transaction.TxID>) responseObserver);
          break;
        case METHODID_READ_TX:
          serviceImpl.readTx((proto.types.client.TxReq) request,
              (io.grpc.stub.StreamObserver<proto.types.transaction.Transaction>) responseObserver);
          break;
        case METHODID_TX_STATUS:
          serviceImpl.txStatus((proto.types.client.TxReq) request,
              (io.grpc.stub.StreamObserver<proto.types.client.TxStatus>) responseObserver);
          break;
        case METHODID_READ_BLOCK:
          serviceImpl.readBlock((proto.types.client.BlockReq) request,
              (io.grpc.stub.StreamObserver<proto.types.block.Block>) responseObserver);
          break;
        case METHODID_GET_HEIGHT:
          serviceImpl.getHeight((proto.types.utils.Empty) request,
              (io.grpc.stub.StreamObserver<proto.types.utils.Integer>) responseObserver);
          break;
        case METHODID_IS_ALIVE:
          serviceImpl.isAlive((proto.types.utils.Empty) request,
              (io.grpc.stub.StreamObserver<proto.types.utils.Empty>) responseObserver);
          break;
        case METHODID_POOL_SIZE:
          serviceImpl.poolSize((proto.types.utils.Empty) request,
              (io.grpc.stub.StreamObserver<proto.types.utils.Integer>) responseObserver);
          break;
        case METHODID_PENDING_SIZE:
          serviceImpl.pendingSize((proto.types.utils.Empty) request,
              (io.grpc.stub.StreamObserver<proto.types.utils.Integer>) responseObserver);
          break;
        case METHODID_GET_VALIDATORS:
          serviceImpl.getValidators((proto.types.utils.Empty) request,
              (io.grpc.stub.StreamObserver<proto.types.client.Validators>) responseObserver);
          break;
        case METHODID_GET_CONFIG_INFO:
          serviceImpl.getConfigInfo((proto.types.utils.Empty) request,
              (io.grpc.stub.StreamObserver<proto.types.client.ConfigInfo>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ClientServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.crpcs.clientService.clientService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ClientService");
    }
  }

  private static final class ClientServiceFileDescriptorSupplier
      extends ClientServiceBaseDescriptorSupplier {
    ClientServiceFileDescriptorSupplier() {}
  }

  private static final class ClientServiceMethodDescriptorSupplier
      extends ClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ClientServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ClientServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ClientServiceFileDescriptorSupplier())
              .addMethod(getWriteTxMethod())
              .addMethod(getReadTxMethod())
              .addMethod(getTxStatusMethod())
              .addMethod(getReadBlockMethod())
              .addMethod(getGetHeightMethod())
              .addMethod(getIsAliveMethod())
              .addMethod(getPoolSizeMethod())
              .addMethod(getPendingSizeMethod())
              .addMethod(getGetValidatorsMethod())
              .addMethod(getGetConfigInfoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
