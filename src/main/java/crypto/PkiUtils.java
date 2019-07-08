package crypto;

import utils.config.Config;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PkiUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PkiUtils.class);
    private static PrivateKey privKey;
    private static HashMap<Integer, Signature> clusterPubKeys = new HashMap<>();
    private static final Object globalLock = new Object();
    private static Signature privateSignature;

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA", "BC");
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().
                    decode(Config.getPrivateKey().replaceAll("\\s+","")));
            privKey = ecKeyFac.generatePrivate(pkcs8EncodedKeySpec);
            HashMap<Integer, String> keys = Config.getClusterPubKeys();
            for (int i : keys.keySet()) {
                X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(keys.get(i).replaceAll("\\s+","")));
              clusterPubKeys.put(i, Signature.getInstance("SHA256withECDSA"));
              clusterPubKeys.get(i).initVerify(ecKeyFac.generatePublic(spec));
            }
            privateSignature = Signature.getInstance("SHA256withECDSA");
            privateSignature.initSign(privKey);
        } catch (Exception e) {
            logger.fatal("Unable to generate rsa keys", e);
        }
    }

    static void generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
        java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println(new String(Base64.getEncoder().encode(privateKey.getEncoded())));
        PublicKey publicKey = keyPair.getPublic();
        System.out.println(new String(Base64.getEncoder().encode(publicKey.getEncoded())));
    }
    static public String sign(String plainText) {
        synchronized (globalLock) {
            try {
                privateSignature.update(plainText.getBytes(UTF_8));
                byte[] signature = privateSignature.sign();
                return Base64.getEncoder().encodeToString(signature);
            } catch (Exception e) {
                logger.fatal("Unable to generate rsa keys", e);
            }
        }
        return null;
    }

   static public boolean verify(int id, String plainText, String signature)  {
        synchronized (globalLock) {
            Signature publicSignature = clusterPubKeys.get(id);
            try {
                publicSignature.update(plainText.getBytes(UTF_8));
                byte[] signatureBytes = Base64.getDecoder().decode(signature);
                return publicSignature.verify(signatureBytes);
            } catch (Exception e) {
                logger.fatal("Unable to validate message", e);
            }
        }
        return false;
    }

}
