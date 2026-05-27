/*
 * Decompiled with CFR 0.152.
 */
package com.payment.core.common;

import java.io.IOException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class DESEncrypt {
    private String key;

    public DESEncrypt() {
    }

    public DESEncrypt(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] desEncrypt(byte[] plainText) throws Exception {
        SecureRandom sr = new SecureRandom();
        DESKeySpec dks = new DESKeySpec(this.key.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(dks);
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(1, (Key)key, sr);
        byte[] data = plainText;
        byte[] encryptedData = cipher.doFinal(data);
        return encryptedData;
    }

    public byte[] desDecrypt(byte[] encryptText) throws Exception {
        SecureRandom sr = new SecureRandom();
        DESKeySpec dks = new DESKeySpec(this.key.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(dks);
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(2, (Key)key, sr);
        byte[] encryptedData = encryptText;
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return decryptedData;
    }

    public static String base64Encode(byte[] s) {
        if (s == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(s);
    }

    public static byte[] base64Decode(String s) throws IOException {
        if (s == null) {
            return null;
        }
        byte[] b = Base64.getDecoder().decode(s);
        return b;
    }

    public String encrypt(String input) {
        try {
            return DESEncrypt.base64Encode(this.desEncrypt(input.getBytes())).replaceAll("\\s*", "");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String input) throws Exception {
        byte[] result = DESEncrypt.base64Decode(input);
        return new String(this.desDecrypt(result));
    }
}

