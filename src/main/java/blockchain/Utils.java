package blockchain;

import blockchain.genesis.SGC;
import blockchain.validation.Validator;
import com.google.protobuf.ByteString;
import crypto.DigestMethod;
import crypto.blockDigSig;
import org.apache.commons.lang.ArrayUtils;
import proto.Types;

import java.nio.file.Path;
import java.util.Objects;

public class Utils {
    public enum BCT {
        SGC,
    }

    static public boolean validateBlockHash(Types.Block prev, Types.Block b) {
        byte[] d = DigestMethod.hash(prev.getHeader().toByteArray());
        return DigestMethod.validate(b.getHeader().getPrev().toByteArray(),
                Objects.requireNonNull(d));
    }

    static public Types.BlockHeader createBlockHeader(Types.Block b, Types.BlockHeader header,
                                                         int creatorID, int height, int cidSeries, int cid,
                                                         int channel, int bid) {
        byte[] tHash = new byte[0];
        for (Types.Transaction t : b.getDataList()) {
            tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
        }
        byte[] headerArray = new byte[0];
        if (header != null) {
            headerArray = header.toByteArray();
        }
        Types.BlockHeader h = Types.BlockHeader.newBuilder()
                .setM(Types.Meta.newBuilder()
                        .setSender(creatorID)
                        .setCid(cid)
                        .setCidSeries(cidSeries)
                        .setChannel(channel))
                .setHeight(height)
                .setBid(bid)
                .setTransactionHash(ByteString.copyFrom(tHash))
                .setPrev(ByteString.copyFrom(DigestMethod.hash(headerArray)))
                .build();

        if (creatorID == -1) {
            return h;
        }

        String signature = blockDigSig.sign(h);
        h.toBuilder()
                .setProof(signature)
        .build();
        return h;
    }

    static public boolean validateTransactionWRTBlock(Types.Block.Builder b, Types.Transaction tx, Validator v) {
        return v.validateTX(b, tx);
    }

    static public Blockchain createBlockchain(BCT type, int creatorID, int maxCacheSize, Path swapPath) {
        switch (type) {

            case SGC: return new Blockchain(creatorID, maxCacheSize, new SGC(), swapPath);
        }
        return null;
    }
}
