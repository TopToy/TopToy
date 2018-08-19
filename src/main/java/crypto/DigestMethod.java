package crypto;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class DigestMethod {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DigestMethod.class);

    static public byte[] hash(byte[] toHash) {
        MessageDigest digestMethod = null;
        try {
            digestMethod = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("", e);
        }
        assert digestMethod != null;
        return digestMethod.digest(toHash);
    }

    static public boolean validate(byte[] d1, byte[] d2) {
        return Arrays.equals(d1, d2);
    }
}
