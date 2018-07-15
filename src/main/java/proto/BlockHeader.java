// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: blockchain.proto

package proto;

/**
 * Protobuf type {@code proto.BlockHeader}
 */
public  final class BlockHeader extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:proto.BlockHeader)
    BlockHeaderOrBuilder {
private static final long serialVersionUID = 0L;
  // Use BlockHeader.newBuilder() to construct.
  private BlockHeader(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private BlockHeader() {
    creatorID_ = 0;
    height_ = 0;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private BlockHeader(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownFieldProto3(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            proto.Crypto.Digest.Builder subBuilder = null;
            if (prev_ != null) {
              subBuilder = prev_.toBuilder();
            }
            prev_ = input.readMessage(proto.Crypto.Digest.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(prev_);
              prev_ = subBuilder.buildPartial();
            }

            break;
          }
          case 16: {

            creatorID_ = input.readInt32();
            break;
          }
          case 24: {

            height_ = input.readInt32();
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return proto.Blockchain.internal_static_proto_BlockHeader_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return proto.Blockchain.internal_static_proto_BlockHeader_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            proto.BlockHeader.class, proto.BlockHeader.Builder.class);
  }

  public static final int PREV_FIELD_NUMBER = 1;
  private proto.Crypto.Digest prev_;
  /**
   * <code>.proto.Digest prev = 1;</code>
   */
  public boolean hasPrev() {
    return prev_ != null;
  }
  /**
   * <code>.proto.Digest prev = 1;</code>
   */
  public proto.Crypto.Digest getPrev() {
    return prev_ == null ? proto.Crypto.Digest.getDefaultInstance() : prev_;
  }
  /**
   * <code>.proto.Digest prev = 1;</code>
   */
  public proto.Crypto.DigestOrBuilder getPrevOrBuilder() {
    return getPrev();
  }

  public static final int CREATORID_FIELD_NUMBER = 2;
  private int creatorID_;
  /**
   * <code>int32 creatorID = 2;</code>
   */
  public int getCreatorID() {
    return creatorID_;
  }

  public static final int HEIGHT_FIELD_NUMBER = 3;
  private int height_;
  /**
   * <pre>
   *    MerkleTree mt = 4;
   * </pre>
   *
   * <code>int32 height = 3;</code>
   */
  public int getHeight() {
    return height_;
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (prev_ != null) {
      output.writeMessage(1, getPrev());
    }
    if (creatorID_ != 0) {
      output.writeInt32(2, creatorID_);
    }
    if (height_ != 0) {
      output.writeInt32(3, height_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (prev_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getPrev());
    }
    if (creatorID_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(2, creatorID_);
    }
    if (height_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(3, height_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof proto.BlockHeader)) {
      return super.equals(obj);
    }
    proto.BlockHeader other = (proto.BlockHeader) obj;

    boolean result = true;
    result = result && (hasPrev() == other.hasPrev());
    if (hasPrev()) {
      result = result && getPrev()
          .equals(other.getPrev());
    }
    result = result && (getCreatorID()
        == other.getCreatorID());
    result = result && (getHeight()
        == other.getHeight());
    result = result && unknownFields.equals(other.unknownFields);
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasPrev()) {
      hash = (37 * hash) + PREV_FIELD_NUMBER;
      hash = (53 * hash) + getPrev().hashCode();
    }
    hash = (37 * hash) + CREATORID_FIELD_NUMBER;
    hash = (53 * hash) + getCreatorID();
    hash = (37 * hash) + HEIGHT_FIELD_NUMBER;
    hash = (53 * hash) + getHeight();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static proto.BlockHeader parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static proto.BlockHeader parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static proto.BlockHeader parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static proto.BlockHeader parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static proto.BlockHeader parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static proto.BlockHeader parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static proto.BlockHeader parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static proto.BlockHeader parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static proto.BlockHeader parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static proto.BlockHeader parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static proto.BlockHeader parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static proto.BlockHeader parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(proto.BlockHeader prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code proto.BlockHeader}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:proto.BlockHeader)
      proto.BlockHeaderOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return proto.Blockchain.internal_static_proto_BlockHeader_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return proto.Blockchain.internal_static_proto_BlockHeader_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              proto.BlockHeader.class, proto.BlockHeader.Builder.class);
    }

    // Construct using proto.BlockHeader.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      if (prevBuilder_ == null) {
        prev_ = null;
      } else {
        prev_ = null;
        prevBuilder_ = null;
      }
      creatorID_ = 0;

      height_ = 0;

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return proto.Blockchain.internal_static_proto_BlockHeader_descriptor;
    }

    public proto.BlockHeader getDefaultInstanceForType() {
      return proto.BlockHeader.getDefaultInstance();
    }

    public proto.BlockHeader build() {
      proto.BlockHeader result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public proto.BlockHeader buildPartial() {
      proto.BlockHeader result = new proto.BlockHeader(this);
      if (prevBuilder_ == null) {
        result.prev_ = prev_;
      } else {
        result.prev_ = prevBuilder_.build();
      }
      result.creatorID_ = creatorID_;
      result.height_ = height_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof proto.BlockHeader) {
        return mergeFrom((proto.BlockHeader)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(proto.BlockHeader other) {
      if (other == proto.BlockHeader.getDefaultInstance()) return this;
      if (other.hasPrev()) {
        mergePrev(other.getPrev());
      }
      if (other.getCreatorID() != 0) {
        setCreatorID(other.getCreatorID());
      }
      if (other.getHeight() != 0) {
        setHeight(other.getHeight());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      proto.BlockHeader parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (proto.BlockHeader) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private proto.Crypto.Digest prev_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        proto.Crypto.Digest, proto.Crypto.Digest.Builder, proto.Crypto.DigestOrBuilder> prevBuilder_;
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public boolean hasPrev() {
      return prevBuilder_ != null || prev_ != null;
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public proto.Crypto.Digest getPrev() {
      if (prevBuilder_ == null) {
        return prev_ == null ? proto.Crypto.Digest.getDefaultInstance() : prev_;
      } else {
        return prevBuilder_.getMessage();
      }
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public Builder setPrev(proto.Crypto.Digest value) {
      if (prevBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        prev_ = value;
        onChanged();
      } else {
        prevBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public Builder setPrev(
        proto.Crypto.Digest.Builder builderForValue) {
      if (prevBuilder_ == null) {
        prev_ = builderForValue.build();
        onChanged();
      } else {
        prevBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public Builder mergePrev(proto.Crypto.Digest value) {
      if (prevBuilder_ == null) {
        if (prev_ != null) {
          prev_ =
            proto.Crypto.Digest.newBuilder(prev_).mergeFrom(value).buildPartial();
        } else {
          prev_ = value;
        }
        onChanged();
      } else {
        prevBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public Builder clearPrev() {
      if (prevBuilder_ == null) {
        prev_ = null;
        onChanged();
      } else {
        prev_ = null;
        prevBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public proto.Crypto.Digest.Builder getPrevBuilder() {
      
      onChanged();
      return getPrevFieldBuilder().getBuilder();
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    public proto.Crypto.DigestOrBuilder getPrevOrBuilder() {
      if (prevBuilder_ != null) {
        return prevBuilder_.getMessageOrBuilder();
      } else {
        return prev_ == null ?
            proto.Crypto.Digest.getDefaultInstance() : prev_;
      }
    }
    /**
     * <code>.proto.Digest prev = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        proto.Crypto.Digest, proto.Crypto.Digest.Builder, proto.Crypto.DigestOrBuilder> 
        getPrevFieldBuilder() {
      if (prevBuilder_ == null) {
        prevBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            proto.Crypto.Digest, proto.Crypto.Digest.Builder, proto.Crypto.DigestOrBuilder>(
                getPrev(),
                getParentForChildren(),
                isClean());
        prev_ = null;
      }
      return prevBuilder_;
    }

    private int creatorID_ ;
    /**
     * <code>int32 creatorID = 2;</code>
     */
    public int getCreatorID() {
      return creatorID_;
    }
    /**
     * <code>int32 creatorID = 2;</code>
     */
    public Builder setCreatorID(int value) {
      
      creatorID_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 creatorID = 2;</code>
     */
    public Builder clearCreatorID() {
      
      creatorID_ = 0;
      onChanged();
      return this;
    }

    private int height_ ;
    /**
     * <pre>
     *    MerkleTree mt = 4;
     * </pre>
     *
     * <code>int32 height = 3;</code>
     */
    public int getHeight() {
      return height_;
    }
    /**
     * <pre>
     *    MerkleTree mt = 4;
     * </pre>
     *
     * <code>int32 height = 3;</code>
     */
    public Builder setHeight(int value) {
      
      height_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     *    MerkleTree mt = 4;
     * </pre>
     *
     * <code>int32 height = 3;</code>
     */
    public Builder clearHeight() {
      
      height_ = 0;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:proto.BlockHeader)
  }

  // @@protoc_insertion_point(class_scope:proto.BlockHeader)
  private static final proto.BlockHeader DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new proto.BlockHeader();
  }

  public static proto.BlockHeader getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BlockHeader>
      PARSER = new com.google.protobuf.AbstractParser<BlockHeader>() {
    public BlockHeader parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new BlockHeader(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<BlockHeader> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<BlockHeader> getParserForType() {
    return PARSER;
  }

  public proto.BlockHeader getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
