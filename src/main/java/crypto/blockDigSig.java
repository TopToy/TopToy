package crypto;


import com.google.protobuf.AbstractMessageLite;
import org.apache.commons.lang.ArrayUtils;
import proto.Types;
import proto.Types.Block;

public class blockDigSig implements digitalSignature {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockDigSig.class);
    @Override
    public String signMessage(Object toSign) {
        Block.Builder b =(Block.Builder) toSign;
        return pkiUtils.sign(String.valueOf(b.getHeader().getCidSeries()) +
                String.valueOf(b.getHeader().getCid()) +
                new String(b.getHeader().getTransactionHash().toByteArray()));
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        Block b = (Block) toVer;
        return  pkiUtils.verify(id,
                String.valueOf(b.getHeader().getCidSeries())
                        + String.valueOf(b.getHeader().getCid())
                        + new String(b.getHeader().getTransactionHash().toByteArray()),
                b.getHeader().getProof());
    }

    public static boolean verify(int id, Block b) {
        return new blockDigSig().verifyMessage(id, b);
    }

    public static String sign(Block.Builder b) {
        return new blockDigSig().signMessage(b);
    }
}
