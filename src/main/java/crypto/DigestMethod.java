package crypto;

import com.google.protobuf.ByteString;
import proto.BlockHeader;
import proto.Crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DigestMethod {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DigestMethod.class);
    private static MessageDigest digestMethod;

    static {
        try {
            digestMethod = MessageDigest.getInstance("SHA-256"); // TODO: Should be pulled from the configuration file ?
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("", e);
        }
    }

    static public Crypto.Digest hash(BlockHeader header) {
        return Crypto.Digest.
                newBuilder().
                setDigest(ByteString.copyFrom(digestMethod.digest(header.toByteArray()))).
                build();

        // ignore close exception
    }

    static public boolean validate(Crypto.Digest d1, Crypto.Digest d2) {
        boolean res = Arrays.equals(d1.getDigest().toByteArray(), d2.getDigest().toByteArray());
        if (!res) {
            int a = 1;
        }
        return res;
    }
}
