/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.statics;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TimeBasedOneTimePasswordUtil {
    public static final int DEFAULT_TIME_STEP_SECONDS = 30;
    private static int NUM_DIGITS_OUTPUT = 6;
    private static final String blockOfZeros;

    public static String generateBase32Secret() {
        return TimeBasedOneTimePasswordUtil.generateBase32Secret(16);
    }

    public static String generateBase32Secret(int length) {
        StringBuilder sb = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; ++i) {
            int val = random.nextInt(32);
            if (val < 26) {
                sb.append((char)(65 + val));
                continue;
            }
            sb.append((char)(50 + (val - 26)));
        }
        return sb.toString();
    }

    public static boolean validateCurrentNumber(String base32Secret, int authNumber, int windowMillis) throws GeneralSecurityException {
        return TimeBasedOneTimePasswordUtil.validateCurrentNumber(base32Secret, authNumber, windowMillis, System.currentTimeMillis(), 30);
    }

    public static boolean validateCurrentNumber(String base32Secret, int authNumber, int windowMillis, long timeMillis, int timeStepSeconds) throws GeneralSecurityException {
        long from = timeMillis;
        long to = timeMillis;
        if (windowMillis > 0) {
            from -= (long)windowMillis;
            to += (long)windowMillis;
        }
        long timeStepMillis = timeStepSeconds * 1000;
        for (long millis = from; millis <= to; millis += timeStepMillis) {
            long compare = TimeBasedOneTimePasswordUtil.generateNumber(base32Secret, millis, timeStepSeconds);
            if (compare != (long)authNumber) continue;
            return true;
        }
        return false;
    }

    public static String generateCurrentNumberString(String base32Secret) throws GeneralSecurityException {
        return TimeBasedOneTimePasswordUtil.generateNumberString(base32Secret, System.currentTimeMillis(), 30);
    }

    public static String generateNumberString(String base32Secret, long timeMillis, int timeStepSeconds) throws GeneralSecurityException {
        long number = TimeBasedOneTimePasswordUtil.generateNumber(base32Secret, timeMillis, timeStepSeconds);
        return TimeBasedOneTimePasswordUtil.zeroPrepend(number, NUM_DIGITS_OUTPUT);
    }

    public static long generateCurrentNumber(String base32Secret) throws GeneralSecurityException {
        return TimeBasedOneTimePasswordUtil.generateNumber(base32Secret, System.currentTimeMillis(), 30);
    }

    public static long generateNumber(String base32Secret, long timeMillis, int timeStepSeconds) throws GeneralSecurityException {
        byte[] key = TimeBasedOneTimePasswordUtil.decodeBase32(base32Secret);
        byte[] data = new byte[8];
        long value = timeMillis / 1000L / (long)timeStepSeconds;
        int i = 7;
        while (value > 0L) {
            data[i] = (byte)(value & 255L);
            value >>= 8;
            --i;
        }
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 15;
        long truncatedHash = 0L;
        for (int i2 = offset; i2 < offset + 4; ++i2) {
            truncatedHash <<= 8;
            truncatedHash |= (long)(hash[i2] & 255);
        }
        truncatedHash &= Integer.MAX_VALUE;
        return truncatedHash %= 1000000L;
    }

    public static String qrImageUrl(String keyId, String secret) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl=200x200&chld=M|0&cht=qr&chl=");
        TimeBasedOneTimePasswordUtil.addOtpAuthPart(keyId, secret, sb);
        return sb.toString();
    }

    public static String generateOtpAuthUrl(String keyId, String secret) {
        StringBuilder sb = new StringBuilder(64);
        TimeBasedOneTimePasswordUtil.addOtpAuthPart(keyId, secret, sb);
        return sb.toString();
    }

    private static void addOtpAuthPart(String keyId, String secret, StringBuilder sb) {
        sb.append("otpauth://totp/").append(keyId).append("%3Fsecret%3D").append(secret);
    }

    static String zeroPrepend(long num, int digits) {
        String numStr = Long.toString(num);
        if (numStr.length() >= digits) {
            return numStr;
        }
        StringBuilder sb = new StringBuilder(digits);
        int zeroCount = digits - numStr.length();
        sb.append(blockOfZeros, 0, zeroCount);
        sb.append(numStr);
        return sb.toString();
    }

    static byte[] decodeBase32(String str) {
        int numBytes = (str.length() * 5 + 7) / 8;
        byte[] result = new byte[numBytes];
        int resultIndex = 0;
        int which = 0;
        int working = 0;
        block10 : for (int i = 0; i < str.length(); ++i) {
            int val;
            char ch = str.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                val = ch - 97;
            } else if (ch >= 'A' && ch <= 'Z') {
                val = ch - 65;
            } else if (ch >= '2' && ch <= '7') {
                val = 26 + (ch - 50);
            } else {
                if (ch == '=') {
                    which = 0;
                    break;
                }
                throw new IllegalArgumentException("Invalid base-32 character: " + ch);
            }
            switch (which) {
                case 0: {
                    working = (val & 31) << 3;
                    which = 1;
                    continue block10;
                }
                case 1: {
                    result[resultIndex++] = (byte)(working |= (val & 28) >> 2);
                    working = (val & 3) << 6;
                    which = 2;
                    continue block10;
                }
                case 2: {
                    working |= (val & 31) << 1;
                    which = 3;
                    continue block10;
                }
                case 3: {
                    result[resultIndex++] = (byte)(working |= (val & 16) >> 4);
                    working = (val & 15) << 4;
                    which = 4;
                    continue block10;
                }
                case 4: {
                    result[resultIndex++] = (byte)(working |= (val & 30) >> 1);
                    working = (val & 1) << 7;
                    which = 5;
                    continue block10;
                }
                case 5: {
                    working |= (val & 31) << 2;
                    which = 6;
                    continue block10;
                }
                case 6: {
                    result[resultIndex++] = (byte)(working |= (val & 24) >> 3);
                    working = (val & 7) << 5;
                    which = 7;
                    continue block10;
                }
                case 7: {
                    result[resultIndex++] = (byte)(working |= val & 31);
                    which = 0;
                }
            }
        }
        if (which != 0) {
            result[resultIndex++] = (byte)working;
        }
        if (resultIndex != result.length) {
            result = Arrays.copyOf(result, resultIndex);
        }
        return result;
    }

    static {
        char[] chars = new char[NUM_DIGITS_OUTPUT];
        for (int i = 0; i < chars.length; ++i) {
            chars[i] = 48;
        }
        blockOfZeros = new String(chars);
    }
}

