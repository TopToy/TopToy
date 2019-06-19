package proto.prpcs.commService;

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
    comments = "Source: prpcs/commService.proto")
public final class CommunicationGrpc {

  private CommunicationGrpc() {}

  public static final String SERVICE_NAME = "proto.prpcs.commService.Communication";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.types.comm.Comm,
      proto.types.empty.Empty> getDsmMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dsm",
      requestType = proto.types.comm.Comm.class,
      responseType = proto.types.empty.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.comm.Comm,
      proto.types.empty.Empty> getDsmMethod() {
    io.grpc.MethodDescriptor<proto.types.comm.Comm, proto.types.empty.Empty> getDsmMethod;
    if ((getDsmMethod = CommunicationGrpc.getDsmMethod) == null) {
      synchronized (CommunicationGrpc.class) {
        if ((getDsmMethod = CommunicationGrpc.getDsmMethod) == null) {
          CommunicationGrpc.getDsmMethod = getDsmMethod = 
              io.grpc.MethodDescriptor.<proto.types.comm.Comm, proto.types.empty.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.prpcs.commService.Communication", "dsm"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.comm.Comm.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.empty.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new CommunicationMethodDescriptorSupplier("dsm"))
                  .build();
          }
        }
     }
     return getDsmMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.types.comm.CommReq,
      proto.types.comm.CommRes> getReqBlockMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "reqBlock",
      requestType = proto.types.comm.CommReq.class,
      responseType = proto.types.comm.CommRes.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.types.comm.CommReq,
      proto.types.comm.CommRes> getReqBlockMethod() {
    io.grpc.MethodDescriptor<proto.types.comm.CommReq, proto.types.comm.CommRes> getReqBlockMethod;
    if ((getReqBlockMethod = CommunicationGrpc.getReqBlockMethod) == null) {
      synchronized (CommunicationGrpc.class) {
        if ((getReqBlockMethod = CommunicationGrpc.getReqBlockMethod) == null) {
          CommunicationGrpc.getReqBlockMethod = getReqBlockMethod = 
              io.grpc.MethodDescriptor.<proto.types.comm.CommReq, proto.types.comm.CommRes>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.prpcs.commService.Communication", "reqBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.comm.CommReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.types.comm.CommRes.getDefaultInstance()))
                  .setSchemaDescriptor(new CommunicationMethodDescriptorSupplier("reqBlock"))
                  .build();
          }
        }
     }
     return getReqBlockMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CommunicationStub newStub(io.grpc.Channel channel) {
    return new CommunicationStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CommunicationBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new CommunicationBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CommunicationFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new CommunicationFutureStub(channel);
  }

  /**
   */
  public static abstract class CommunicationImplBase implements io.grpc.BindableService {

    /**
     */
    public void dsm(proto.types.comm.Comm request,
        io.grpc.stub.StreamObserver<proto.types.empty.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDsmMethod(), responseObserver);
    }

    /**
     */
    public void reqBlock(proto.types.comm.CommReq request,
        io.grpc.stub.StreamObserver<proto.types.comm.CommRes> responseObserver) {
      asyncUnimplementedUnaryCall(getReqBlockMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDsmMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.comm.Comm,
                proto.types.empty.Empty>(
                  this, METHODID_DSM)))
          .addMethod(
            getReqBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.types.comm.CommReq,
                proto.types.comm.CommRes>(
                  this, METHODID_REQ_BLOCK)))
          .build();
    }
  }

  /**
   */
  public static final class CommunicationStub extends io.grpc.stub.AbstractStub<CommunicationStub> {
    private CommunicationStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CommunicationStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CommunicationStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CommunicationStub(channel, callOptions);
    }

    /**
     */
    public void dsm(proto.types.comm.Comm request,
        io.grpc.stub.StreamObserver<proto.types.empty.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDsmMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reqBlock(proto.types.comm.CommReq request,
        io.grpc.stub.StreamObserver<proto.types.comm.CommRes> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReqBlockMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CommunicationBlockingStub extends io.grpc.stub.AbstractStub<CommunicationBlockingStub> {
    private CommunicationBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CommunicationBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CommunicationBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CommunicationBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.types.empty.Empty dsm(proto.types.comm.Comm request) {
      return blockingUnaryCall(
          getChannel(), getDsmMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.types.comm.CommRes reqBlock(proto.types.comm.CommReq request) {
      return blockingUnaryCall(
          getChannel(), getReqBlockMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CommunicationFutureStub extends io.grpc.stub.AbstractStub<CommunicationFutureStub> {
    private CommunicationFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CommunicationFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CommunicationFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CommunicationFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.empty.Empty> dsm(
        proto.types.comm.Comm request) {
      return futureUnaryCall(
          getChannel().newCall(getDsmMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.types.comm.CommRes> reqBlock(
        proto.types.comm.CommReq request) {
      return futureUnaryCall(
          getChannel().newCall(getReqBlockMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_DSM = 0;
  private static final int METHODID_REQ_BLOCK = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CommunicationImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(CommunicationImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DSM:
          serviceImpl.dsm((proto.types.comm.Comm) request,
              (io.grpc.stub.StreamObserver<proto.types.empty.Empty>) responseObserver);
          break;
        case METHODID_REQ_BLOCK:
          serviceImpl.reqBlock((proto.types.comm.CommReq) request,
              (io.grpc.stub.StreamObserver<proto.types.comm.CommRes>) responseObserver);
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

  private static abstract class CommunicationBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CommunicationBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.prpcs.commService.commService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Communication");
    }
  }

  private static final class CommunicationFileDescriptorSupplier
      extends CommunicationBaseDescriptorSupplier {
    CommunicationFileDescriptorSupplier() {}
  }

  private static final class CommunicationMethodDescriptorSupplier
      extends CommunicationBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    CommunicationMethodDescriptorSupplier(String methodName) {
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
      synchronized (CommunicationGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CommunicationFileDescriptorSupplier())
              .addMethod(getDsmMethod())
              .addMethod(getReqBlockMethod())
              .build();
        }
      }
    }
    return result;
  }
}
