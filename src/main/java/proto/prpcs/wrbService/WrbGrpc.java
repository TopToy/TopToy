package proto.prpcs.wrbService;

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
    comments = "Source: prpcs/wrbService.proto")
public final class WrbGrpc {

  private WrbGrpc() {}

  public static final String SERVICE_NAME = "proto.prpcs.wrbService.Wrb";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.types.block.BlockHeader,
      proto.types.utils.Empty> getDisseminateMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DisseminateMessage",
      requestType = proto.types.block.BlockHeader.class,
      responseType = proto.types.utils.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.block.BlockHeader,
      proto.types.utils.Empty> getDisseminateMessageMethod() {
    io.grpc.MethodDescriptor<proto.types.block.BlockHeader, proto.types.utils.Empty> getDisseminateMessageMethod;
    if ((getDisseminateMessageMethod = WrbGrpc.getDisseminateMessageMethod) == null) {
      synchronized (WrbGrpc.class) {
        if ((getDisseminateMessageMethod = WrbGrpc.getDisseminateMessageMethod) == null) {
          WrbGrpc.getDisseminateMessageMethod = getDisseminateMessageMethod = 
              io.grpc.MethodDescriptor.<proto.types.block.BlockHeader, proto.types.utils.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.prpcs.wrbService.Wrb", "DisseminateMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.block.BlockHeader.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.utils.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new WrbMethodDescriptorSupplier("DisseminateMessage"))
                  .build();
          }
        }
     }
     return getDisseminateMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.wrb.WrbReq,
      proto.types.wrb.WrbRes> getReqMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "reqMessage",
      requestType = proto.types.wrb.WrbReq.class,
      responseType = proto.types.wrb.WrbRes.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.wrb.WrbReq,
      proto.types.wrb.WrbRes> getReqMessageMethod() {
    io.grpc.MethodDescriptor<proto.types.wrb.WrbReq, proto.types.wrb.WrbRes> getReqMessageMethod;
    if ((getReqMessageMethod = WrbGrpc.getReqMessageMethod) == null) {
      synchronized (WrbGrpc.class) {
        if ((getReqMessageMethod = WrbGrpc.getReqMessageMethod) == null) {
          WrbGrpc.getReqMessageMethod = getReqMessageMethod = 
              io.grpc.MethodDescriptor.<proto.types.wrb.WrbReq, proto.types.wrb.WrbRes>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.prpcs.wrbService.Wrb", "reqMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.wrb.WrbReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.wrb.WrbRes.getDefaultInstance()))
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
    public void disseminateMessage(proto.types.block.BlockHeader request,
        io.grpc.stub.StreamObserver<proto.types.utils.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDisseminateMessageMethod(), responseObserver);
    }

    /**
     */
    public void reqMessage(proto.types.wrb.WrbReq request,
        io.grpc.stub.StreamObserver<proto.types.wrb.WrbRes> responseObserver) {
      asyncUnimplementedUnaryCall(getReqMessageMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDisseminateMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.block.BlockHeader,
                proto.types.utils.Empty>(
                  this, METHODID_DISSEMINATE_MESSAGE)))
          .addMethod(
            getReqMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.wrb.WrbReq,
                proto.types.wrb.WrbRes>(
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
    public void disseminateMessage(proto.types.block.BlockHeader request,
        io.grpc.stub.StreamObserver<proto.types.utils.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisseminateMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reqMessage(proto.types.wrb.WrbReq request,
        io.grpc.stub.StreamObserver<proto.types.wrb.WrbRes> responseObserver) {
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
    public proto.types.utils.Empty disseminateMessage(proto.types.block.BlockHeader request) {
      return blockingUnaryCall(
          getChannel(), getDisseminateMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.wrb.WrbRes reqMessage(proto.types.wrb.WrbReq request) {
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
    public com.google.common.util.concurrent.ListenableFuture<proto.types.utils.Empty> disseminateMessage(
        proto.types.block.BlockHeader request) {
      return futureUnaryCall(
          getChannel().newCall(getDisseminateMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.wrb.WrbRes> reqMessage(
        proto.types.wrb.WrbReq request) {
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
          serviceImpl.disseminateMessage((proto.types.block.BlockHeader) request,
              (io.grpc.stub.StreamObserver<proto.types.utils.Empty>) responseObserver);
          break;
        case METHODID_REQ_MESSAGE:
          serviceImpl.reqMessage((proto.types.wrb.WrbReq) request,
              (io.grpc.stub.StreamObserver<proto.types.wrb.WrbRes>) responseObserver);
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
      return proto.prpcs.wrbService.wrbService.getDescriptor();
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
