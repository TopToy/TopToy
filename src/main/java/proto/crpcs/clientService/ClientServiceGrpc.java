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
      proto.types.transaction.TxID> getTxWriteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "txWrite",
      requestType = proto.types.transaction.Transaction.class,
      responseType = proto.types.transaction.TxID.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.transaction.Transaction,
      proto.types.transaction.TxID> getTxWriteMethod() {
    io.grpc.MethodDescriptor<proto.types.transaction.Transaction, proto.types.transaction.TxID> getTxWriteMethod;
    if ((getTxWriteMethod = ClientServiceGrpc.getTxWriteMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getTxWriteMethod = ClientServiceGrpc.getTxWriteMethod) == null) {
          ClientServiceGrpc.getTxWriteMethod = getTxWriteMethod = 
              io.grpc.MethodDescriptor.<proto.types.transaction.Transaction, proto.types.transaction.TxID>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "txWrite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.transaction.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.transaction.TxID.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("txWrite"))
                  .build();
          }
        }
     }
     return getTxWriteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.client.TxReq,
      proto.types.transaction.Transaction> getTxReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "txRead",
      requestType = proto.types.client.TxReq.class,
      responseType = proto.types.transaction.Transaction.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.client.TxReq,
      proto.types.transaction.Transaction> getTxReadMethod() {
    io.grpc.MethodDescriptor<proto.types.client.TxReq, proto.types.transaction.Transaction> getTxReadMethod;
    if ((getTxReadMethod = ClientServiceGrpc.getTxReadMethod) == null) {
      synchronized (ClientServiceGrpc.class) {
        if ((getTxReadMethod = ClientServiceGrpc.getTxReadMethod) == null) {
          ClientServiceGrpc.getTxReadMethod = getTxReadMethod = 
              io.grpc.MethodDescriptor.<proto.types.client.TxReq, proto.types.transaction.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.crpcs.clientService.ClientService", "txRead"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.client.TxReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.transaction.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new ClientServiceMethodDescriptorSupplier("txRead"))
                  .build();
          }
        }
     }
     return getTxReadMethod;
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
    public void txWrite(proto.types.transaction.Transaction request,
        io.grpc.stub.StreamObserver<proto.types.transaction.TxID> responseObserver) {
      asyncUnimplementedUnaryCall(getTxWriteMethod(), responseObserver);
    }

    /**
     */
    public void txRead(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.transaction.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getTxReadMethod(), responseObserver);
    }

    /**
     */
    public void txStatus(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.client.TxStatus> responseObserver) {
      asyncUnimplementedUnaryCall(getTxStatusMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getTxWriteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.transaction.Transaction,
                proto.types.transaction.TxID>(
                  this, METHODID_TX_WRITE)))
          .addMethod(
            getTxReadMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.client.TxReq,
                proto.types.transaction.Transaction>(
                  this, METHODID_TX_READ)))
          .addMethod(
            getTxStatusMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.client.TxReq,
                proto.types.client.TxStatus>(
                  this, METHODID_TX_STATUS)))
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
    public void txWrite(proto.types.transaction.Transaction request,
        io.grpc.stub.StreamObserver<proto.types.transaction.TxID> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTxWriteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void txRead(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.transaction.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTxReadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void txStatus(proto.types.client.TxReq request,
        io.grpc.stub.StreamObserver<proto.types.client.TxStatus> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTxStatusMethod(), getCallOptions()), request, responseObserver);
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
    public proto.types.transaction.TxID txWrite(proto.types.transaction.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getTxWriteMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.transaction.Transaction txRead(proto.types.client.TxReq request) {
      return blockingUnaryCall(
          getChannel(), getTxReadMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.client.TxStatus txStatus(proto.types.client.TxReq request) {
      return blockingUnaryCall(
          getChannel(), getTxStatusMethod(), getCallOptions(), request);
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
    public com.google.common.util.concurrent.ListenableFuture<proto.types.transaction.TxID> txWrite(
        proto.types.transaction.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getTxWriteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.transaction.Transaction> txRead(
        proto.types.client.TxReq request) {
      return futureUnaryCall(
          getChannel().newCall(getTxReadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.client.TxStatus> txStatus(
        proto.types.client.TxReq request) {
      return futureUnaryCall(
          getChannel().newCall(getTxStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TX_WRITE = 0;
  private static final int METHODID_TX_READ = 1;
  private static final int METHODID_TX_STATUS = 2;

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
        case METHODID_TX_WRITE:
          serviceImpl.txWrite((proto.types.transaction.Transaction) request,
              (io.grpc.stub.StreamObserver<proto.types.transaction.TxID>) responseObserver);
          break;
        case METHODID_TX_READ:
          serviceImpl.txRead((proto.types.client.TxReq) request,
              (io.grpc.stub.StreamObserver<proto.types.transaction.Transaction>) responseObserver);
          break;
        case METHODID_TX_STATUS:
          serviceImpl.txStatus((proto.types.client.TxReq) request,
              (io.grpc.stub.StreamObserver<proto.types.client.TxStatus>) responseObserver);
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
              .addMethod(getTxWriteMethod())
              .addMethod(getTxReadMethod())
              .addMethod(getTxStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
