// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: commService.proto

package proto;

public final class CommService {
  private CommService() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021commService.proto\022\005proto\032\013types.proto2" +
      "a\n\rCommunication\022\"\n\003dsm\022\013.proto.Comm\032\014.p" +
      "roto.Empty\"\000\022,\n\010reqBlock\022\016.proto.commReq" +
      "\032\016.proto.commRes\"\000B\007\n\005protob\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          proto.Types.getDescriptor(),
        }, assigner);
    proto.Types.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
