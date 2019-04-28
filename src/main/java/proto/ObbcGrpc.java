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
    comments = "Source: obbc.proto")
public final class ObbcGrpc {

  private ObbcGrpc() {}

  public static final String SERVICE_NAME = "proto.Obbc";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<proto.Types.BbcMsg,
      proto.Types.Empty> getFastVoteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FastVote",
      requestType = proto.Types.BbcMsg.class,
      responseType = proto.Types.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.BbcMsg,
      proto.Types.Empty> getFastVoteMethod() {
    io.grpc.MethodDescriptor<proto.Types.BbcMsg, proto.Types.Empty> getFastVoteMethod;
    if ((getFastVoteMethod = ObbcGrpc.getFastVoteMethod) == null) {
      synchronized (ObbcGrpc.class) {
        if ((getFastVoteMethod = ObbcGrpc.getFastVoteMethod) == null) {
          ObbcGrpc.getFastVoteMethod = getFastVoteMethod = 
              io.grpc.MethodDescriptor.<proto.Types.BbcMsg, proto.Types.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Obbc", "FastVote"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.BbcMsg.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.Empty.getDefaultInstance()))
                  .setSchemaDescriptor(new ObbcMethodDescriptorSupplier("FastVote"))
                  .build();
          }
        }
     }
     return getFastVoteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<proto.Types.EvidenceReq,
      proto.Types.EvidenceRes> getEvidenceReqMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EvidenceReqMessage",
      requestType = proto.Types.EvidenceReq.class,
      responseType = proto.Types.EvidenceRes.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<proto.Types.EvidenceReq,
      proto.Types.EvidenceRes> getEvidenceReqMessageMethod() {
    io.grpc.MethodDescriptor<proto.Types.EvidenceReq, proto.Types.EvidenceRes> getEvidenceReqMessageMethod;
    if ((getEvidenceReqMessageMethod = ObbcGrpc.getEvidenceReqMessageMethod) == null) {
      synchronized (ObbcGrpc.class) {
        if ((getEvidenceReqMessageMethod = ObbcGrpc.getEvidenceReqMessageMethod) == null) {
          ObbcGrpc.getEvidenceReqMessageMethod = getEvidenceReqMessageMethod = 
              io.grpc.MethodDescriptor.<proto.Types.EvidenceReq, proto.Types.EvidenceRes>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Obbc", "EvidenceReqMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.EvidenceReq.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  proto.Types.EvidenceRes.getDefaultInstance()))
                  .setSchemaDescriptor(new ObbcMethodDescriptorSupplier("EvidenceReqMessage"))
                  .build();
          }
        }
     }
     return getEvidenceReqMessageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ObbcStub newStub(io.grpc.Channel channel) {
    return new ObbcStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ObbcBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ObbcBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ObbcFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ObbcFutureStub(channel);
  }

  /**
   */
  public static abstract class ObbcImplBase implements io.grpc.BindableService {

    /**
     */
    public void fastVote(proto.Types.BbcMsg request,
        io.grpc.stub.StreamObserver<proto.Types.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getFastVoteMethod(), responseObserver);
    }

    /**
     */
    public void evidenceReqMessage(proto.Types.EvidenceReq request,
        io.grpc.stub.StreamObserver<proto.Types.EvidenceRes> responseObserver) {
      asyncUnimplementedUnaryCall(getEvidenceReqMessageMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getFastVoteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.BbcMsg,
                proto.Types.Empty>(
                  this, METHODID_FAST_VOTE)))
          .addMethod(
            getEvidenceReqMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                proto.Types.EvidenceReq,
                proto.Types.EvidenceRes>(
                  this, METHODID_EVIDENCE_REQ_MESSAGE)))
          .build();
    }
  }

  /**
   */
  public static final class ObbcStub extends io.grpc.stub.AbstractStub<ObbcStub> {
    private ObbcStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ObbcStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ObbcStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ObbcStub(channel, callOptions);
    }

    /**
     */
    public void fastVote(proto.Types.BbcMsg request,
        io.grpc.stub.StreamObserver<proto.Types.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFastVoteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void evidenceReqMessage(proto.Types.EvidenceReq request,
        io.grpc.stub.StreamObserver<proto.Types.EvidenceRes> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEvidenceReqMessageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ObbcBlockingStub extends io.grpc.stub.AbstractStub<ObbcBlockingStub> {
    private ObbcBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ObbcBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ObbcBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ObbcBlockingStub(channel, callOptions);
    }

    /**
     */
    public proto.Types.Empty fastVote(proto.Types.BbcMsg request) {
      return blockingUnaryCall(
          getChannel(), getFastVoteMethod(), getCallOptions(), request);
    }

    /**
     */
    public proto.Types.EvidenceRes evidenceReqMessage(proto.Types.EvidenceReq request) {
      return blockingUnaryCall(
          getChannel(), getEvidenceReqMessageMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ObbcFutureStub extends io.grpc.stub.AbstractStub<ObbcFutureStub> {
    private ObbcFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ObbcFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ObbcFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ObbcFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.Empty> fastVote(
        proto.Types.BbcMsg request) {
      return futureUnaryCall(
          getChannel().newCall(getFastVoteMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<proto.Types.EvidenceRes> evidenceReqMessage(
        proto.Types.EvidenceReq request) {
      return futureUnaryCall(
          getChannel().newCall(getEvidenceReqMessageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_FAST_VOTE = 0;
  private static final int METHODID_EVIDENCE_REQ_MESSAGE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ObbcImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ObbcImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_FAST_VOTE:
          serviceImpl.fastVote((proto.Types.BbcMsg) request,
              (io.grpc.stub.StreamObserver<proto.Types.Empty>) responseObserver);
          break;
        case METHODID_EVIDENCE_REQ_MESSAGE:
          serviceImpl.evidenceReqMessage((proto.Types.EvidenceReq) request,
              (io.grpc.stub.StreamObserver<proto.Types.EvidenceRes>) responseObserver);
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

  private static abstract class ObbcBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ObbcBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return proto.ObbcOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Obbc");
    }
  }

  private static final class ObbcFileDescriptorSupplier
      extends ObbcBaseDescriptorSupplier {
    ObbcFileDescriptorSupplier() {}
  }

  private static final class ObbcMethodDescriptorSupplier
      extends ObbcBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ObbcMethodDescriptorSupplier(String methodName) {
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
      synchronized (ObbcGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ObbcFileDescriptorSupplier())
              .addMethod(getFastVoteMethod())
              .addMethod(getEvidenceReqMessageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
