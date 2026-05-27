/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.bouncycastle.jce.provider.BouncyCastleProvider
 */
package com.vinplay.epay.megacard;

import com.vinplay.epay.megacard.IEncryption;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class TripleDESEncryption
implements IEncryption {
    static final byte[] SALT = new byte[]{9};
    static final int ITERATIONS = 11;

    private static SecretKey getKey(String key) {
        byte[] bKey = TripleDESEncryption.hexToBytes(key);
        try {
            DESedeKeySpec keyspec = new DESedeKeySpec(bKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey lclSK = keyFactory.generateSecret(keyspec);
            return lclSK;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encrypt(String key, String input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        byte[] bytes = null;
        SecretKey sKey = null;
        Security.addProvider((Provider)new BouncyCastleProvider());
        Cipher encipher = Cipher.getInstance("DESede/ECB/PKCS5Padding", "BC");
        bytes = input.getBytes("UTF-8");
        sKey = TripleDESEncryption.getKey(key);
        encipher.init(1, sKey);
        byte[] enc = encipher.doFinal(bytes);
        return TripleDESEncryption.bytesToHex(enc);
    }

    public static String decrypt(String key, String cipher) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        byte[] bytes = null;
        SecretKey sKey = null;
        Security.addProvider((Provider)new BouncyCastleProvider());
        Cipher encipher = Cipher.getInstance("DESede/ECB/PKCS5Padding", "BC");
        bytes = TripleDESEncryption.hexToBytes(cipher);
        sKey = TripleDESEncryption.getKey(key);
        encipher.init(2, sKey);
        byte[] enc = encipher.doFinal(bytes);
        return new String(enc);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) {
                buf.append("0");
            }
            buf.append(hex);
        }
        return buf.toString();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte)Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    public static void main(String[] args) {
    }
}

