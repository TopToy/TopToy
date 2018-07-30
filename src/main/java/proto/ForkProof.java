// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: blockchain.proto

package proto;

/**
 * Protobuf type {@code proto.ForkProof}
 */
public  final class ForkProof extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:proto.ForkProof)
    ForkProofOrBuilder {
private static final long serialVersionUID = 0L;
  // Use ForkProof.newBuilder() to construct.
  private ForkProof(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ForkProof() {
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private ForkProof(
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
            proto.RmfResult.Builder subBuilder = null;
            if (curr_ != null) {
              subBuilder = curr_.toBuilder();
            }
            curr_ = input.readMessage(proto.RmfResult.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(curr_);
              curr_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            proto.RmfResult.Builder subBuilder = null;
            if (prev_ != null) {
              subBuilder = prev_.toBuilder();
            }
            prev_ = input.readMessage(proto.RmfResult.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(prev_);
              prev_ = subBuilder.buildPartial();
            }

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
    return proto.Blockchain.internal_static_proto_ForkProof_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return proto.Blockchain.internal_static_proto_ForkProof_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            proto.ForkProof.class, proto.ForkProof.Builder.class);
  }

  public static final int CURR_FIELD_NUMBER = 1;
  private proto.RmfResult curr_;
  /**
   * <code>.proto.RmfResult curr = 1;</code>
   */
  public boolean hasCurr() {
    return curr_ != null;
  }
  /**
   * <code>.proto.RmfResult curr = 1;</code>
   */
  public proto.RmfResult getCurr() {
    return curr_ == null ? proto.RmfResult.getDefaultInstance() : curr_;
  }
  /**
   * <code>.proto.RmfResult curr = 1;</code>
   */
  public proto.RmfResultOrBuilder getCurrOrBuilder() {
    return getCurr();
  }

  public static final int PREV_FIELD_NUMBER = 2;
  private proto.RmfResult prev_;
  /**
   * <code>.proto.RmfResult prev = 2;</code>
   */
  public boolean hasPrev() {
    return prev_ != null;
  }
  /**
   * <code>.proto.RmfResult prev = 2;</code>
   */
  public proto.RmfResult getPrev() {
    return prev_ == null ? proto.RmfResult.getDefaultInstance() : prev_;
  }
  /**
   * <code>.proto.RmfResult prev = 2;</code>
   */
  public proto.RmfResultOrBuilder getPrevOrBuilder() {
    return getPrev();
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
    if (curr_ != null) {
      output.writeMessage(1, getCurr());
    }
    if (prev_ != null) {
      output.writeMessage(2, getPrev());
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (curr_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getCurr());
    }
    if (prev_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, getPrev());
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
    if (!(obj instanceof proto.ForkProof)) {
      return super.equals(obj);
    }
    proto.ForkProof other = (proto.ForkProof) obj;

    boolean result = true;
    result = result && (hasCurr() == other.hasCurr());
    if (hasCurr()) {
      result = result && getCurr()
          .equals(other.getCurr());
    }
    result = result && (hasPrev() == other.hasPrev());
    if (hasPrev()) {
      result = result && getPrev()
          .equals(other.getPrev());
    }
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
    if (hasCurr()) {
      hash = (37 * hash) + CURR_FIELD_NUMBER;
      hash = (53 * hash) + getCurr().hashCode();
    }
    if (hasPrev()) {
      hash = (37 * hash) + PREV_FIELD_NUMBER;
      hash = (53 * hash) + getPrev().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static proto.ForkProof parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static proto.ForkProof parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static proto.ForkProof parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static proto.ForkProof parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static proto.ForkProof parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static proto.ForkProof parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static proto.ForkProof parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static proto.ForkProof parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static proto.ForkProof parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static proto.ForkProof parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static proto.ForkProof parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static proto.ForkProof parseFrom(
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
  public static Builder newBuilder(proto.ForkProof prototype) {
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
   * Protobuf type {@code proto.ForkProof}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:proto.ForkProof)
      proto.ForkProofOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return proto.Blockchain.internal_static_proto_ForkProof_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return proto.Blockchain.internal_static_proto_ForkProof_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              proto.ForkProof.class, proto.ForkProof.Builder.class);
    }

    // Construct using proto.ForkProof.newBuilder()
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
      if (currBuilder_ == null) {
        curr_ = null;
      } else {
        curr_ = null;
        currBuilder_ = null;
      }
      if (prevBuilder_ == null) {
        prev_ = null;
      } else {
        prev_ = null;
        prevBuilder_ = null;
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return proto.Blockchain.internal_static_proto_ForkProof_descriptor;
    }

    public proto.ForkProof getDefaultInstanceForType() {
      return proto.ForkProof.getDefaultInstance();
    }

    public proto.ForkProof build() {
      proto.ForkProof result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public proto.ForkProof buildPartial() {
      proto.ForkProof result = new proto.ForkProof(this);
      if (currBuilder_ == null) {
        result.curr_ = curr_;
      } else {
        result.curr_ = currBuilder_.build();
      }
      if (prevBuilder_ == null) {
        result.prev_ = prev_;
      } else {
        result.prev_ = prevBuilder_.build();
      }
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
      if (other instanceof proto.ForkProof) {
        return mergeFrom((proto.ForkProof)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(proto.ForkProof other) {
      if (other == proto.ForkProof.getDefaultInstance()) return this;
      if (other.hasCurr()) {
        mergeCurr(other.getCurr());
      }
      if (other.hasPrev()) {
        mergePrev(other.getPrev());
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
      proto.ForkProof parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (proto.ForkProof) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private proto.RmfResult curr_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        proto.RmfResult, proto.RmfResult.Builder, proto.RmfResultOrBuilder> currBuilder_;
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public boolean hasCurr() {
      return currBuilder_ != null || curr_ != null;
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public proto.RmfResult getCurr() {
      if (currBuilder_ == null) {
        return curr_ == null ? proto.RmfResult.getDefaultInstance() : curr_;
      } else {
        return currBuilder_.getMessage();
      }
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public Builder setCurr(proto.RmfResult value) {
      if (currBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        curr_ = value;
        onChanged();
      } else {
        currBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public Builder setCurr(
        proto.RmfResult.Builder builderForValue) {
      if (currBuilder_ == null) {
        curr_ = builderForValue.build();
        onChanged();
      } else {
        currBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public Builder mergeCurr(proto.RmfResult value) {
      if (currBuilder_ == null) {
        if (curr_ != null) {
          curr_ =
            proto.RmfResult.newBuilder(curr_).mergeFrom(value).buildPartial();
        } else {
          curr_ = value;
        }
        onChanged();
      } else {
        currBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public Builder clearCurr() {
      if (currBuilder_ == null) {
        curr_ = null;
        onChanged();
      } else {
        curr_ = null;
        currBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public proto.RmfResult.Builder getCurrBuilder() {
      
      onChanged();
      return getCurrFieldBuilder().getBuilder();
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    public proto.RmfResultOrBuilder getCurrOrBuilder() {
      if (currBuilder_ != null) {
        return currBuilder_.getMessageOrBuilder();
      } else {
        return curr_ == null ?
            proto.RmfResult.getDefaultInstance() : curr_;
      }
    }
    /**
     * <code>.proto.RmfResult curr = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        proto.RmfResult, proto.RmfResult.Builder, proto.RmfResultOrBuilder> 
        getCurrFieldBuilder() {
      if (currBuilder_ == null) {
        currBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            proto.RmfResult, proto.RmfResult.Builder, proto.RmfResultOrBuilder>(
                getCurr(),
                getParentForChildren(),
                isClean());
        curr_ = null;
      }
      return currBuilder_;
    }

    private proto.RmfResult prev_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        proto.RmfResult, proto.RmfResult.Builder, proto.RmfResultOrBuilder> prevBuilder_;
    /**
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public boolean hasPrev() {
      return prevBuilder_ != null || prev_ != null;
    }
    /**
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public proto.RmfResult getPrev() {
      if (prevBuilder_ == null) {
        return prev_ == null ? proto.RmfResult.getDefaultInstance() : prev_;
      } else {
        return prevBuilder_.getMessage();
      }
    }
    /**
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public Builder setPrev(proto.RmfResult value) {
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
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public Builder setPrev(
        proto.RmfResult.Builder builderForValue) {
      if (prevBuilder_ == null) {
        prev_ = builderForValue.build();
        onChanged();
      } else {
        prevBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public Builder mergePrev(proto.RmfResult value) {
      if (prevBuilder_ == null) {
        if (prev_ != null) {
          prev_ =
            proto.RmfResult.newBuilder(prev_).mergeFrom(value).buildPartial();
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
     * <code>.proto.RmfResult prev = 2;</code>
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
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public proto.RmfResult.Builder getPrevBuilder() {
      
      onChanged();
      return getPrevFieldBuilder().getBuilder();
    }
    /**
     * <code>.proto.RmfResult prev = 2;</code>
     */
    public proto.RmfResultOrBuilder getPrevOrBuilder() {
      if (prevBuilder_ != null) {
        return prevBuilder_.getMessageOrBuilder();
      } else {
        return prev_ == null ?
            proto.RmfResult.getDefaultInstance() : prev_;
      }
    }
    /**
     * <code>.proto.RmfResult prev = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        proto.RmfResult, proto.RmfResult.Builder, proto.RmfResultOrBuilder> 
        getPrevFieldBuilder() {
      if (prevBuilder_ == null) {
        prevBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            proto.RmfResult, proto.RmfResult.Builder, proto.RmfResultOrBuilder>(
                getPrev(),
                getParentForChildren(),
                isClean());
        prev_ = null;
      }
      return prevBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:proto.ForkProof)
  }

  // @@protoc_insertion_point(class_scope:proto.ForkProof)
  private static final proto.ForkProof DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new proto.ForkProof();
  }

  public static proto.ForkProof getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ForkProof>
      PARSER = new com.google.protobuf.AbstractParser<ForkProof>() {
    public ForkProof parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new ForkProof(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<ForkProof> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ForkProof> getParserForType() {
    return PARSER;
  }

  public proto.ForkProof getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

