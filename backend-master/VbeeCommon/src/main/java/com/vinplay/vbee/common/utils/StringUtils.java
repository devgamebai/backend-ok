/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import java.util.Random;

public class StringUtils {
    private static final char[] symbols;
    private static final char[] symbolsNumber;

    public static String randomString(int length) {
        Random random = new Random();
        char[] buf = new char[length];
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buf);
    }

    public static String randomStringNumber(int length) {
        Random random = new Random();
        char[] buf = new char[length];
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbolsNumber[random.nextInt(symbolsNumber.length)];
        }
        return new String(buf);
    }

    static {
        char ch;
        StringBuilder tmp = new StringBuilder();
        StringBuilder tmpNumber = new StringBuilder();
        for (ch = '0'; ch <= '9'; ch = (char)(ch + '\u0001')) {
            tmp.append(ch);
            tmpNumber.append(ch);
        }
        for (ch = 'a'; ch <= 'z'; ch = (char)(ch + '\u0001')) {
            tmp.append(ch);
        }
        symbols = tmp.toString().toCharArray();
        symbolsNumber = tmpNumber.toString().toCharArray();
    }
}

