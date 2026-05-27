/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.utils;

import java.util.Random;

public class RandomUtil {
    public static int randInt(int min, int max) {
        Random rand = new Random();
        int i = rand.nextInt(++max - min) + min;
        return i;
    }

    public static int randInt(int max) {
        Random random = new Random();
        return random.nextInt(max);
    }

    public static double randDouble(double min, double max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }
}

