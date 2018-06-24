/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.tom.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import bftsmart.reconfiguration.ViewController;

public class TOMUtil {

    //private static final int BENCHMARK_PERIOD = 10000;

    //some message types
    public static final int RR_REQUEST = 0;
    public static final int RR_REPLY = 1;
    public static final int RR_DELIVERED = 2;
    public static final int STOP = 3;
    public static final int STOPDATA = 4;
    public static final int SYNC = 5;
    public static final int SM_REQUEST = 6;
    public static final int SM_REPLY = 7;
    public static final int SM_ASK_INITIAL = 11;
    public static final int SM_REPLY_INITIAL = 12;

    public static final int TRIGGER_LC_LOCALLY = 8;
    public static final int TRIGGER_SM_LOCALLY = 9;
    
    private static int signatureSize = -1;
    
    public static int getSignatureSize(ViewController controller) {
        if (signatureSize > 0) {
            return signatureSize;
        }

        byte[] signature = signMessage(controller.getStaticConf().getRSAPrivateKey(),
                "a".getBytes());

        if (signature != null) {
            signatureSize = signature.length;
        }

        return signatureSize;
    }
    
    //******* EDUARDO BEGIN **************//
    public static byte[] getBytes(Object o) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream obOut = null;
        try {
            obOut = new ObjectOutputStream(bOut);
            obOut.writeObject(o);
            obOut.flush();
            bOut.flush();
            obOut.close();
            bOut.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return bOut.toByteArray();
    }

    public static Object getObject(byte[] b) {
        if (b == null)
            return null;

        ByteArrayInputStream bInp = new ByteArrayInputStream(b);
        try {
            ObjectInputStream obInp = new ObjectInputStream(bInp);
            Object ret = obInp.readObject();
            obInp.close();
            bInp.close();
            return ret;
        } catch (Exception ex) {
            return null;
        }
    }
    //******* EDUARDO END **************//

    /**
     * Sign a message.
     *
     * @param key the private key to be used to generate the signature
     * @param message the message to be signed
     * @return the signature
     */
    public static byte[] signMessage(PrivateKey key, byte[] message) {

        byte[] result = null;
        try {

            Signature signatureEngine = Signature.getInstance("SHA1withRSA");

            signatureEngine.initSign(key);

            signatureEngine.update(message);

            result = signatureEngine.sign();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Verify the signature of a message.
     *
     * @param key the public key to be used to verify the signature
     * @param message the signed message
     * @param signature the signature to be verified
     * @return true if the signature is valid, false otherwise
     */
    public static boolean verifySignature(PublicKey key, byte[] message, byte[] signature) {

        boolean result = false;

        try {
            Signature signatureEngine = Signature.getInstance("SHA1withRSA");

            signatureEngine.initVerify(key);

            result = verifySignature(signatureEngine, message, signature);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Verify the signature of a message.
     *
     * @param initializedSignatureEngine a signature engine already initialized
     *        for verification
     * @param message the signed message
     * @param signature the signature to be verified
     * @return true if the signature is valid, false otherwise
     */
    public static boolean verifySignature(Signature initializedSignatureEngine, byte[] message, byte[] signature) throws SignatureException {

        initializedSignatureEngine.update(message);
        return initializedSignatureEngine.verify(signature);
    }

    public static String byteArrayToString(byte[] b) {
        String s = "";
        for (int i = 0; i < b.length; i++) {
            s = s + b[i];
        }

        return s;
    }

    public static boolean equalsHash(byte[] h1, byte[] h2) {
        return Arrays.equals(h2, h2);
    }

    public static final byte[] computeHash(byte[] data) {
        
        byte[] result = null;
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            result = md.digest(data);
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } // TODO: shouldn't it be SHA?
                
        return result;
    }
    
}
