// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: blockchain.proto

package proto;

public interface ForkProofOrBuilder extends
    // @@protoc_insertion_point(interface_extends:proto.ForkProof)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.proto.RmfResult curr = 1;</code>
   */
  boolean hasCurr();
  /**
   * <code>.proto.RmfResult curr = 1;</code>
   */
  proto.RmfResult getCurr();
  /**
   * <code>.proto.RmfResult curr = 1;</code>
   */
  proto.RmfResultOrBuilder getCurrOrBuilder();

  /**
   * <code>string currSig = 2;</code>
   */
  java.lang.String getCurrSig();
  /**
   * <code>string currSig = 2;</code>
   */
  com.google.protobuf.ByteString
      getCurrSigBytes();

  /**
   * <code>.proto.RmfResult prev = 3;</code>
   */
  boolean hasPrev();
  /**
   * <code>.proto.RmfResult prev = 3;</code>
   */
  proto.RmfResult getPrev();
  /**
   * <code>.proto.RmfResult prev = 3;</code>
   */
  proto.RmfResultOrBuilder getPrevOrBuilder();

  /**
   * <code>string prevSig = 4;</code>
   */
  java.lang.String getPrevSig();
  /**
   * <code>string prevSig = 4;</code>
   */
  com.google.protobuf.ByteString
      getPrevSigBytes();
}