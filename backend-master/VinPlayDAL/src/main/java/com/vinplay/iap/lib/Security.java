/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.util.TextUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.iap.lib;

import com.vinplay.iap.lib.Base64;
import com.vinplay.iap.lib.Base64DecoderException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.apache.http.util.TextUtils;
import org.apache.log4j.Logger;

public class Security {
    private static final Logger logger = Logger.getLogger((String)"user_core");
    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    public static boolean verifyPurchase(String base64PublicKey, String signedData, String signature) {
        if (TextUtils.isEmpty((CharSequence)signedData) || TextUtils.isEmpty((CharSequence)base64PublicKey) || TextUtils.isEmpty((CharSequence)signature)) {
            logger.debug("Purchase verification failed: missing data.");
            return false;
        }
        PublicKey key = Security.generatePublicKey(base64PublicKey);
        return Security.verify(key, signedData, signature);
    }

    public static PublicKey generatePublicKey(String encodedPublicKey) {
        try {
            byte[] decodedKey = Base64.decode(encodedPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        catch (InvalidKeySpecException e2) {
            logger.debug("Invalid key specification.");
            throw new IllegalArgumentException(e2);
        }
        catch (Base64DecoderException e3) {
            logger.debug("Base64 decoding failed.");
            throw new IllegalArgumentException(e3);
        }
    }

    public static boolean verify(PublicKey publicKey, String signedData, String signature) {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes());
            if (!sig.verify(Base64.decode(signature))) {
                logger.debug("Signature verification failed.");
                return false;
            }
            return true;
        }
        catch (NoSuchAlgorithmException e) {
            logger.debug("NoSuchAlgorithmException.");
        }
        catch (InvalidKeyException e2) {
            logger.debug("Invalid key specification.");
        }
        catch (SignatureException e3) {
            logger.debug("Signature exception.");
        }
        catch (Base64DecoderException e4) {
            logger.debug("Base64 decoding failed.");
        }
        return false;
    }
}

