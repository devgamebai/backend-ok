/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.epay;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; ++i) {
            int halfbyte = data[i] >>> 4 & 15;
            int two_halfs = 0;
            do {
                if (0 <= halfbyte && halfbyte <= 9) {
                    buf.append((char)(48 + halfbyte));
                } else {
                    buf.append((char)(97 + (halfbyte - 10)));
                }
                halfbyte = data[i] & 15;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String hash(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return MD5.convertToHex(md5hash);
    }
}

