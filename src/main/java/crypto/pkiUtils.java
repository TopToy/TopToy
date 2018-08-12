package crypto;

import config.Config;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.bouncycastle.asn1.x9.X962NamedCurves;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
//import java.security.spec.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class pkiUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(pkiUtils.class);
//    private static PublicKey pubKey;
    private static PrivateKey privKey;
    private static HashMap<Integer, PublicKey> clusterPubKeys = new HashMap<>();

    static {
        KeyFactory kf = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
//            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
//            keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
//            java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
//            PrivateKey privateKey = keyPair.getPrivate();
//            System.out.println(privateKey.getFormat());
//            PublicKey publicKey = keyPair.getPublic();
//            System.out.println(publicKey.getFormat());
            KeyFactory ecKeyFac = KeyFactory.getInstance("ECDSA", "BC");
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().
                    decode(Config.getPrivateKey().replaceAll("\\s+","")));
            privKey = ecKeyFac.generatePrivate(pkcs8EncodedKeySpec);
            HashMap<Integer, String> keys = Config.getClusterPubKeys();
            for (int i : keys.keySet()) {
                X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(keys.get(i).replaceAll("\\s+","")));
              clusterPubKeys.put(i, ecKeyFac.generatePublic(spec));
            }
        } catch (Exception e) {
            logger.fatal("Unable to generate rsa keys", e);
        }
    }
    // TODO: We should extract it out of this class
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
        Signature privateSignature = null;
        try {
            privateSignature = Signature.getInstance("SHA256withECDSA");
            privateSignature.initSign(privKey);
            privateSignature.update(plainText.getBytes(UTF_8));
            byte[] signature = privateSignature.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            logger.fatal("Unable to generate rsa keys", e);
        }
        return null;
    }

   static public boolean verify(int id, String plainText, String signature)  {
        Signature publicSignature = null;
        try {
            publicSignature = Signature.getInstance("SHA256withECDSA");
            publicSignature.initVerify(clusterPubKeys.get(id));
            publicSignature.update(plainText.getBytes(UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signature);

            return publicSignature.verify(signatureBytes);
        } catch (Exception e) {
            logger.fatal("Unable to generate rsa keys", e);
        }
        return false;
    }

}
