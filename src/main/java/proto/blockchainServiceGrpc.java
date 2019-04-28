package proto;

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
    comments = "Source: blockchainService.proto")
public final class blockchainServiceGrpc {

  private blockchainServiceGrpc() {}

  public static final String SERVICE_NAME = "proto.blockchainService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.Types.Transaction,
      proto.Types.accepted> getAddTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "addTransaction",
      requestType = proto.Types.Transaction.class,
      responseType = proto.Types.accepted.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.Transaction,
      proto.Types.accepted> getAddTransactionMethod() {
    io.grpc.MethodDescriptor<proto.Types.Transaction, proto.Types.accepted> getAddTransactionMethod;
    if ((getAddTransactionMethod = blockchainServiceGrpc.getAddTransactionMethod) == null) {
      synchronized (blockchainServiceGrpc.class) {
        if ((getAddTransactionMethod = blockchainServiceGrpc.getAddTransactionMethod) == null) {
          blockchainServiceGrpc.getAddTransactionMethod = getAddTransactionMethod = 
              io.grpc.MethodDescriptor.<proto.Types.Transaction, proto.Types.accepted>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.blockchainService", "addTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.accepted.getDefaultInstance()))
                  .setSchemaDescriptor(new blockchainServiceMethodDescriptorSupplier("addTransaction"))
                  .build();
          }
        }
     }
     return getAddTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.Types.read,
      proto.Types.approved> getGetTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getTransaction",
      requestType = proto.Types.read.class,
      responseType = proto.Types.approved.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.read,
      proto.Types.approved> getGetTransactionMethod() {
    io.grpc.MethodDescriptor<proto.Types.read, proto.Types.approved> getGetTransactionMethod;
    if ((getGetTransactionMethod = blockchainServiceGrpc.getGetTransactionMethod) == null) {
      synchronized (blockchainServiceGrpc.class) {
        if ((getGetTransactionMethod = blockchainServiceGrpc.getGetTransactionMethod) == null) {
          blockchainServiceGrpc.getGetTransactionMethod = getGetTransactionMethod = 
              io.grpc.MethodDescriptor.<proto.Types.read, proto.Types.approved>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.blockchainService", "getTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.read.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.approved.getDefaultInstance()))
                  .setSchemaDescriptor(new blockchainServiceMethodDescriptorSupplier("getTransaction"))
                  .build();
          }
        }
     }
     return getGetTransactionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static blockchainServiceStub newStub(io.grpc.Channel channel) {
    return new blockchainServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static blockchainServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new blockchainServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static blockchainServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new blockchainServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class blockchainServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void addTransaction(proto.Types.Transaction request,
        io.grpc.stub.StreamObserver<proto.Types.accepted> responseObserver) {
      asyncUnimplementedUnaryCall(getAddTransactionMethod(), responseObserver);
    }

    /**
     */
    public void getTransaction(proto.Types.read request,
        io.grpc.stub.StreamObserver<proto.Types.approved> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getAddTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.Transaction,
                proto.Types.accepted>(
                  this, METHODID_ADD_TRANSACTION)))
          .addMethod(
            getGetTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.read,
                proto.Types.approved>(
                  this, METHODID_GET_TRANSACTION)))
          .build();
    }
  }

  /**
   */
  public static final class blockchainServiceStub extends io.grpc.stub.AbstractStub<blockchainServiceStub> {
    private blockchainServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private blockchainServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected blockchainServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new blockchainServiceStub(channel, callOptions);
    }

    /**
     */
    public void addTransaction(proto.Types.Transaction request,
        io.grpc.stub.StreamObserver<proto.Types.accepted> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransaction(proto.Types.read request,
        io.grpc.stub.StreamObserver<proto.Types.approved> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class blockchainServiceBlockingStub extends io.grpc.stub.AbstractStub<blockchainServiceBlockingStub> {
    private blockchainServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private blockchainServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected blockchainServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new blockchainServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.Types.accepted addTransaction(proto.Types.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getAddTransactionMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.Types.approved getTransaction(proto.Types.read request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class blockchainServiceFutureStub extends io.grpc.stub.AbstractStub<blockchainServiceFutureStub> {
    private blockchainServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private blockchainServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected blockchainServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new blockchainServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.accepted> addTransaction(
        proto.Types.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getAddTransactionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.approved> getTransaction(
        proto.Types.read request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADD_TRANSACTION = 0;
  private static final int METHODID_GET_TRANSACTION = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final blockchainServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(blockchainServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ADD_TRANSACTION:
          serviceImpl.addTransaction((proto.Types.Transaction) request,
              (io.grpc.stub.StreamObserver<proto.Types.accepted>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION:
          serviceImpl.getTransaction((proto.Types.read) request,
              (io.grpc.stub.StreamObserver<proto.Types.approved>) responseObserver);
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

  private static abstract class blockchainServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    blockchainServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.BlockchainService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("blockchainService");
    }
  }

  private static final class blockchainServiceFileDescriptorSupplier
      extends blockchainServiceBaseDescriptorSupplier {
    blockchainServiceFileDescriptorSupplier() {}
  }

  private static final class blockchainServiceMethodDescriptorSupplier
      extends blockchainServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    blockchainServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (blockchainServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new blockchainServiceFileDescriptorSupplier())
              .addMethod(getAddTransactionMethod())
              .addMethod(getGetTransactionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
