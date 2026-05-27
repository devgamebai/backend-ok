/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Base64
 *  org.bouncycastle.jce.provider.BouncyCastleProvider
 */
package com.vinplay.dichvuthe.encode;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class RSA {
    public static String sign(String data, String key) throws Exception {
        byte[] inputData = data.getBytes();
        PrivateKey privateKey = RSA.convertString2PrivateKey(key);
        byte[] signByte = RSA.signData(inputData, privateKey);
        String sign = Base64.encodeBase64String((byte[])signByte);
        return sign;
    }

    public static boolean verify(String data, String sign, String key) throws Exception {
        byte[] inputData = data.getBytes();
        byte[] inputSign = Base64.decodeBase64((String)sign);
        PublicKey publicKey = RSA.convertString2PublicKey(key);
        boolean status = RSA.verifySig(inputData, publicKey, inputSign);
        return status;
    }

    public static String enCrypt(String plain_text, String key) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] inputData = plain_text.getBytes();
        PrivateKey privateKey = RSA.convertString2PrivateKey(key);
        Security.addProvider((Provider)new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance("RSA/ECB/NOPADDING", "BC");
        cipher.init(1, privateKey);
        byte[] cipherData = cipher.doFinal(inputData);
        return Base64.encodeBase64String((byte[])cipherData);
    }

    public static String deCrypt(String cipher_text, String key) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PublicKey publicKey = RSA.convertString2PublicKey(key);
        Security.addProvider((Provider)new BouncyCastleProvider());
        Cipher cipher = Cipher.getInstance("RSA/ECB/NOPADDING", "BC");
        cipher.init(2, publicKey);
        byte[] cipherData = cipher.doFinal(Base64.decodeBase64((String)cipher_text));
        return new String(cipherData);
    }

    private static byte[] signData(byte[] data, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(key);
        signer.update(data);
        return signer.sign();
    }

    private static boolean verifySig(byte[] data, PublicKey key, byte[] sign) throws Exception {
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initVerify(key);
        signer.update(data);
        return signer.verify(sign);
    }

    private static PrivateKey convertString2PrivateKey(String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] bprivateKey = Base64.decodeBase64((String)privateKey);
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bprivateKey));
        return key;
    }

    private static PublicKey convertString2PublicKey(String publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] bpublicKey = Base64.decodeBase64((String)publicKey);
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bpublicKey));
        return key;
    }
}

