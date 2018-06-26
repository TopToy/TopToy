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
    comments = "Source: rmfService.proto")
public final class RmfGrpc {

  private RmfGrpc() {}

  public static final String SERVICE_NAME = "proto.Rmf";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.Data,
      proto.Empty> getSendMessageMethod;

  public static io.grpc.MethodDescriptor<proto.Data,
      proto.Empty> getSendMessageMethod() {
    io.grpc.MethodDescriptor<proto.Data, proto.Empty> getSendMessageMethod;
    if ((getSendMessageMethod = RmfGrpc.getSendMessageMethod) == null) {
      synchronized (RmfGrpc.class) {
        if ((getSendMessageMethod = RmfGrpc.getSendMessageMethod) == null) {
          RmfGrpc.getSendMessageMethod = getSendMessageMethod = 
              io.grpc.MethodDescriptor.<proto.Data, proto.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Rmf", "sendMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Data.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new RmfMethodDescriptorSupplier("sendMessage"))
                  .build();
          }
        }
     }
     return getSendMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.Req,
      proto.Res> getReqMessageMethod;

  public static io.grpc.MethodDescriptor<proto.Req,
      proto.Res> getReqMessageMethod() {
    io.grpc.MethodDescriptor<proto.Req, proto.Res> getReqMessageMethod;
    if ((getReqMessageMethod = RmfGrpc.getReqMessageMethod) == null) {
      synchronized (RmfGrpc.class) {
        if ((getReqMessageMethod = RmfGrpc.getReqMessageMethod) == null) {
          RmfGrpc.getReqMessageMethod = getReqMessageMethod = 
              io.grpc.MethodDescriptor.<proto.Req, proto.Res>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Rmf", "reqMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Req.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Res.getDefaultInstance()))
                  .setSchemaDescriptor(new RmfMethodDescriptorSupplier("reqMessage"))
                  .build();
          }
        }
     }
     return getReqMessageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RmfStub newStub(io.grpc.Channel channel) {
    return new RmfStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RmfBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new RmfBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RmfFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new RmfFutureStub(channel);
  }

  /**
   */
  public static abstract class RmfImplBase implements io.grpc.BindableService {

    /**
     */
    public void sendMessage(proto.Data request,
        io.grpc.stub.StreamObserver<proto.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSendMessageMethod(), responseObserver);
    }

    /**
     */
    public void reqMessage(proto.Req request,
        io.grpc.stub.StreamObserver<proto.Res> responseObserver) {
      asyncUnimplementedUnaryCall(getReqMessageMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Data,
                proto.Empty>(
                  this, METHODID_SEND_MESSAGE)))
          .addMethod(
            getReqMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Req,
                proto.Res>(
                  this, METHODID_REQ_MESSAGE)))
          .build();
    }
  }

  /**
   */
  public static final class RmfStub extends io.grpc.stub.AbstractStub<RmfStub> {
    private RmfStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RmfStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RmfStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RmfStub(channel, callOptions);
    }

    /**
     */
    public void sendMessage(proto.Data request,
        io.grpc.stub.StreamObserver<proto.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reqMessage(proto.Req request,
        io.grpc.stub.StreamObserver<proto.Res> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReqMessageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class RmfBlockingStub extends io.grpc.stub.AbstractStub<RmfBlockingStub> {
    private RmfBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RmfBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RmfBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RmfBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.Empty sendMessage(proto.Data request) {
      return blockingUnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.Res reqMessage(proto.Req request) {
      return blockingUnaryCall(
          getChannel(), getReqMessageMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class RmfFutureStub extends io.grpc.stub.AbstractStub<RmfFutureStub> {
    private RmfFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RmfFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RmfFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RmfFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Empty> sendMessage(
        proto.Data request) {
      return futureUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Res> reqMessage(
        proto.Req request) {
      return futureUnaryCall(
          getChannel().newCall(getReqMessageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_MESSAGE = 0;
  private static final int METHODID_REQ_MESSAGE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RmfImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RmfImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_MESSAGE:
          serviceImpl.sendMessage((proto.Data) request,
              (io.grpc.stub.StreamObserver<proto.Empty>) responseObserver);
          break;
        case METHODID_REQ_MESSAGE:
          serviceImpl.reqMessage((proto.Req) request,
              (io.grpc.stub.StreamObserver<proto.Res>) responseObserver);
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

  private static abstract class RmfBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RmfBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.RmfService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Rmf");
    }
  }

  private static final class RmfFileDescriptorSupplier
      extends RmfBaseDescriptorSupplier {
    RmfFileDescriptorSupplier() {}
  }

  private static final class RmfMethodDescriptorSupplier
      extends RmfBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RmfMethodDescriptorSupplier(String methodName) {
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
      synchronized (RmfGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RmfFileDescriptorSupplier())
              .addMethod(getSendMessageMethod())
              .addMethod(getReqMessageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
