package crypto;

import org.apache.commons.lang.ArrayUtils;
import proto.Types;
import proto.Types.Block;

import java.util.Arrays;

public class blockDigSig implements digitalSignature {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockDigSig.class);
    @Override
    public String signMessage(Object toSign) {
        Types.BlockHeader header = (Types.BlockHeader) toSign;
//        Block.Builder b =(Block.Builder) toSign;
        return pkiUtils.sign(new String(header.getM().toByteArray()) +
                String.valueOf(header.getHeight()) +
                new String(header.getTransactionHash().toByteArray()));
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        Block b = (Block) toVer;
        byte[] tHash = new byte[0];
        for (Types.Transaction t : b.getDataList()) {
            tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
        }
        if (!Arrays.equals(tHash, b.getHeader().getTransactionHash().toByteArray())) return false;
        Types.BlockHeader header = b.getHeader();
        return pkiUtils.verify(id,
                new String(header.getM().toByteArray()) +
                        String.valueOf(header.getHeight()) +
                        new String(header.getTransactionHash().toByteArray()), header.getProof());
    }

    public static boolean verify(int id, Block b) {
        return new blockDigSig().verifyMessage(id, b);
    }

    public static String sign(Types.BlockHeader b) {
        return new blockDigSig().signMessage(b);
    }
}
