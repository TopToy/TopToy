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
    comments = "Source: wrbService.proto")
public final class WrbGrpc {

  private WrbGrpc() {}

  public static final String SERVICE_NAME = "proto.Wrb";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.Types.BlockHeader,
      proto.Types.Empty> getDisseminateMessageMethod;

  public static io.grpc.MethodDescriptor<proto.Types.BlockHeader,
      proto.Types.Empty> getDisseminateMessageMethod() {
    io.grpc.MethodDescriptor<proto.Types.BlockHeader, proto.Types.Empty> getDisseminateMessageMethod;
    if ((getDisseminateMessageMethod = WrbGrpc.getDisseminateMessageMethod) == null) {
      synchronized (WrbGrpc.class) {
        if ((getDisseminateMessageMethod = WrbGrpc.getDisseminateMessageMethod) == null) {
          WrbGrpc.getDisseminateMessageMethod = getDisseminateMessageMethod = 
              io.grpc.MethodDescriptor.<proto.Types.BlockHeader, proto.Types.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Wrb", "DisseminateMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.BlockHeader.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new WrbMethodDescriptorSupplier("DisseminateMessage"))
                  .build();
          }
        }
     }
     return getDisseminateMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.Types.WrbReq,
      proto.Types.WrbRes> getReqMessageMethod;

  public static io.grpc.MethodDescriptor<proto.Types.WrbReq,
      proto.Types.WrbRes> getReqMessageMethod() {
    io.grpc.MethodDescriptor<proto.Types.WrbReq, proto.Types.WrbRes> getReqMessageMethod;
    if ((getReqMessageMethod = WrbGrpc.getReqMessageMethod) == null) {
      synchronized (WrbGrpc.class) {
        if ((getReqMessageMethod = WrbGrpc.getReqMessageMethod) == null) {
          WrbGrpc.getReqMessageMethod = getReqMessageMethod = 
              io.grpc.MethodDescriptor.<proto.Types.WrbReq, proto.Types.WrbRes>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Wrb", "reqMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.WrbReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.WrbRes.getDefaultInstance()))
                  .setSchemaDescriptor(new WrbMethodDescriptorSupplier("reqMessage"))
                  .build();
          }
        }
     }
     return getReqMessageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WrbStub newStub(io.grpc.Channel channel) {
    return new WrbStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WrbBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WrbBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WrbFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WrbFutureStub(channel);
  }

  /**
   */
  public static abstract class WrbImplBase implements io.grpc.BindableService {

    /**
     */
    public void disseminateMessage(proto.Types.BlockHeader request,
        io.grpc.stub.StreamObserver<proto.Types.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDisseminateMessageMethod(), responseObserver);
    }

    /**
     * <pre>
     *    rpc FastVote(BbcMsg) returns (Empty) {};
     * </pre>
     */
    public void reqMessage(proto.Types.WrbReq request,
        io.grpc.stub.StreamObserver<proto.Types.WrbRes> responseObserver) {
      asyncUnimplementedUnaryCall(getReqMessageMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDisseminateMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.BlockHeader,
                proto.Types.Empty>(
                  this, METHODID_DISSEMINATE_MESSAGE)))
          .addMethod(
            getReqMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.WrbReq,
                proto.Types.WrbRes>(
                  this, METHODID_REQ_MESSAGE)))
          .build();
    }
  }

  /**
   */
  public static final class WrbStub extends io.grpc.stub.AbstractStub<WrbStub> {
    private WrbStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WrbStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WrbStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WrbStub(channel, callOptions);
    }

    /**
     */
    public void disseminateMessage(proto.Types.BlockHeader request,
        io.grpc.stub.StreamObserver<proto.Types.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisseminateMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *    rpc FastVote(BbcMsg) returns (Empty) {};
     * </pre>
     */
    public void reqMessage(proto.Types.WrbReq request,
        io.grpc.stub.StreamObserver<proto.Types.WrbRes> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReqMessageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class WrbBlockingStub extends io.grpc.stub.AbstractStub<WrbBlockingStub> {
    private WrbBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WrbBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WrbBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WrbBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.Types.Empty disseminateMessage(proto.Types.BlockHeader request) {
      return blockingUnaryCall(
          getChannel(), getDisseminateMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *    rpc FastVote(BbcMsg) returns (Empty) {};
     * </pre>
     */
    public proto.Types.WrbRes reqMessage(proto.Types.WrbReq request) {
      return blockingUnaryCall(
          getChannel(), getReqMessageMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class WrbFutureStub extends io.grpc.stub.AbstractStub<WrbFutureStub> {
    private WrbFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WrbFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WrbFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WrbFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.Empty> disseminateMessage(
        proto.Types.BlockHeader request) {
      return futureUnaryCall(
          getChannel().newCall(getDisseminateMessageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *    rpc FastVote(BbcMsg) returns (Empty) {};
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.WrbRes> reqMessage(
        proto.Types.WrbReq request) {
      return futureUnaryCall(
          getChannel().newCall(getReqMessageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_DISSEMINATE_MESSAGE = 0;
  private static final int METHODID_REQ_MESSAGE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WrbImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WrbImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DISSEMINATE_MESSAGE:
          serviceImpl.disseminateMessage((proto.Types.BlockHeader) request,
              (io.grpc.stub.StreamObserver<proto.Types.Empty>) responseObserver);
          break;
        case METHODID_REQ_MESSAGE:
          serviceImpl.reqMessage((proto.Types.WrbReq) request,
              (io.grpc.stub.StreamObserver<proto.Types.WrbRes>) responseObserver);
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

  private static abstract class WrbBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WrbBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.WrbService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Wrb");
    }
  }

  private static final class WrbFileDescriptorSupplier
      extends WrbBaseDescriptorSupplier {
    WrbFileDescriptorSupplier() {}
  }

  private static final class WrbMethodDescriptorSupplier
      extends WrbBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WrbMethodDescriptorSupplier(String methodName) {
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
      synchronized (WrbGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WrbFileDescriptorSupplier())
              .addMethod(getDisseminateMessageMethod())
              .addMethod(getReqMessageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
