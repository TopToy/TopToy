// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: client.proto

package proto;

public final class Client {
  private Client() {}
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
      "\n\014client.proto\022\005proto\032\013types.proto2\226\001\n\rc" +
      "lientService\022*\n\005write\022\022.proto.Transactio" +
      "n\032\013.proto.txID\"\000\022,\n\004read\022\016.proto.readReq" +
      "\032\022.proto.Transaction\"\000\022+\n\006status\022\016.proto" +
      ".readReq\032\017.proto.txStatus\"\000B\007\n\005protob\006pr" +
      "oto3"
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