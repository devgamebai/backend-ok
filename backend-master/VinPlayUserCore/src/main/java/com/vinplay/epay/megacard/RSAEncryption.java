/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.bouncycastle.jce.provider.BouncyCastleProvider
 */
package com.vinplay.epay.megacard;

import com.vinplay.epay.megacard.Base64Utils;
import com.vinplay.epay.megacard.IEncryption;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class RSAEncryption
implements IEncryption {
    String publicKeyStr;
    String privateKeyStr;

    public String getPublicKeyStr() {
        return this.publicKeyStr;
    }

    public void setPublicKeyStr(String publicKeyStr) {
        this.publicKeyStr = publicKeyStr;
    }

    public String getPrivateKeyStr() {
        return this.privateKeyStr;
    }

    public void setPrivateKeyStr(String privateKeyStr) throws InvalidKeySpecException, NoSuchAlgorithmException {
        this.privateKeyStr = privateKeyStr;
    }

    public String encrypt(String clear_text) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        byte[] bpublicKey = Base64Utils.base64Decode(this.publicKeyStr);
        Security.addProvider((Provider)new BouncyCastleProvider());
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bpublicKey));
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "BC");
        cipher.init(1, key);
        byte[] cipherData = cipher.doFinal(clear_text.getBytes());
        return Base64Utils.base64Encode(cipherData);
    }

    public String decrypt(String cipher_text) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        byte[] bprivateKey = Base64Utils.base64Decode(this.privateKeyStr);
        Security.addProvider((Provider)new BouncyCastleProvider());
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bprivateKey));
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "BC");
        cipher.init(2, key);
        byte[] cipherData = cipher.doFinal(Base64Utils.base64Decode(cipher_text));
        return new String(cipherData);
    }
}

