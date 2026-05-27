/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.commons.codec.DecoderException
 *  org.apache.commons.codec.binary.Hex
 */
package com.vinplay.dichvuthe.encode;

import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class AES {
    public static String encrypt(String plainText, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String sKey = VinPlayUtils.getMD5Hash((String)key);
        byte[] keyBytes = sKey.getBytes();
        SecretKeySpec sKeySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(1, sKeySpec);
        byte[] encryptBytes = cipher.doFinal(plainText.getBytes());
        return Hex.encodeHexString((byte[])encryptBytes);
    }

    public static String decrypt(String cipherText, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, DecoderException {
        String sKey = VinPlayUtils.getMD5Hash((String)key);
        byte[] keyBytes = sKey.getBytes();
        SecretKeySpec sKeySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(2, sKeySpec);
        byte[] decryptBytes = cipher.doFinal(Hex.decodeHex((char[])cipherText.toCharArray()));
        return new String(decryptBytes);
    }
}

