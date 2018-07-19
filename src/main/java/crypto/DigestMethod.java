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
 // TODO: Replace signature module??
public class DigestMethod {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DigestMethod.class);
//    private static MessageDigest digestMethod;

//    static {
//        try {
//            digestMethod = MessageDigest.getInstance("SHA-256"); // TODO: Should be pulled from the configuration file ?
//        } catch (NoSuchAlgorithmException e) {
//            logger.fatal("", e);
//        }
//    }

    static public byte[] hash(BlockHeader header) {
        MessageDigest digestMethod = null;
        try {
            digestMethod = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("", e);
        }
        assert digestMethod != null;
        return digestMethod.digest(header.toByteArray());
    }

    static public boolean validate(byte[] d1, byte[] d2) {
        return Arrays.equals(d1, d2);
    }
}
