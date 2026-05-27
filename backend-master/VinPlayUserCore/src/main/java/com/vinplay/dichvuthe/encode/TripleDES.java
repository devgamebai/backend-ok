/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.encode;

import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class TripleDES {
    public static String Encrypt(String data, String key) throws Exception {
        byte[] dataBytes = data.getBytes("UTF-8");
        byte[] keyBytes = key.getBytes("UTF-8");
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(keyBytes, 0, key.length());
        byte[] keyBuyDes = m.digest();
        SecretKeySpec keyspec = new SecretKeySpec(new BigInteger(1, keyBuyDes).toString(16).substring(0, 24).getBytes(), "DESede");
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(1, keyspec);
        byte[] encrypted = cipher.doFinal(dataBytes);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String Decrypt(String data, String key) throws Exception {
        byte[] keyBytes = key.getBytes("UTF-8");
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(keyBytes, 0, key.length());
        byte[] keyBuyDes = m.digest();
        SecretKeySpec keyspec = new SecretKeySpec(new BigInteger(1, keyBuyDes).toString(16).substring(0, 24).getBytes(), "DESede");
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(2, keyspec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));
        return new String(decrypted);
    }
}

