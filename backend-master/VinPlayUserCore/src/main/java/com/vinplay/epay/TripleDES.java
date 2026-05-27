/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.epay;

import com.vinplay.epay.MD5;
import java.io.PrintStream;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class TripleDES {
    public static String Encrypt(String plainText, String key) {
        try {
            byte[] arrayBytes = TripleDES.getValidKey(key);
            DESedeKeySpec ks = new DESedeKeySpec(arrayBytes);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
            Cipher cipher = Cipher.getInstance("DESede");
            SecretKey seckey = skf.generateSecret(ks);
            cipher.init(1, seckey);
            byte[] plainByte = plainText.getBytes("UTF8");
            byte[] encryptedByte = cipher.doFinal(plainByte);
            return Base64.getEncoder().encodeToString(encryptedByte);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String Decrypt(String encryptData, String key) {
        try {
            byte[] arrayBytes = TripleDES.getValidKey(key);
            DESedeKeySpec ks = new DESedeKeySpec(arrayBytes);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DESede");
            Cipher cipher = Cipher.getInstance("DESede");
            SecretKey seckey = skf.generateSecret(ks);
            cipher.init(2, seckey);
            byte[] encryptByte = Base64.getDecoder().decode(encryptData);
            byte[] plainByte = cipher.doFinal(encryptByte);
            return new String(plainByte);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] getValidKey(String key) {
        try {
            String sTemp = MD5.hash(key).substring(0, 24);
            return sTemp.getBytes();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(TripleDES.Encrypt("hai", "123456"));
    }
}

