package crypto;

import org.apache.commons.lang.ArrayUtils;
import proto.Types;

import java.util.Arrays;

public class blockDigSig {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockDigSig.class);

    static public boolean verifyHeader(int id, Types.BlockHeader header) {
        return pkiUtils.verify(id,
                new String(header.getM().toByteArray()) +
                        String.valueOf(header.getHeight()) +
                        new String(header.getTransactionHash().toByteArray()), header.getProof());
    }

    static public boolean verfiyBlockWRTheader(Types.Block b, Types.BlockHeader h) {
        byte[] tHash = new byte[0];
        for (Types.Transaction t : b.getDataList()) {
            tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
        }
        return Arrays.equals(tHash, h.getTransactionHash().toByteArray());
    }

    static public boolean verifyBlock(Types.Block b) {
        return verfiyBlockWRTheader(b, b.getHeader())
                && verifyHeader(b.getHeader().getM().getSender(), b.getHeader());
    }

    static public String sign(Types.BlockHeader header) {
        return pkiUtils.sign(new String(header.getM().toByteArray()) +
                String.valueOf(header.getHeight()) +
                new String(header.getTransactionHash().toByteArray()));
    }
}
