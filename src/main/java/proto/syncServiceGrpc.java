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
    value = "by gRPC proto compiler (version 1.13.1)",
    comments = "Source: syncService.proto")
public final class syncServiceGrpc {

  private syncServiceGrpc() {}

  public static final String SERVICE_NAME = "proto.syncService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.subChainVersion,
      proto.Empty> getDisseminateVersionMethod;

  public static io.grpc.MethodDescriptor<proto.subChainVersion,
      proto.Empty> getDisseminateVersionMethod() {
    io.grpc.MethodDescriptor<proto.subChainVersion, proto.Empty> getDisseminateVersionMethod;
    if ((getDisseminateVersionMethod = syncServiceGrpc.getDisseminateVersionMethod) == null) {
      synchronized (syncServiceGrpc.class) {
        if ((getDisseminateVersionMethod = syncServiceGrpc.getDisseminateVersionMethod) == null) {
          syncServiceGrpc.getDisseminateVersionMethod = getDisseminateVersionMethod = 
              io.grpc.MethodDescriptor.<proto.subChainVersion, proto.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.syncService", "disseminateVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.subChainVersion.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new syncServiceMethodDescriptorSupplier("disseminateVersion"))
                  .build();
          }
        }
     }
     return getDisseminateVersionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static syncServiceStub newStub(io.grpc.Channel channel) {
    return new syncServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static syncServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new syncServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static syncServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new syncServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class syncServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void disseminateVersion(proto.subChainVersion request,
        io.grpc.stub.StreamObserver<proto.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDisseminateVersionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDisseminateVersionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.subChainVersion,
                proto.Empty>(
                  this, METHODID_DISSEMINATE_VERSION)))
          .build();
    }
  }

  /**
   */
  public static final class syncServiceStub extends io.grpc.stub.AbstractStub<syncServiceStub> {
    private syncServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private syncServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected syncServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new syncServiceStub(channel, callOptions);
    }

    /**
     */
    public void disseminateVersion(proto.subChainVersion request,
        io.grpc.stub.StreamObserver<proto.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisseminateVersionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class syncServiceBlockingStub extends io.grpc.stub.AbstractStub<syncServiceBlockingStub> {
    private syncServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private syncServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected syncServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new syncServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.Empty disseminateVersion(proto.subChainVersion request) {
      return blockingUnaryCall(
          getChannel(), getDisseminateVersionMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class syncServiceFutureStub extends io.grpc.stub.AbstractStub<syncServiceFutureStub> {
    private syncServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private syncServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected syncServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new syncServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Empty> disseminateVersion(
        proto.subChainVersion request) {
      return futureUnaryCall(
          getChannel().newCall(getDisseminateVersionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_DISSEMINATE_VERSION = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final syncServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(syncServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DISSEMINATE_VERSION:
          serviceImpl.disseminateVersion((proto.subChainVersion) request,
              (io.grpc.stub.StreamObserver<proto.Empty>) responseObserver);
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

  private static abstract class syncServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    syncServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.SyncService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("syncService");
    }
  }

  private static final class syncServiceFileDescriptorSupplier
      extends syncServiceBaseDescriptorSupplier {
    syncServiceFileDescriptorSupplier() {}
  }

  private static final class syncServiceMethodDescriptorSupplier
      extends syncServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    syncServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (syncServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new syncServiceFileDescriptorSupplier())
              .addMethod(getDisseminateVersionMethod())
              .build();
        }
      }
    }
    return result;
  }
}