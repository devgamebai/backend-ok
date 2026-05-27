/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import java.util.Random;

public class NumberUtils {
    public static String formatNumber(String s) {
        if (s.length() > 3) {
            int x = s.length() / 3;
            int y = s.length() % 3;
            StringBuilder res = new StringBuilder(s.substring(0, y));
            for (int i = 1; i <= x; ++i) {
                if (i == 1 && y == 0) {
                    res.append(s.substring(y, y + 3));
                } else {
                    res.append(",").append(s.substring(y, y + 3));
                }
                y += 3;
            }
            return res.toString();
        }
        return s;
    }

    public static int ranDomIntMinMax(int min, int max) {
        if (min > 0 && max >= min) {
            Random random = new Random();
            return random.nextInt(max + 1 - min) + min;
        }
        return 0;
    }
}

