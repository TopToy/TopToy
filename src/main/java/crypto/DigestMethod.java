package crypto;

import com.google.protobuf.ByteString;
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

    static public Crypto.Digest hash(Object obj) {
        ObjectOutput out = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.flush();
            byte[] bytesArray = bos.toByteArray();
            return Crypto.Digest.newBuilder().setDigest(ByteString.copyFrom(digestMethod.digest(bytesArray))).build();
        } catch (IOException e) {
            logger.error("", e);
        }
        return null;
        // ignore close exception
    }

    static public boolean validate(Crypto.Digest d1, Crypto.Digest d2)   {
        return Arrays.equals(d1.getDigest().toByteArray(), d2.getDigest().toByteArray());
    }
}
