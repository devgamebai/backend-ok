/*
 * Decompiled with CFR 0_116.
 */
package game.utils;

import java.util.Random;

public class NumberUtils {
    private static Random rd = new Random();

    public static boolean isDoWithRatio(double ratio) {
        double i = rd.nextDouble() * 100.0;
        if (i < ratio) {
            return true;
        }
        return false;
    }

    public static int randomIntLimit(int min, int max) {
        if (min > 0 && max >= min) {
            return rd.nextInt(max + 1 - min) + min;
        }
        return 0;
    }

    public static int randomInt(int i) {
        if (i > 0) {
            return rd.nextInt(i);
        }
        return 0;
    }
}

