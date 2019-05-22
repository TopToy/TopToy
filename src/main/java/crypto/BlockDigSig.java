package crypto;

import org.apache.commons.lang.ArrayUtils;
import proto.Types;

import java.math.BigInteger;
import java.util.Arrays;

import static java.lang.String.format;

public class BlockDigSig {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BlockDigSig.class);

    static public boolean verifyHeader(int id, Types.BlockHeader header) {
        return PkiUtils.verify(id,
                new String(header.getM().toByteArray())
                        + String.valueOf(header.getHeight())
                        + new String(header.getTransactionHash().toByteArray())
                        + new String(header.getBid().toByteArray())
                        + new String(header.getPrev().toByteArray()),
                header.getProof());
    }

    static public boolean verfiyBlockWRTheader(Types.Block b, Types.BlockHeader h) {
                return Arrays.equals(hashBlockData(b), h.getTransactionHash().toByteArray());
    }

    static public boolean verifyBlock(Types.Block b) {
        boolean res1 = verfiyBlockWRTheader(b, b.getHeader());
        boolean res2 = verifyHeader(b.getHeader().getBid().getPid(), b.getHeader());
        logger.debug(format("C[%d] valid results are [%b : %b] for [height=%d ; cidSeries=%d ; cid=%d]",
                b.getHeader().getM().getChannel(), res1, res2, b.getHeader().getHeight(), b.getHeader().getM().getCidSeries(),
                b.getHeader().getM().getCid()));
        return res1 && res2;
    }

    static public String sign(Types.BlockHeader header) {
        return PkiUtils.sign(
                new String(header.getM().toByteArray())
                + String.valueOf(header.getHeight())
                + new String(header.getTransactionHash().toByteArray())
                + new String(header.getBid().toByteArray())
                + new String(header.getPrev().toByteArray()));
    }

    static public byte[] hashBlockData(Types.Block b) {
        byte[] tHash = new byte[0];
        for (Types.Transaction t : b.getDataList()) {
            tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
        }
        return tHash;
    }

    static public byte[] hashHeader(Types.BlockHeader h) {
        byte[] hash = DigestMethod.hash(ArrayUtils.addAll(h.getM().toByteArray(),
                BigInteger.valueOf(h.getHeight()).toByteArray()));

        hash = DigestMethod.hash(ArrayUtils.addAll(hash, h.getTransactionHash().toByteArray()));

        hash = DigestMethod.hash(ArrayUtils.addAll(hash, h.getPrev().toByteArray()));

        return hash;
    }
}
