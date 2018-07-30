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
 * <pre>
 *message empty {
 *}
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.13.1)",
    comments = "Source: blockchain.proto")
public final class blockchainServiceGrpc {

  private blockchainServiceGrpc() {}

  public static final String SERVICE_NAME = "proto.blockchainService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.Transaction,
      proto.accepted> getAddTransactionMethod;

  public static io.grpc.MethodDescriptor<proto.Transaction,
      proto.accepted> getAddTransactionMethod() {
    io.grpc.MethodDescriptor<proto.Transaction, proto.accepted> getAddTransactionMethod;
    if ((getAddTransactionMethod = blockchainServiceGrpc.getAddTransactionMethod) == null) {
      synchronized (blockchainServiceGrpc.class) {
        if ((getAddTransactionMethod = blockchainServiceGrpc.getAddTransactionMethod) == null) {
          blockchainServiceGrpc.getAddTransactionMethod = getAddTransactionMethod = 
              io.grpc.MethodDescriptor.<proto.Transaction, proto.accepted>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.blockchainService", "addTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.accepted.getDefaultInstance()))
                  .setSchemaDescriptor(new blockchainServiceMethodDescriptorSupplier("addTransaction"))
                  .build();
          }
        }
     }
     return getAddTransactionMethod;
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
   * <pre>
   *message empty {
   *}
   * </pre>
   */
  public static abstract class blockchainServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * BLocking call for client!
     * </pre>
     */
    public void addTransaction(proto.Transaction request,
        io.grpc.stub.StreamObserver<proto.accepted> responseObserver) {
      asyncUnimplementedUnaryCall(getAddTransactionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getAddTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Transaction,
                proto.accepted>(
                  this, METHODID_ADD_TRANSACTION)))
          .build();
    }
  }

  /**
   * <pre>
   *message empty {
   *}
   * </pre>
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
     * <pre>
     * BLocking call for client!
     * </pre>
     */
    public void addTransaction(proto.Transaction request,
        io.grpc.stub.StreamObserver<proto.accepted> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddTransactionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   *message empty {
   *}
   * </pre>
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
     * <pre>
     * BLocking call for client!
     * </pre>
     */
    public proto.accepted addTransaction(proto.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getAddTransactionMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   *message empty {
   *}
   * </pre>
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
     * <pre>
     * BLocking call for client!
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.accepted> addTransaction(
        proto.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getAddTransactionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADD_TRANSACTION = 0;

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
          serviceImpl.addTransaction((proto.Transaction) request,
              (io.grpc.stub.StreamObserver<proto.accepted>) responseObserver);
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
      return proto.Blockchain.getDescriptor();
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
              .build();
        }
      }
    }
    return result;
  }
}
