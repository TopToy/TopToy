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
    comments = "Source: client.proto")
public final class clientServiceGrpc {

  private clientServiceGrpc() {}

  public static final String SERVICE_NAME = "proto.clientService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.Types.Transaction,
      proto.Types.txID> getWriteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "write",
      requestType = proto.Types.Transaction.class,
      responseType = proto.Types.txID.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.Transaction,
      proto.Types.txID> getWriteMethod() {
    io.grpc.MethodDescriptor<proto.Types.Transaction, proto.Types.txID> getWriteMethod;
    if ((getWriteMethod = clientServiceGrpc.getWriteMethod) == null) {
      synchronized (clientServiceGrpc.class) {
        if ((getWriteMethod = clientServiceGrpc.getWriteMethod) == null) {
          clientServiceGrpc.getWriteMethod = getWriteMethod = 
              io.grpc.MethodDescriptor.<proto.Types.Transaction, proto.Types.txID>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.clientService", "write"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.txID.getDefaultInstance()))
                  .setSchemaDescriptor(new clientServiceMethodDescriptorSupplier("write"))
                  .build();
          }
        }
     }
     return getWriteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.Types.readReq,
      proto.Types.Transaction> getReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "read",
      requestType = proto.Types.readReq.class,
      responseType = proto.Types.Transaction.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.readReq,
      proto.Types.Transaction> getReadMethod() {
    io.grpc.MethodDescriptor<proto.Types.readReq, proto.Types.Transaction> getReadMethod;
    if ((getReadMethod = clientServiceGrpc.getReadMethod) == null) {
      synchronized (clientServiceGrpc.class) {
        if ((getReadMethod = clientServiceGrpc.getReadMethod) == null) {
          clientServiceGrpc.getReadMethod = getReadMethod = 
              io.grpc.MethodDescriptor.<proto.Types.readReq, proto.Types.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.clientService", "read"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.readReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new clientServiceMethodDescriptorSupplier("read"))
                  .build();
          }
        }
     }
     return getReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.Types.readReq,
      proto.Types.txStatus> getStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "status",
      requestType = proto.Types.readReq.class,
      responseType = proto.Types.txStatus.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.readReq,
      proto.Types.txStatus> getStatusMethod() {
    io.grpc.MethodDescriptor<proto.Types.readReq, proto.Types.txStatus> getStatusMethod;
    if ((getStatusMethod = clientServiceGrpc.getStatusMethod) == null) {
      synchronized (clientServiceGrpc.class) {
        if ((getStatusMethod = clientServiceGrpc.getStatusMethod) == null) {
          clientServiceGrpc.getStatusMethod = getStatusMethod = 
              io.grpc.MethodDescriptor.<proto.Types.readReq, proto.Types.txStatus>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.clientService", "status"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.readReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.txStatus.getDefaultInstance()))
                  .setSchemaDescriptor(new clientServiceMethodDescriptorSupplier("status"))
                  .build();
          }
        }
     }
     return getStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static clientServiceStub newStub(io.grpc.Channel channel) {
    return new clientServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static clientServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new clientServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static clientServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new clientServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class clientServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void write(proto.Types.Transaction request,
        io.grpc.stub.StreamObserver<proto.Types.txID> responseObserver) {
      asyncUnimplementedUnaryCall(getWriteMethod(), responseObserver);
    }

    /**
     */
    public void read(proto.Types.readReq request,
        io.grpc.stub.StreamObserver<proto.Types.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getReadMethod(), responseObserver);
    }

    /**
     */
    public void status(proto.Types.readReq request,
        io.grpc.stub.StreamObserver<proto.Types.txStatus> responseObserver) {
      asyncUnimplementedUnaryCall(getStatusMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getWriteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.Transaction,
                proto.Types.txID>(
                  this, METHODID_WRITE)))
          .addMethod(
            getReadMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.readReq,
                proto.Types.Transaction>(
                  this, METHODID_READ)))
          .addMethod(
            getStatusMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.readReq,
                proto.Types.txStatus>(
                  this, METHODID_STATUS)))
          .build();
    }
  }

  /**
   */
  public static final class clientServiceStub extends io.grpc.stub.AbstractStub<clientServiceStub> {
    private clientServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private clientServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected clientServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new clientServiceStub(channel, callOptions);
    }

    /**
     */
    public void write(proto.Types.Transaction request,
        io.grpc.stub.StreamObserver<proto.Types.txID> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWriteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void read(proto.Types.readReq request,
        io.grpc.stub.StreamObserver<proto.Types.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void status(proto.Types.readReq request,
        io.grpc.stub.StreamObserver<proto.Types.txStatus> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class clientServiceBlockingStub extends io.grpc.stub.AbstractStub<clientServiceBlockingStub> {
    private clientServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private clientServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected clientServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new clientServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.Types.txID write(proto.Types.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getWriteMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.Types.Transaction read(proto.Types.readReq request) {
      return blockingUnaryCall(
          getChannel(), getReadMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.Types.txStatus status(proto.Types.readReq request) {
      return blockingUnaryCall(
          getChannel(), getStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class clientServiceFutureStub extends io.grpc.stub.AbstractStub<clientServiceFutureStub> {
    private clientServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private clientServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected clientServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new clientServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.txID> write(
        proto.Types.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getWriteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.Transaction> read(
        proto.Types.readReq request) {
      return futureUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.txStatus> status(
        proto.Types.readReq request) {
      return futureUnaryCall(
          getChannel().newCall(getStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_WRITE = 0;
  private static final int METHODID_READ = 1;
  private static final int METHODID_STATUS = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final clientServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(clientServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_WRITE:
          serviceImpl.write((proto.Types.Transaction) request,
              (io.grpc.stub.StreamObserver<proto.Types.txID>) responseObserver);
          break;
        case METHODID_READ:
          serviceImpl.read((proto.Types.readReq) request,
              (io.grpc.stub.StreamObserver<proto.Types.Transaction>) responseObserver);
          break;
        case METHODID_STATUS:
          serviceImpl.status((proto.Types.readReq) request,
              (io.grpc.stub.StreamObserver<proto.Types.txStatus>) responseObserver);
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

  private static abstract class clientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    clientServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.Client.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("clientService");
    }
  }

  private static final class clientServiceFileDescriptorSupplier
      extends clientServiceBaseDescriptorSupplier {
    clientServiceFileDescriptorSupplier() {}
  }

  private static final class clientServiceMethodDescriptorSupplier
      extends clientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    clientServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (clientServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new clientServiceFileDescriptorSupplier())
              .addMethod(getWriteMethod())
              .addMethod(getReadMethod())
              .addMethod(getStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
