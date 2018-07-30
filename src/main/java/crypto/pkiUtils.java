package crypto;

import config.Config;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.*;
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
            kf = KeyFactory.getInstance("RSA");
            byte[] key = addpkcs8data(Base64.getDecoder().decode(Config.getPrivateKey().replaceAll("\\s+","")));
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(key);

            privKey = kf.generatePrivate(keySpecPKCS8);
//            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(Config.getPublicKey().replaceAll("\\s+","")));
//            pubKey = kf.generatePublic(keySpecX509);
            HashMap<Integer, String> keys = Config.getClusterPubKeys();
            for (int i : keys.keySet()) {
                X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(keys.get(i).replaceAll("\\s+","")));
                clusterPubKeys.put(i,  kf.generatePublic(spec));
            }
        } catch (Exception e) {
            logger.fatal("Unable to generate rsa keys", e);
        }
    }

    static byte[] addpkcs8data(byte[] key) throws IOException {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(0));
        ASN1EncodableVector v2 = new ASN1EncodableVector();
        v2.add(new ASN1ObjectIdentifier(PKCSObjectIdentifiers.rsaEncryption.getId()));
        v2.add(DERNull.INSTANCE);
        v.add(new DERSequence(v2));
        v.add(new DEROctetString(key));
        ASN1Sequence seq = new DERSequence(v);
        return seq.getEncoded("DER");
    }
    static public String sign(String plainText) {
        Signature privateSignature = null;
        try {
            privateSignature = Signature.getInstance("SHA256withRSA");
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
            publicSignature = Signature.getInstance("SHA256withRSA");
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
